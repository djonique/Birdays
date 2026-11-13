/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

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
            btnOpenSettings.setOnClickListener(v -> openBatteryOptimization());
        if (btnSendEmail != null) btnSendEmail.setOnClickListener(v -> sendEmail());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                Utils.overrideCloseTransition(HelpActivity.this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
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

    private void openBatteryOptimization() {
        try {
            startActivity(Intent.createChooser(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, ACTIVITY_NOT_FOUND_EXCEPTION, Toast.LENGTH_LONG).show();
        }
    }

    private void sendEmail() {
        String email = getString(R.string.birdays_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse(Constants.MAILTO + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, Build.MANUFACTURER + "/"
                + Build.MODEL + "/"
                + Build.VERSION.SDK_INT + "/"
                + getString(R.string.version_name));
        startActivity(Intent.createChooser(intent, null));
    }
}
