/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.djonique.birdays.R;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;

public class HelpActivity extends AppCompatActivity {

    private static final String ACTIVITY_NOT_FOUND_EXCEPTION = "ActivityNotFoundException";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        Utils.setupSystemBarAppearance(this);
        View container = findViewById(R.id.container_help);
        AppBarLayout appBarLayout = findViewById(R.id.appbar_help);
        Utils.applySystemBarsInsets(container, appBarLayout, null, container);

        Toolbar toolbar = findViewById(R.id.toolbar_help);
        toolbar.setTitle(R.string.help);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        AppCompatButton btnOpenSettings = findViewById(R.id.button_help_open_settings);
        AppCompatButton btnSendEmail = findViewById(R.id.button_help_send_email);

        if (btnOpenSettings != null) btnOpenSettings.setText(R.string.help_settings);
        if (btnSendEmail != null) btnSendEmail.setText(R.string.send_email);

        if (btnOpenSettings != null)
            btnOpenSettings.setOnClickListener(v -> Utils.openAppSettings(this));
        if (btnSendEmail != null) btnSendEmail.setOnClickListener(v -> sendEmail());

        setupExactAlarmButton();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                Utils.overrideCloseTransition(HelpActivity.this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
            }
        });
    }

    /**
     * Exact-alarm access is a special app access, not a runtime permission: it can only be
     * granted on the system screen opened by {@code ACTION_REQUEST_SCHEDULE_EXACT_ALARM}.
     *
     * <p>SCHEDULE_EXACT_ALARM requires a Play Policy declaration. When the user denies it,
     * {@code AlarmHelper} keeps using the documented {@code setAndAllowWhileIdle} fallback, so
     * reminders still arrive — just not to the exact minute. The row is only offered here, never
     * as a startup dialog, and it is hidden once the access is granted.</p>
     */
    private void setupExactAlarmButton() {
        AppCompatButton btnExactAlarm = findViewById(R.id.button_help_exact_alarm);
        if (btnExactAlarm == null) return;

        if (!needsExactAlarmPermission()) {
            btnExactAlarm.setVisibility(View.GONE);
            return;
        }

        btnExactAlarm.setText(R.string.help_exact_alarm);
        btnExactAlarm.setOnClickListener(v -> requestExactAlarmPermission());
    }

    private boolean needsExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false;

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return alarmManager != null && !alarmManager.canScheduleExactAlarms();
    }

    private void requestExactAlarmPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        } catch (ActivityNotFoundException e) {
            // The fallback alarm scheduling still works on devices without this screen.
            Toast.makeText(this, ACTIVITY_NOT_FOUND_EXCEPTION, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendEmail() {
        try {
            String email = getString(R.string.birdays_email);
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse(Constants.MAILTO + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, Build.MANUFACTURER + "/"
                    + Build.MODEL + "/"
                    + Build.VERSION.SDK_INT + "/"
                    + getString(R.string.version_name));
            startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, ACTIVITY_NOT_FOUND_EXCEPTION, Toast.LENGTH_LONG).show();
        }
    }
}
