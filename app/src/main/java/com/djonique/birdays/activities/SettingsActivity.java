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
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.djonique.birdays.R;
import com.djonique.birdays.ad.RewardedVideo;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.backup.ExportHelper;
import com.djonique.birdays.backup.RecoverHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.ContactsHelper;
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.Utils;

import java.util.List;

public class SettingsActivity extends AppCompatActivity implements ContactsHelper.LoadingContactsListener {

    private static final String DO_NOT_SHOW_OPTIMIZATION_ALERT = "DO_NOT_SHOW_OPTIMIZATION_ALERT";
    private static final int FILE_MANAGER = 5;

    private AppCompatCheckBox cbDoNotShowAgain;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BirdaysPreferenceFragment fragment = new BirdaysPreferenceFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!preferences.getBoolean(DO_NOT_SHOW_OPTIMIZATION_ALERT, false)) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                    showWarningMessage(this);
                }
            }
        }
    }

    private void showWarningMessage(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_warning_red_24dp);
        builder.setTitle(R.string.optimization_enabled);

        @SuppressLint("InflateParams") LinearLayout view = (LinearLayout)
                getLayoutInflater().inflate(R.layout.checkbox_do_not_show_again, null);

        builder.setView(view);
        cbDoNotShowAgain = view.findViewById(R.id.checkbox_alert);

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
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_primary_in, R.anim.activity_secondary_out);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.READ_CONTACTS_PERMISSION_CODE && PermissionManager.readingContactsPermissionGranted(this)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (!preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                new ContactsHelper(this, getContentResolver()).loadContacts(preferences);
            }
        } else if (PermissionManager.writingSdPermissionGranted(this)) {
            if (requestCode == Constants.WRITE_EXTERNAL_STORAGE_PERMISSION_CODE) {
                new ExportHelper(this).exportRecords();
            } else if (requestCode == Constants.READ_EXTERNAL_STORAGE_PERMISSION_CODE) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                            .setType("text/xml")
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    startActivityForResult(Intent.createChooser(intent, null), FILE_MANAGER);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.recover_records_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_MANAGER && resultCode == RESULT_OK) {
            new RecoverHelper(this).recoverRecords(this, data.getData());
        }
    }

    @Override
    public void onContactsUploaded() {
    }

    public static class BirdaysPreferenceFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences preferences;
        private RewardedVideo rewardedVideo;

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

            Preference ringtonePreference = findPreference(Constants.RINGTONE_KEY);
            if (ringtonePreference != null) {
                try {
                    String defaultRingtoneString = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                    String ringtoneString = preferences.getString(ringtonePreference.getKey(), defaultRingtoneString);
                    if (ringtoneString.equals(defaultRingtoneString)) {
                        preferences.edit().putString(Constants.RINGTONE_KEY, defaultRingtoneString).apply();
                    }
                    if (ringtoneString.equals("")) {
                        ringtonePreference.setSummary(getString(R.string.silent));
                    } else {
                        String ringtoneName = RingtoneManager.getRingtone(getActivity(),
                                Uri.parse(ringtoneString)).getTitle(getActivity());
                        ringtonePreference.setSummary(ringtoneName);
                    }
                } catch (Exception ignored) {
                }

                ringtonePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        if (newValue.toString().equals("")) {
                            preference.setSummary(getString(R.string.silent));
                        } else {
                            String newRingtoneName = RingtoneManager.getRingtone(getActivity(),
                                    Uri.parse(newValue.toString())).getTitle(getActivity());
                            preference.setSummary(newRingtoneName);
                        }
                    } catch (Exception ignored) {
                    }
                    return true;
                });
            }

            Preference helpPref = findPreference(getString(R.string.help_key));
            if (helpPref != null) {
                helpPref.setOnPreferenceClickListener(preference -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        startActivity(new Intent(activity, HelpActivity.class));
                        activity.overridePendingTransition(R.anim.activity_secondary_in, R.anim.activity_primary_out);
                    }
                    return true;
                });
            }

            Preference importPref = findPreference(getString(R.string.import_contacts_key));
            if (importPref != null) {
                importPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null && !preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                        ContactsHelper contactsHelper = new ContactsHelper(getActivity(), getActivity().getContentResolver());
                        contactsHelper.loadContacts(preferences);
                    }
                    return true;
                });
            }

            Preference exportPref = findPreference(getString(R.string.export_key));
            if (exportPref != null) {
                exportPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        if (PermissionManager.writingSdPermissionGranted(getActivity())) {
                            new ExportHelper(getActivity()).exportRecords();
                        } else {
                            PermissionManager.requestWritingSdPermission(getActivity());
                        }
                    }
                    return true;
                });
            }

            Preference recoverPref = findPreference(getString(R.string.recover_key));
            if (recoverPref != null) {
                recoverPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        if (PermissionManager.writingSdPermissionGranted(getActivity())) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                                        .setType("text/xml")
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                                startActivityForResult(intent, FILE_MANAGER);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(getActivity(), R.string.recover_records_error,
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            PermissionManager.requestReadingSdPermission(getActivity());
                        }
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
                    if (getActivity() != null) Utils.openBrowser(getActivity(), getString(R.string.play_market_app_link));
                    return true;
                });
            }

            Preference sharePref = findPreference(getString(R.string.share_key));
            if (sharePref != null) {
                sharePref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + getString(R.string.play_market_app_link));
                    startActivity(Intent.createChooser(intent, null));
                    return true;
                });
            }

            Preference adBannerPref = findPreference(getString(R.string.ad_banner_key));
            if (adBannerPref != null) {
                adBannerPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    showRestartAppDialog();
                    return true;
                });
            }

            Preference adRewardedPref = findPreference(getString(R.string.ad_rewarded_key));
            if (adRewardedPref != null) {
                rewardedVideo = new RewardedVideo(getActivity());
                rewardedVideo.preload();
                adRewardedPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        rewardedVideo.show(getActivity());
                    }
                    return true;
                });
            }

            Preference sourcePref = findPreference(getString(R.string.source_code_key));
            if (sourcePref != null) {
                sourcePref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) Utils.openBrowser(getActivity(), getString(R.string.github_url));
                    return true;
                });
            }

            Preference privacyPref = findPreference(getString(R.string.privacy_policy_key));
            if (privacyPref != null) {
                privacyPref.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) Utils.openBrowser(getActivity(), getString(R.string.privacy_policy_link));
                    return true;
                });
            }

            Preference licensesPref = findPreference(getString(R.string.licenses_key));
            if (licensesPref != null) {
                licensesPref.setOnPreferenceClickListener(preference -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        startActivity(new Intent(activity, LicensesActivity.class));
                        activity.overridePendingTransition(R.anim.activity_secondary_in, R.anim.activity_primary_out);
                    }
                    return true;
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == FILE_MANAGER && resultCode == RESULT_OK && getActivity() != null && data != null) {
                new RecoverHelper(getActivity()).recoverRecords(getActivity(), data.getData());
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (getActivity() == null) return;
            final AlarmHelper alarmHelper = new AlarmHelper(getActivity());
            DbHelper dbHelper = new DbHelper(getActivity());
            final List<Person> persons = dbHelper.query().getPersons();
            switch (key) {
                case Constants.NOTIFICATIONS_KEY:
                    final boolean isChecked = sharedPreferences.getBoolean(Constants.NOTIFICATIONS_KEY, false);
                    if (isChecked && !PermissionManager.notificationsPermissionGranted(getActivity())) {
                        PermissionManager.requestNotificationsPermission(getActivity());
                    }
                    new Thread(() -> {
                        if (isChecked) {
                            for (Person person : persons) {
                                alarmHelper.setAlarms(person);
                            }
                        } else {
                            for (Person person : persons) {
                                alarmHelper.removeAlarms(person.getTimeStamp());
                            }
                        }
                    }).start();
                    break;
                case Constants.NOTIFICATION_TIME_KEY:
                case Constants.ADDITIONAL_NOTIFICATION_KEY:
                    restartAlarms(alarmHelper, persons);
                    break;
                case Constants.NIGHT_MODE_KEY:
                    Utils.setupDayNightTheme(sharedPreferences);
                    restartApp();
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            if (rewardedVideo != null) {
                rewardedVideo.destroy();
            }
            super.onDestroy();
        }

        private void restartAlarms(final AlarmHelper alarmHelper, final List<Person> persons) {
            new Thread(() -> {
                for (Person person : persons) {
                    alarmHelper.removeAlarms(person.getTimeStamp());
                    alarmHelper.setAlarms(person);
                }
            }).start();
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.action_settings));
            builder.setMessage(R.string.displayed_age_changes);
            builder.setPositiveButton(R.string.restart_now, (dialog, which) -> restartApp());
            builder.setNegativeButton(R.string.later, (dialog, which) -> dialog.cancel());
            builder.show();
        }
    }
}