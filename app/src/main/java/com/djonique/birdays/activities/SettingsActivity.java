/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.ad.RewardedVideo;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.backup.BackupMetadata;
import com.djonique.birdays.backup.ExportHelper;
import com.djonique.birdays.backup.GoogleDriveBackupManager;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.DonateHelper;
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.ProgressDialogHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String STATE_PENDING_ACTION = "STATE_PENDING_ACTION";
    private Action pendingAction = Action.NONE;
    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private SharedPreferences preferences;
    private GoogleDriveBackupManager backupManager;
    /**
     * Set while the app settings screen is open to grant the contacts permission there.
     */
    private boolean contactsPermissionSettingsOpened;
    /**
     * Kept as a field so the non-cancelable dialog can be dismissed from onDestroy().
     */
    private ProgressDialogHelper progressDialog;

    private GoogleDriveBackupManager getBackupManager() {
        if (backupManager == null) {
            backupManager = new GoogleDriveBackupManager(this);
        }
        return backupManager;
    }

    void launchRingtonePicker(Intent intent) {
        try {
            ringtonePickerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.web_search_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // The consent screen is a separate activity, so this one can be recreated while the
        // result is pending. Restoring the action keeps the resumed flow (backup or restore).
        if (savedInstanceState != null) {
            int ordinal = savedInstanceState.getInt(STATE_PENDING_ACTION, Action.NONE.ordinal());
            Action[] actions = Action.values();
            if (ordinal >= 0 && ordinal < actions.length) {
                pendingAction = actions[ordinal];
            }
        }

        authorizationLauncher =
                registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (pendingAction == Action.BACKUP) {
                            performCloudBackup();
                        } else if (pendingAction == Action.RESTORE) {
                            performCloudRestore();
                        }
                    } else {
                        Toast.makeText(this, R.string.cancel_button, Toast.LENGTH_SHORT).show();
                    }
                    pendingAction = Action.NONE;
                });

        ringtonePickerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = IntentCompat.getParcelableExtra(result.getData(), RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
                        String ringtoneString = uri != null ? uri.toString() : "";
                        BirdaysPreferencesManager.getDefaultSharedPreferences(this).edit().putString(Constants.RINGTONE_KEY, ringtoneString).apply();

                        BirdaysPreferenceFragment fragment = (BirdaysPreferenceFragment)
                                getSupportFragmentManager().findFragmentById(R.id.content_settings);
                        if (fragment != null) {
                            fragment.updateRingtoneSummary(ringtoneString);
                        }
                    }
                });

        Utils.setupSystemBarAppearance(this);
        View container = findViewById(R.id.container_settings);
        AppBarLayout appBarLayout = findViewById(R.id.appbar_settings);
        Utils.applySystemBarsInsets(container, appBarLayout, null, container);

        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setTitle(R.string.action_settings);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            BirdaysPreferenceFragment fragment = new BirdaysPreferenceFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.content_settings, fragment).commit();
        }

        preferences = BirdaysPreferencesManager.getDefaultSharedPreferences(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                Utils.overrideCloseTransition(SettingsActivity.this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PENDING_ACTION, pendingAction.ordinal());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != Constants.READ_CONTACTS_PERMISSION_CODE) return;

        if (PermissionManager.readingContactsPermissionGranted(this)) {
            if (!preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                importContacts();
            }
            return;
        }

        // Denied. After the second denial Android stops showing the system dialog, so a repeated
        // tap on the preference would otherwise silently do nothing; the user is offered the app
        // settings instead, which are the only remaining way to grant the permission.
        showContactsPermissionDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The permission can only be granted manually in the app settings, so the import is
        // resumed by itself once the user comes back.
        if (contactsPermissionSettingsOpened) {
            contactsPermissionSettingsOpened = false;
            if (PermissionManager.readingContactsPermissionGranted(this)
                    && !preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                importContacts();
            }
        }
    }

    /**
     * Explains why the import cannot run and opens the app settings, where the permission can
     * still be granted after the system dialog has stopped appearing.
     */
    private void showContactsPermissionDialog() {
        if (isFinishing() || isDestroyed()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.contacts_import_title)
                .setMessage(R.string.permission_required)
                .setPositiveButton(R.string.help_settings, (dialog, which) -> openApplicationSettings())
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void openApplicationSettings() {
        contactsPermissionSettingsOpened = true;
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            contactsPermissionSettingsOpened = false;
            Toast.makeText(this, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Imports contacts on the repository pool, showing progress until either outcome arrives.
     */
    void startContactImport() {
        if (PermissionManager.readingContactsPermissionGranted(this)) {
            importContacts();
        } else {
            PermissionManager.requestReadingContactsPermission(this);
        }
    }

    private void importContacts() {
        showProgressDialog(getString(R.string.loading_contacts));

        BirdaysRepository.getInstance(this).importContacts(
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Integer inserted) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        preferences.edit().putBoolean(Constants.CONTACTS_UPLOADED, true).apply();
                        Utils.notifyWidget(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this, R.string.contacts_uploaded,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        preferences.edit().putBoolean(Constants.WRONG_CONTACTS_FORMAT, true).apply();
                        Toast.makeText(SettingsActivity.this, R.string.loading_contacts_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void performCloudBackup() {
        if (progressDialog != null) return;
        pendingAction = Action.BACKUP;
        showProgressDialog(getString(R.string.google_drive_backup_progress));

        GoogleDriveBackupManager manager = getBackupManager();
        manager.requestAuthorization(this, new GoogleDriveBackupManager.AuthorizationCallback() {
            @Override
            public void onSuccess(String accessToken) {
                pendingAction = Action.NONE;
                // Export reads the whole table and writes XML; it used to run on the main thread.
                BirdaysRepository.getInstance(SettingsActivity.this).createBackupFile(
                        new BirdaysRepository.Callback<>() {
                            @Override
                            public void onSuccess(ExportHelper.Backup backup) {
                                if (isFinishing() || isDestroyed()) return;
                                manager.uploadBackup(accessToken, backup.getFile(),
                                        backup.getAppProperties(),
                                        new GoogleDriveBackupManager.UploadCallback() {
                                            @Override
                                            public void onSuccess() {
                                                if (isFinishing() || isDestroyed()) return;
                                                dismissProgressDialog();
                                                Toast.makeText(SettingsActivity.this,
                                                        R.string.backup_complete, Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                if (isFinishing() || isDestroyed()) return;
                                                dismissProgressDialog();
                                                Toast.makeText(SettingsActivity.this,
                                                        R.string.upload_error + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }

                            @Override
                            public void onError(Throwable t) {
                                if (isFinishing() || isDestroyed()) return;
                                dismissProgressDialog();
                                Toast.makeText(SettingsActivity.this,
                                        R.string.backup_error + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onRequiresResolution(android.app.PendingIntent pendingIntent) {
                dismissProgressDialog();
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent).build();
                    authorizationLauncher.launch(request);
                } catch (Exception e) {
                    pendingAction = Action.NONE;
                    Toast.makeText(SettingsActivity.this, R.string.auth_error + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                dismissProgressDialog();
                pendingAction = Action.NONE;
                Toast.makeText(SettingsActivity.this, R.string.authorization_failed_error + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void performCloudRestore() {
        if (progressDialog != null) return;
        pendingAction = Action.RESTORE;
        showProgressDialog(getString(R.string.google_drive_restore_progress));

        GoogleDriveBackupManager manager = getBackupManager();
        manager.requestAuthorization(this, new GoogleDriveBackupManager.AuthorizationCallback() {
            @Override
            public void onSuccess(String accessToken) {
                pendingAction = Action.NONE;
                manager.findLatestBackup(accessToken, new GoogleDriveBackupManager.FindBackupCallback() {
                    @Override
                    public void onFound(BackupMetadata metadata) {
                        manager.downloadBackup(accessToken, metadata.getFileId(),
                                new GoogleDriveBackupManager.DownloadCallback() {
                                    @Override
                                    public void onSuccess(File downloadedFile) {
                                        if (isFinishing() || isDestroyed()) {
                                            GoogleDriveBackupManager.deleteRestoreTempFile(SettingsActivity.this);
                                            return;
                                        }
                                        prepareRestore(metadata, downloadedFile);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (isFinishing() || isDestroyed()) return;
                                        dismissProgressDialog();
                                        Toast.makeText(SettingsActivity.this, R.string.download_error + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onNotFound() {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        Toast.makeText(SettingsActivity.this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        Toast.makeText(SettingsActivity.this, R.string.search_error + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onRequiresResolution(android.app.PendingIntent pendingIntent) {
                // The consent screen replaces this activity's window; leaving the progress
                // dialog up would strand it behind the Google UI on return.
                dismissProgressDialog();
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent).build();
                    authorizationLauncher.launch(request);
                } catch (Exception e) {
                    pendingAction = Action.NONE;
                    Toast.makeText(SettingsActivity.this, R.string.auth_error + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                dismissProgressDialog();
                pendingAction = Action.NONE;
                Toast.makeText(SettingsActivity.this, R.string.authorization_failed_error + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Parses the downloaded backup on the repository pool. Nothing is written to the database: the
     * records are only held in memory until the user confirms the restore.
     */
    private void prepareRestore(BackupMetadata metadata, File downloadedFile) {
        BirdaysRepository.getInstance(this).prepareBackup(Uri.fromFile(downloadedFile),
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(BirdaysRepository.PreparedBackup prepared) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        showRestoreConfirmation(metadata, prepared);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        Toast.makeText(SettingsActivity.this,
                                R.string.recover_records_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * The backup is applied only after this dialog is confirmed. The count comes from the file's
     * {@code appProperties}; when Drive did not store it, the number of records parsed from the
     * downloaded file is shown instead.
     */
    private void showRestoreConfirmation(BackupMetadata metadata,
                                         BirdaysRepository.PreparedBackup prepared) {
        Integer recordCount = metadata.resolveRecordCount(prepared.getRecordCount());
        boolean fallbackUsed = !metadata.hasRecordCount();

        long modifiedTime = metadata.getModifiedTime();
        String date = modifiedTime > BackupMetadata.UNKNOWN_MODIFIED_TIME
                ? Utils.getDateTime(modifiedTime)
                : getString(R.string.restore_unknown);

        Log.d(TAG, "Restore confirmation: modifiedTime=" + modifiedTime
                + " (" + date + "), recordCount=" + (recordCount != null ? recordCount : "unknown")
                + ", appProperties fallback " + (fallbackUsed ? "used" : "not needed")
                + (fallbackUsed ? ", records parsed from the file: " + prepared.getRecordCount() : ""));

        String countText = recordCount != null
                ? String.valueOf(recordCount)
                : getString(R.string.restore_unknown);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_confirm_title)
                .setMessage(getString(R.string.restore_confirm_message, date, countText))
                .setPositiveButton(R.string.restore_button,
                        (dialog, which) -> applyRestore(prepared))
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void applyRestore(BirdaysRepository.PreparedBackup prepared) {
        showProgressDialog(getString(R.string.google_drive_restore_progress));

        BirdaysRepository.getInstance(this).restoreBackup(prepared,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Integer inserted) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        Utils.notifyWidget(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this,
                                R.string.records_recovered, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        Toast.makeText(SettingsActivity.this,
                                R.string.recover_records_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Replaces any dialog that is already on screen. Without this a second tap on an import or a
     * backup row during a slow request attached a second, non-cancelable dialog to the activity.
     */
    private void showProgressDialog(String message) {
        if (isFinishing() || isDestroyed()) return;
        dismissProgressDialog();
        progressDialog = new ProgressDialogHelper(this);
        progressDialog.startProgressDialog(message);
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismissProgressDialog();
            progressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        // The progress dialog is not cancelable, so it must be dismissed before the activity
        // window is torn down; otherwise it is left attached to a destroyed activity.
        dismissProgressDialog();
        if (backupManager != null) {
            backupManager.close();
        }
        super.onDestroy();
    }

    private enum Action {NONE, BACKUP, RESTORE}

    public static class BirdaysPreferenceFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String TIME_PICKER_TAG = "TIME_PICKER_TAG";
        private static final String SCROLL_POSITION_KEY = "SCROLL_POSITION";

        private SharedPreferences preferences;
        private RewardedVideo rewardedVideo;

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (getListView() != null) {
                getListView().setClipToPadding(false);
            }
            if (savedInstanceState != null) {
                int position = savedInstanceState.getInt(SCROLL_POSITION_KEY, RecyclerView.NO_POSITION);
                if (position != RecyclerView.NO_POSITION && getListView() != null) {
                    getListView().scrollToPosition(position);
                }
            }
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            RecyclerView listView = getListView();
            if (listView != null && listView.getLayoutManager() instanceof LinearLayoutManager) {
                int position = ((LinearLayoutManager) listView.getLayoutManager())
                        .findFirstVisibleItemPosition();
                outState.putInt(SCROLL_POSITION_KEY, position);
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            if (getActivity() == null) return;
            preferences = BirdaysPreferencesManager.getDefaultSharedPreferences(getActivity());

            Preference addNotifPref = findPreference(Constants.ADDITIONAL_NOTIFICATION_KEY);
            if (addNotifPref != null) {
                addNotifPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    ((ListPreference) preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference) preference).getEntry());
                    return true;
                });
            }

            Preference timePref = findPreference(Constants.NOTIFICATION_TIME_KEY);
            if (timePref != null) {
                String savedTime = Utils.getNotificationTime(preferences);
                timePref.setSummary(Utils.getNotificationTimeSummary(getActivity(), savedTime));
                timePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() == null) return true;
                    int[] time =
                            Utils.parseNotificationTime(Utils.getNotificationTime(preferences));

                    MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                            .setTimeFormat(DateFormat.is24HourFormat(getActivity())
                                    ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                            .setHour(time[0])
                            .setMinute(time[1])
                            .setTitleText(R.string.notifications_time)
                            .build();
                    timePicker.addOnPositiveButtonClickListener(picker -> {
                        Utils.setNotificationTime(preferences,
                                timePicker.getHour(), timePicker.getMinute());
                        timePref.setSummary(Utils.getNotificationTimeSummary(getActivity(),
                                Utils.getNotificationTime(preferences)));
                        onSharedPreferenceChanged(preferences, Constants.NOTIFICATION_TIME_KEY);
                    });
                    timePicker.show(getParentFragmentManager(), TIME_PICKER_TAG);
                    return true;
                });
            }

            Preference ringtonePreference = findPreference(Constants.RINGTONE_KEY);
            if (ringtonePreference != null) {
                updateRingtoneSummary(preferences.getString(Constants.RINGTONE_KEY, Settings.System.DEFAULT_NOTIFICATION_URI.toString()));
                ringtonePreference.setOnPreferenceClickListener(preference -> {
                    if (getActivity() == null) return true;
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

                    String currentRingtone = preferences.getString(Constants.RINGTONE_KEY, Settings.System.DEFAULT_NOTIFICATION_URI.toString());
                    if (!currentRingtone.isEmpty()) {
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentRingtone));
                    }
                    ((SettingsActivity) getActivity()).launchRingtonePicker(intent);
                    return true;
                });
            }

            Preference helpPref = findPreference(getString(R.string.help_key));
            if (helpPref != null) {
                helpPref.setOnPreferenceClickListener(preference -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        startActivity(new Intent(activity, HelpActivity.class));
                        Utils.overrideOpenTransition(activity, R.anim.activity_secondary_in, R.anim.activity_primary_out);
                    }
                    return true;
                });
            }

            Preference importPref = findPreference(getString(R.string.import_contacts_key));
            if (importPref != null) {
                importPref.setOnPreferenceClickListener(preference -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity == null) return true;
                    // A tap always has to produce a result: either the import runs, the permission
                    // is requested, or the user is told why the import is blocked.
                    if (preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                        Toast.makeText(activity, R.string.loading_contacts_error, Toast.LENGTH_LONG).show();
                    } else {
                        activity.startContactImport();
                    }
                    return true;
                });
            }

            Preference googleDriveBackupPref = findPreference(getString(R.string.google_drive_backup_key));
            if (googleDriveBackupPref != null) {
                googleDriveBackupPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        ((SettingsActivity) getActivity()).performCloudBackup();
                    }
                    return true;
                });
            }

            Preference googleDriveRestorePref = findPreference(getString(R.string.google_drive_restore_key));
            if (googleDriveRestorePref != null) {
                googleDriveRestorePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        ((SettingsActivity) getActivity()).performCloudRestore();
                    }
                    return true;
                });
            }

            Preference startPagePref = findPreference(getString(R.string.start_page_key));
            if (startPagePref != null) {
                startPagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    ((ListPreference) preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference) preference).getEntry());
                    return true;
                });
            }

            Preference displayedAgePref = findPreference(getString(R.string.displayed_age_key));
            if (displayedAgePref != null) {
                displayedAgePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    ((ListPreference) preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference) preference).getEntry());
                    showRestartAppDialog();
                    return true;
                });
            }

            Preference ratePref = findPreference(getString(R.string.rate_app_key));
            if (ratePref != null) {
                ratePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null)
                        Utils.openBrowser(getActivity(), getString(R.string.play_market_app_link));
                    return true;
                });
            }

            Preference sharePref = findPreference(getString(R.string.share_app_key));
            if (sharePref != null) {
                sharePref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + getString(R.string.play_market_app_link));
                    startActivity(Intent.createChooser(intent, null));
                    return true;
                });
            }

            if (getActivity() != null) {
                rewardedVideo = new RewardedVideo(getActivity());
                rewardedVideo.preload();
            }

            SwitchPreferenceCompat adSwitchPref = findPreference(getString(R.string.show_ads_key));
            if (adSwitchPref != null) {
                adSwitchPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabling = (Boolean) newValue;
                    if (!isEnabling) {
                        if (getActivity() == null) return false;
                        Context appContext = getActivity().getApplicationContext();
                        if (rewardedVideo == null) {
                            rewardedVideo = new RewardedVideo(appContext);
                            rewardedVideo.preload();
                        }
                        rewardedVideo.show(getActivity(), new RewardedVideo.RewardCallback() {
                            @Override
                            public void onRewarded() {
                                BirdaysPreferencesManager.getDefaultSharedPreferences(appContext)
                                        .edit()
                                        .putBoolean(appContext.getString(R.string.show_ads_key), false)
                                        .apply();
                            }

                            @Override
                            public void onAdDismissed(boolean rewardEarned) {
                                if (rewardEarned) {
                                    BirdaysPreferencesManager.getDefaultSharedPreferences(appContext)
                                            .edit()
                                            .putBoolean(appContext.getString(R.string.show_ads_key), false)
                                            .apply();
                                }
                                if (!isAdded() || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
                                    return;
                                }
                                if (rewardEarned) {
                                    adSwitchPref.setChecked(false);
                                    Toast.makeText(getActivity(), R.string.ad_disabled_success, Toast.LENGTH_SHORT).show();
                                } else {
                                    adSwitchPref.setChecked(true);
                                    Toast.makeText(getActivity(), R.string.ad_watch_full_video_to_disable, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onAdFailed() {
                                if (!isAdded() || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
                                    return;
                                }
                                adSwitchPref.setChecked(true);
                                Toast.makeText(getActivity(), R.string.ad_rewarded_unavailable, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return false;
                    }
                    return true;
                });
            }

            Preference adRewardedPref = findPreference(getString(R.string.ad_rewarded_key));
            if (adRewardedPref != null) {
                adRewardedPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        if (rewardedVideo == null) {
                            rewardedVideo = new RewardedVideo(getActivity());
                            rewardedVideo.preload();
                        }
                        rewardedVideo.show(getActivity());
                    }
                    return true;
                });
            }

            Preference donatePref = findPreference(getString(R.string.donate_key));
            if (donatePref != null) {
                donatePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        DonateHelper.openDonatePage(getActivity());
                    }
                    return true;
                });
            }

            Preference sourcePref = findPreference(getString(R.string.source_code_key));
            if (sourcePref != null) {
                sourcePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null)
                        Utils.openBrowser(getActivity(), getString(R.string.github_url));
                    return true;
                });
            }

            Preference privacyPref = findPreference(getString(R.string.privacy_policy_key));
            if (privacyPref != null) {
                privacyPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null)
                        DonateHelper.openPrivacyPolicy(getActivity());
                    return true;
                });
            }

            Preference licensesPref = findPreference(getString(R.string.licenses_key));
            if (licensesPref != null) {
                licensesPref.setOnPreferenceClickListener(preference -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        startActivity(new Intent(activity, LicensesActivity.class));
                        Utils.overrideOpenTransition(activity, R.anim.activity_secondary_in, R.anim.activity_primary_out);
                    }
                    return true;
                });
            }
        }

        public void updateRingtoneSummary(String ringtoneString) {
            Preference ringtonePreference = findPreference(Constants.RINGTONE_KEY);
            if (ringtonePreference == null || getActivity() == null) return;
            try {
                if (ringtoneString == null || ringtoneString.isEmpty()) {
                    ringtonePreference.setSummary(getString(R.string.silent));
                } else {
                    String ringtoneName = RingtoneManager.getRingtone(getActivity(), Uri.parse(ringtoneString)).getTitle(getActivity());
                    ringtonePreference.setSummary(ringtoneName);
                }
            } catch (Exception e) {
                ringtonePreference.setSummary(getString(R.string.silent));
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (getActivity() == null) return;

            // AppCompatDelegate recreates the started activities itself, so this must run on the
            // main thread and nothing else is needed here.
            if (Constants.NIGHT_MODE_KEY.equals(key)) {
                Utils.setupDayNightTheme(sharedPreferences);
                return;
            }

            // Everything below touches the database and re-arms alarms; it runs on a worker
            // thread with an application context so a slow read cannot block the settings UI.
            final Context appContext = getActivity().getApplicationContext();

            switch (key) {
                case Constants.NOTIFICATIONS_KEY:
                    final boolean isChecked = sharedPreferences.getBoolean(Constants.NOTIFICATIONS_KEY, false);
                    if (isChecked && !PermissionManager.notificationsPermissionGranted(getActivity())) {
                        PermissionManager.requestNotificationsPermission(getActivity());
                    }
                    //noinspection resource
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    try {
                        executor.execute(() -> {
                            final AlarmHelper alarmHelper = new AlarmHelper(appContext);
                            try (DbHelper dbHelper = new DbHelper(appContext)) {
                                List<Person> persons = dbHelper.query().getPersons();
                                for (Person person : persons) {
                                    if (isChecked) {
                                        alarmHelper.setAlarms(person);
                                    } else {
                                        alarmHelper.removeAlarms(person.getTimeStamp());
                                    }
                                }
                            }
                        });
                    } finally {
                        executor.shutdown();
                    }
                    break;
                case Constants.NOTIFICATION_TIME_KEY:
                case Constants.ADDITIONAL_NOTIFICATION_KEY:
                    restartAlarms(appContext);
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceScreen screen = getPreferenceScreen();
            if (screen != null) {
                SharedPreferences prefs = screen.getSharedPreferences();
                if (prefs != null) {
                    prefs.registerOnSharedPreferenceChangeListener(this);
                }
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferenceScreen screen = getPreferenceScreen();
            if (screen != null) {
                SharedPreferences prefs = screen.getSharedPreferences();
                if (prefs != null) {
                    prefs.unregisterOnSharedPreferenceChangeListener(this);
                }
            }
        }

        @Override
        public void onDestroy() {
            if (rewardedVideo != null) {
                rewardedVideo.destroy();
                rewardedVideo = null;
            }
            super.onDestroy();
        }

        private void restartAlarms(final Context appContext) {
            //noinspection resource
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.execute(() -> {
                    final AlarmHelper alarmHelper = new AlarmHelper(appContext);
                    try (DbHelper dbHelper = new DbHelper(appContext)) {
                        for (Person person : dbHelper.query().getPersons()) {
                            alarmHelper.removeAlarms(person.getTimeStamp());
                            alarmHelper.setAlarms(person);
                        }
                    }
                });
            } finally {
                executor.shutdown();
            }
        }

        private void restartApp() {
            if (getActivity() == null) return;
            Intent intent = getActivity().getPackageManager()
                    .getLaunchIntentForPackage(getActivity().getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }

        private void showRestartAppDialog() {
            if (getActivity() == null) return;
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            builder.setTitle(getString(R.string.action_settings));
            builder.setMessage(R.string.displayed_age_changes);
            builder.setPositiveButton(R.string.restart_now, (dialog, which) -> restartApp());
            builder.setNegativeButton(R.string.later, (dialog, which) -> dialog.cancel());
            builder.show();
        }
    }
}
