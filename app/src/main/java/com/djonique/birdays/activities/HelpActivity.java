/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.djonique.birdays.R;
import com.djonique.birdays.utils.Constants;
import com.google.android.material.snackbar.Snackbar;

public class HelpActivity extends AppCompatActivity {

    private static final String HUAWEI = "huawei";
    private static final String SAMSUNG = "samsung";
    private static final String XIAOMI = "xiaomi";
    private static final String ALERT_SHOWED = "ALERT_SHOWED";
    private static final String ACTIVITY_NOT_FOUND_EXCEPTION = "ActivityNotFoundException";

    private AppCompatButton btnOpenWhitelist;
    private AppCompatButton btnOpenSettings;
    private AppCompatButton btnSendEmail;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        btnOpenWhitelist = findViewById(R.id.button_help_whitelist);
        btnOpenSettings = findViewById(R.id.button_help_settings);
        btnSendEmail = findViewById(R.id.button_help_email);

        if (btnOpenWhitelist != null) btnOpenWhitelist.setText(R.string.help_whitelist);
        if (btnOpenSettings != null) btnOpenSettings.setText(R.string.help_settings);
        if (btnSendEmail != null) btnSendEmail.setText(R.string.send_email);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!preferences.getBoolean(ALERT_SHOWED, false)) {
            checkManufacturer(Build.MANUFACTURER.toLowerCase());
        }

        if (btnOpenWhitelist != null) btnOpenWhitelist.setOnClickListener(v -> openBatteryOptimization());
        if (btnOpenSettings != null) btnOpenSettings.setOnClickListener(v -> openSettings());
        if (btnSendEmail != null) btnSendEmail.setOnClickListener(v -> sendEmail());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                overridePendingTransition(R.anim.activity_primary_in, R.anim.activity_secondary_out);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkManufacturer(String manufacturer) {
        switch (manufacturer) {
            case HUAWEI:
                showAlertSnackbar(getString(R.string.huawei_header), getString(R.string.huawei_description));
                break;
            case SAMSUNG:
                showAlertSnackbar(getString(R.string.samsung_header), getString(R.string.samsung_description));
                break;
            case XIAOMI:
                showAlertSnackbar(getString(R.string.xiaomi_header), getString(R.string.xiaomi_description));
                break;
        }
    }

    private void showAlertSnackbar(final String title, final String message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.ll_help_activity), title, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.show_button, view -> {
                    showAlertDialog(title, message);
                    preferences.edit().putBoolean(ALERT_SHOWED, true).apply();
                });
        snackbar.setActionTextColor(Color.WHITE);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.red_alert));
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) textView.setAllCaps(true);
        snackbar.show();
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok_button, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void openBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(Intent.createChooser(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), null));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, ACTIVITY_NOT_FOUND_EXCEPTION, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.help_whitelist_error, Toast.LENGTH_LONG).show();
        }
    }

    private void openSettings() {
        startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    private void sendEmail() {
        String email = getString(R.string.birdays_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType(Constants.TYPE_EMAIL);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, Build.MANUFACTURER + "/"
                + Build.MODEL + "/"
                + Build.VERSION.SDK_INT + "/"
                + getString(R.string.version_name));
        intent.setData(Uri.parse(Constants.MAILTO + email));
        startActivity(Intent.createChooser(intent, null));
    }
}