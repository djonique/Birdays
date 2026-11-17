/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.api.services.drive.model.File;

import java.util.Collections;
import java.util.Map;

/**
 * Metadata of the backup file in Google Drive: when it was written and how many records it holds.
 *
 * <p>{@code recordCount} is stored in the file's {@code appProperties}. It is only a hint for the
 * confirmation dialog and never a restore prerequisite: a missing, blank or unreadable value falls
 * back to the number of records parsed from the downloaded file.</p>
 */
public final class BackupMetadata {

    /**
     * {@code modifiedTime} is missing from the Drive response.
     */
    public static final long UNKNOWN_MODIFIED_TIME = 0L;
    /**
     * Key of the record count inside {@code appProperties}; part of the published backup format.
     */
    static final String KEY_RECORD_COUNT = "recordCount";
    private static final String TAG = "BackupMetadata";
    private final String fileId;
    private final long modifiedTime;
    @Nullable
    private final Integer recordCount;

    BackupMetadata(@NonNull String fileId, long modifiedTime, @Nullable Integer recordCount) {
        this.fileId = fileId;
        this.modifiedTime = modifiedTime;
        this.recordCount = recordCount;
    }

    /**
     * Reads the metadata of a Drive file. The {@code file} must be requested with the
     * {@code id, modifiedTime, appProperties} fields, otherwise the values are not populated.
     */
    @NonNull
    public static BackupMetadata fromDriveFile(@NonNull File file) {
        long modifiedTime = file.getModifiedTime() != null
                ? file.getModifiedTime().getValue()
                : UNKNOWN_MODIFIED_TIME;
        Integer recordCount = parseRecordCount(file.getAppProperties());
        Log.d(TAG, "Backup '" + file.getId() + "': modifiedTime=" + modifiedTime
                + ", appProperties[" + KEY_RECORD_COUNT + "]="
                + (recordCount != null ? recordCount : "missing or unreadable"));
        return new BackupMetadata(file.getId(), modifiedTime, recordCount);
    }

    /**
     * {@code appProperties} written next to the uploaded backup file.
     */
    @NonNull
    static Map<String, String> createAppProperties(int recordCount) {
        return Collections.singletonMap(KEY_RECORD_COUNT, String.valueOf(recordCount));
    }

    /**
     * Reads {@code appProperties["recordCount"]}. A blank, non-numeric or non-positive value is
     * reported as unknown instead of failing the restore.
     */
    @Nullable
    static Integer parseRecordCount(@Nullable Map<String, String> appProperties) {
        if (appProperties == null) {
            return null;
        }
        String raw = appProperties.get(KEY_RECORD_COUNT);
        if (raw == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            // Logged by fromDriveFile() as "missing or unreadable"; kept out of the parser so the
            // fallback logic stays testable without the Android framework.
            return null;
        }
    }

    @NonNull
    public String getFileId() {
        return fileId;
    }

    /**
     * Milliseconds since the epoch, or {@link #UNKNOWN_MODIFIED_TIME} when Drive omitted it.
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

    public boolean hasRecordCount() {
        return recordCount != null;
    }

    /**
     * Count shown to the user: the value from {@code appProperties} when it is readable, otherwise
     * the number of records parsed from the downloaded backup.
     */
    @Nullable
    public Integer resolveRecordCount(@Nullable Integer parsedRecordCount) {
        return recordCount != null ? recordCount : parsedRecordCount;
    }
}
