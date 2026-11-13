/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.ad.RewardedVideo;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.backup.GoogleDriveBackupManager;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.ProgressDialogHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private static final String DO_NOT_SHOW_OPTIMIZATION_ALERT = "DO_NOT_SHOW_OPTIMIZATION_ALERT";
    private Action pendingAction = Action.NONE;
    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private SharedPreferences preferences;
    private GoogleDriveBackupManager backupManager;

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
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.RINGTONE_KEY, ringtoneString).apply();

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

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!preferences.getBoolean(DO_NOT_SHOW_OPTIMIZATION_ALERT, false)) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                showWarningMessage(this);
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                Utils.overrideCloseTransition(SettingsActivity.this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
            }
        });
    }

    private void showWarningMessage(Context context) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setIcon(R.drawable.ic_warning_red_24dp);
        builder.setTitle(R.string.optimization_enabled);

        @SuppressLint("InflateParams") LinearLayout view = (LinearLayout)
                getLayoutInflater().inflate(R.layout.checkbox_do_not_show_again, null);

        builder.setView(view);
        AppCompatCheckBox cbDoNotShowAgain = view.findViewById(R.id.checkbox_alert);

        builder.setMessage(R.string.optimization_description);
        builder.setPositiveButton(R.string.disable_button, (dialog, which) -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                if (cbDoNotShowAgain.isChecked()) {
                    preferences.edit().putBoolean(DO_NOT_SHOW_OPTIMIZATION_ALERT, true).apply();
                }
            } catch (ActivityNotFoundException ignored) {
            }
        });
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> {
            dialog.cancel();
            if (cbDoNotShowAgain.isChecked()) {
                preferences.edit().putBoolean(DO_NOT_SHOW_OPTIMIZATION_ALERT, true).apply();
            }
        });
        builder.show();
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
        if (requestCode == Constants.READ_CONTACTS_PERMISSION_CODE && PermissionManager.readingContactsPermissionGranted(this)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (!preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                importContacts();
            }
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
        ProgressDialogHelper progressDialog = new ProgressDialogHelper(this);
        progressDialog.startProgressDialog(getString(R.string.loading_contacts));

        BirdaysRepository.getInstance(this).importContacts(
                new BirdaysRepository.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer inserted) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismissProgressDialog();
                        preferences.edit().putBoolean(Constants.CONTACTS_UPLOADED, true).apply();
                        Utils.notifyWidget(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this, R.string.contacts_uploaded,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismissProgressDialog();
                        preferences.edit().putBoolean(Constants.WRONG_CONTACTS_FORMAT, true).apply();
                        Toast.makeText(SettingsActivity.this, R.string.loading_contacts_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void performCloudBackup() {
        pendingAction = Action.BACKUP;
        ProgressDialogHelper progressDialog = new ProgressDialogHelper(this);
        progressDialog.startProgressDialog(getString(R.string.google_drive_backup_progress));

        GoogleDriveBackupManager manager = getBackupManager();
        manager.requestAuthorization(this, new GoogleDriveBackupManager.AuthorizationCallback() {
            @Override
            public void onSuccess(String accessToken) {
                pendingAction = Action.NONE;
                // Export reads the whole table and writes XML; it used to run on the main thread.
                BirdaysRepository.getInstance(SettingsActivity.this).createBackupFile(
                        new BirdaysRepository.Callback<File>() {
                            @Override
                            public void onSuccess(File backupFile) {
                                if (isFinishing() || isDestroyed()) return;
                                manager.uploadBackup(accessToken, backupFile,
                                        new GoogleDriveBackupManager.UploadCallback() {
                                            @Override
                                            public void onSuccess() {
                                                if (isFinishing() || isDestroyed()) return;
                                                progressDialog.dismissProgressDialog();
                                                Toast.makeText(SettingsActivity.this,
                                                        R.string.backup_complete, Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                if (isFinishing() || isDestroyed()) return;
                                                progressDialog.dismissProgressDialog();
                                                Toast.makeText(SettingsActivity.this,
                                                        "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }

                            @Override
                            public void onError(Throwable t) {
                                if (isFinishing() || isDestroyed()) return;
                                progressDialog.dismissProgressDialog();
                                Toast.makeText(SettingsActivity.this,
                                        "Backup error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onRequiresResolution(android.app.PendingIntent pendingIntent) {
                progressDialog.dismissProgressDialog();
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent).build();
                    authorizationLauncher.launch(request);
                } catch (Exception e) {
                    pendingAction = Action.NONE;
                    Toast.makeText(SettingsActivity.this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismissProgressDialog();
                pendingAction = Action.NONE;
                Toast.makeText(SettingsActivity.this, "Authorization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void performCloudRestore() {
        pendingAction = Action.RESTORE;
        ProgressDialogHelper progressDialog = new ProgressDialogHelper(this);
        progressDialog.startProgressDialog(getString(R.string.google_drive_restore_progress));

        GoogleDriveBackupManager manager = getBackupManager();
        manager.requestAuthorization(this, new GoogleDriveBackupManager.AuthorizationCallback() {
            @Override
            public void onSuccess(String accessToken) {
                pendingAction = Action.NONE;
                manager.findLatestBackup(accessToken, new GoogleDriveBackupManager.FindBackupCallback() {
                    @Override
                    public void onFound(String fileId, String modifiedTime) {
                        manager.downloadBackup(accessToken, fileId, new GoogleDriveBackupManager.DownloadCallback() {
                            @Override
                            public void onSuccess(File downloadedFile) {
                                if (isFinishing() || isDestroyed()) return;
                                // Parsing the XML and inserting every row used to run on the main
                                // thread through RecoverHelper; it now runs on the repository pool.
                                BirdaysRepository.getInstance(SettingsActivity.this).restoreBackup(
                                        Uri.fromFile(downloadedFile),
                                        new BirdaysRepository.Callback<Integer>() {
                                            @Override
                                            public void onSuccess(Integer inserted) {
                                                if (isFinishing() || isDestroyed()) return;
                                                progressDialog.dismissProgressDialog();
                                                Utils.notifyWidget(SettingsActivity.this);
                                                Toast.makeText(SettingsActivity.this,
                                                        R.string.records_recovered, Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onError(Throwable t) {
                                                if (isFinishing() || isDestroyed()) return;
                                                progressDialog.dismissProgressDialog();
                                                Toast.makeText(SettingsActivity.this,
                                                        R.string.recover_records_error, Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }

                            @Override
                            public void onError(Exception e) {
                                if (isFinishing() || isDestroyed()) return;
                                progressDialog.dismissProgressDialog();
                                Toast.makeText(SettingsActivity.this, "Download error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onNotFound() {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismissProgressDialog();
                        Toast.makeText(SettingsActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismissProgressDialog();
                        Toast.makeText(SettingsActivity.this, "Search error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onRequiresResolution(android.app.PendingIntent pendingIntent) {
                // The consent screen replaces this activity's window; leaving the progress
                // dialog up would strand it behind the Google UI on return.
                progressDialog.dismissProgressDialog();
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent).build();
                    authorizationLauncher.launch(request);
                } catch (Exception e) {
                    pendingAction = Action.NONE;
                    Toast.makeText(SettingsActivity.this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismissProgressDialog();
                pendingAction = Action.NONE;
                Toast.makeText(SettingsActivity.this, "Authorization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private enum Action {NONE, BACKUP, RESTORE}

    @Override
    protected void onDestroy() {
        if (backupManager != null) {
            backupManager.close();
        }
        super.onDestroy();
    }

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
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

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
                long savedTime = preferences.getLong(Constants.NOTIFICATION_TIME_KEY, 645703200000L - Utils.getTimeOffset());
                timePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(new Date(savedTime)));
                timePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() == null) return true;
                    long time = preferences.getLong(Constants.NOTIFICATION_TIME_KEY, 645703200000L - Utils.getTimeOffset());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(time);
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);

                    MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                            .setTimeFormat(DateFormat.is24HourFormat(getActivity())
                                    ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                            .setHour(hour)
                            .setMinute(minute)
                            .setTitleText(R.string.notifications_time)
                            .build();
                    timePicker.addOnPositiveButtonClickListener(picker -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                        cal.set(Calendar.MINUTE, timePicker.getMinute());
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        long newTime = cal.getTimeInMillis();
                        preferences.edit().putLong(Constants.NOTIFICATION_TIME_KEY, newTime).apply();
                        timePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(new Date(newTime)));
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
                    if (activity != null && !preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
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
                        if (rewardedVideo == null) {
                            rewardedVideo = new RewardedVideo(getActivity());
                            rewardedVideo.preload();
                        }
                        rewardedVideo.show(getActivity(), new RewardedVideo.RewardCallback() {
                            @Override
                            public void onRewarded() {
                            }

                            @Override
                            public void onAdDismissed(boolean rewardEarned) {
                                if (getActivity() == null) return;
                                if (rewardEarned) {
                                    preferences.edit().putBoolean(getString(R.string.show_ads_key), false).apply();
                                    adSwitchPref.setChecked(false);
                                    Toast.makeText(getActivity(), R.string.ad_disabled_success, Toast.LENGTH_SHORT).show();
                                } else {
                                    adSwitchPref.setChecked(true);
                                    Toast.makeText(getActivity(), R.string.ad_watch_full_video_to_disable, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onAdFailed() {
                                if (getActivity() == null) return;
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
                        Utils.openBrowser(getActivity(), getString(R.string.donate_url));
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
                        Utils.openBrowser(getActivity(), getString(R.string.privacy_policy_link));
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
            final AlarmHelper alarmHelper = new AlarmHelper(getActivity());
            List<Person> persons;
            try (DbHelper dbHelper = new DbHelper(getActivity())) {
                persons = dbHelper.query().getPersons();
            }
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
                            if (!isAdded() || getActivity() == null) return;
                            if (isChecked) {
                                for (Person person : persons) {
                                    alarmHelper.setAlarms(person);
                                }
                            } else {
                                for (Person person : persons) {
                                    alarmHelper.removeAlarms(person.getTimeStamp());
                                }
                            }
                        });
                    } finally {
                        executor.shutdown();
                    }
                    break;
                case Constants.NOTIFICATION_TIME_KEY:
                case Constants.ADDITIONAL_NOTIFICATION_KEY:
                    restartAlarms(alarmHelper, persons);
                    break;
                case Constants.NIGHT_MODE_KEY:
                    // AppCompatDelegate recreates the started activities itself, so the settings
                    // screen and its scroll position are restored by the framework
                    Utils.setupDayNightTheme(sharedPreferences);
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
            }
            super.onDestroy();
        }

        private void restartAlarms(final AlarmHelper alarmHelper, final List<Person> persons) {
            //noinspection resource
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.execute(() -> {
                    if (!isAdded() || getActivity() == null) return;
                    for (Person person : persons) {
                        alarmHelper.removeAlarms(person.getTimeStamp());
                        alarmHelper.setAlarms(person);
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