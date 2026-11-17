/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import com.djonique.birdays.utils.NamedDaemonThreadFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveBackupManager {

    private static final String TAG = "GoogleDriveBackup";
    /** Name of the file inside the Drive app-data folder; part of the published backup format. */
    private static final String BACKUP_FILE_NAME = "database_backup.xml";
    /** Local cache file for a downloaded backup; never reused for an upload. */
    private static final String RESTORE_FILE_NAME = "backup_restore.xml";
    private static final String MIME_TYPE_XML = "application/xml";

    private final Context context;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(new NamedDaemonThreadFactory("drive-backup"));
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GoogleDriveBackupManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Releases the worker thread. Call from the owning Activity's {@code onDestroy()}; pending
     * work is allowed to finish, but no new work is accepted afterward.
     */
    public void close() {
        executor.shutdown();
    }

    public void requestAuthorization(Activity activity, AuthorizationCallback callback) {
        AuthorizationRequest request = new AuthorizationRequest.Builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_APPDATA)))
                .build();

        AuthorizationClient client = Identity.getAuthorizationClient(activity);
        client.authorize(request)
                .addOnSuccessListener(result -> {
                    if (result.hasResolution() && result.getPendingIntent() != null) {
                        Log.d(TAG, "Authorization requires resolution (user consent / account picker).");
                        mainHandler.post(() -> callback.onRequiresResolution(result.getPendingIntent()));
                    } else if (result.getAccessToken() != null) {
                        Log.d(TAG, "Authorization successful, access token acquired.");
                        mainHandler.post(() -> callback.onSuccess(result.getAccessToken()));
                    } else {
                        Log.e(TAG, "Authorization result error: Access token is null.");
                        mainHandler.post(() -> callback.onError(new IllegalStateException("Access token is null")));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Authorization failed with exception", e);
                    mainHandler.post(() -> callback.onError(e));
                });
    }

    private Drive getDriveService(String accessToken) {
        AccessToken token = new AccessToken(accessToken, null);
        GoogleCredentials credentials = GoogleCredentials.create(token);
        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Birdays")
                .build();
    }

    public void uploadBackup(String accessToken, java.io.File xmlFile,
                             Map<String, String> appProperties, UploadCallback callback) {
        executor.execute(() -> {
            try {
                Drive driveService = getDriveService(accessToken);

                FileList result = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .setFields("files(id, name)")
                        .execute();

                FileContent mediaContent = new FileContent(MIME_TYPE_XML, xmlFile);
                String fileId;

                if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                    fileId = result.getFiles().get(0).getId();
                    driveService.files().update(fileId, new File(), mediaContent).execute();
                } else {
                    File fileMetadata = new File();
                    fileMetadata.setName(BACKUP_FILE_NAME);
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    fileId = driveService.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute()
                            .getId();
                }

                storeAppProperties(driveService, fileId, appProperties);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                Log.e(TAG, "Upload backup failed", e);
                mainHandler.post(() -> callback.onError(e));
            } finally {
                // The XML is a temporary local file; it is deleted even when the upload failed so
                // a copy of the user's data is not left in the cache directory.
                //noinspection ResultOfMethodCallIgnored
                xmlFile.delete();
            }
        });
    }

    /**
     * Writes the caller's {@code appProperties} (the record count) onto the uploaded file. The XML
     * is already in Drive when this runs, so a failure is only logged: the backup counts as
     * successful and the restore falls back to counting the records in the downloaded file.
     */
    private static void storeAppProperties(Drive driveService, String fileId,
                                           Map<String, String> appProperties) {
        if (appProperties == null || appProperties.isEmpty()) {
            return;
        }
        try {
            File metadata = new File();
            metadata.setAppProperties(appProperties);
            driveService.files().update(fileId, metadata).setFields("id").execute();
        } catch (Exception e) {
            Log.w(TAG, "Backup uploaded, but appProperties could not be stored", e);
        }
    }

    public void findLatestBackup(String accessToken, FindBackupCallback callback) {
        executor.execute(() -> {
            try {
                Drive driveService = getDriveService(accessToken);
                FileList result = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .setOrderBy("modifiedTime desc")
                        .setFields("files(id, name)")
                        .execute();

                if (result.getFiles() == null || result.getFiles().isEmpty()) {
                    mainHandler.post(callback::onNotFound);
                    return;
                }

                String fileId = result.getFiles().get(0).getId();
                // A direct GET asks Drive for the metadata again instead of reusing the list
                // response, so the confirmation dialog shows the current modifiedTime and
                // appProperties. The client keeps no metadata cache of its own.
                File metadataFile = driveService.files().get(fileId)
                        .setFields("id, modifiedTime, appProperties")
                        .execute();
                BackupMetadata metadata = BackupMetadata.fromDriveFile(metadataFile);
                mainHandler.post(() -> callback.onFound(metadata));
            } catch (Exception e) {
                Log.e(TAG, "Find latest backup failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void downloadBackup(String accessToken, String fileId, DownloadCallback callback) {
        executor.execute(() -> {
            try {
                Drive driveService = getDriveService(accessToken);
                java.io.File tempFile = new java.io.File(context.getCacheDir(), RESTORE_FILE_NAME);
                try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                    driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                }
                mainHandler.post(() -> callback.onSuccess(tempFile));
            } catch (Exception e) {
                Log.e(TAG, "Download backup failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public interface AuthorizationCallback {
        void onSuccess(String accessToken);

        void onRequiresResolution(PendingIntent pendingIntent);

        void onError(Exception e);
    }

    public interface UploadCallback {
        void onSuccess();

        void onError(Exception e);
    }

    public interface FindBackupCallback {
        void onFound(BackupMetadata metadata);

        void onNotFound();

        void onError(Exception e);
    }

    public interface DownloadCallback {
        void onSuccess(java.io.File downloadedFile);

        void onError(Exception e);
    }
}
