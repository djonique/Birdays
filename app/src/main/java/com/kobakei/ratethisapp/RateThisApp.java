/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kobakei.ratethisapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import com.djonique.birdays.R;
import com.djonique.birdays.utils.Utils;

public class RateThisApp {

    private static final String PREF_NAME = "RateThisApp";
    private static final String KEY_LAUNCH_COUNT = "launch_count";
    private static final String KEY_INSTALL_DATE = "install_date";
    private static final String KEY_OPT_OUT = "opt_out";

    private static Config mConfig = new Config(7, 5);

    public static class Config {
        private int mDays;
        private int mLaunches;

        public Config(int days, int launches) {
            this.mDays = days;
            this.mLaunches = launches;
        }
    }

    public static void init(Config config) {
        if (config != null) {
            mConfig = config;
        }
    }

    public static void onCreate(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        int launchCount = pref.getInt(KEY_LAUNCH_COUNT, 0) + 1;
        editor.putInt(KEY_LAUNCH_COUNT, launchCount);

        long installDate = pref.getLong(KEY_INSTALL_DATE, 0);
        if (installDate == 0) {
            editor.putLong(KEY_INSTALL_DATE, System.currentTimeMillis());
        }

        editor.apply();
    }

    public static void showRateDialogIfNeeded(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (pref.getBoolean(KEY_OPT_OUT, false)) {
            return;
        }

        int launchCount = pref.getInt(KEY_LAUNCH_COUNT, 0);
        long installDate = pref.getLong(KEY_INSTALL_DATE, 0);

        long daysMs = mConfig.mDays * 86400000L;
        if (launchCount >= mConfig.mLaunches && (System.currentTimeMillis() - installDate) >= daysMs) {
            showRateDialog(context);
        }
    }

    public static void showRateDialog(final Context context) {
        final SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rate_app_key);
        builder.setMessage(R.string.rate_app_key);
        builder.setPositiveButton(R.string.ok_button, (dialog, which) -> {
            pref.edit().putBoolean(KEY_OPT_OUT, true).apply();
            Utils.openBrowser(context, context.getString(R.string.play_market_app_link));
        });
        builder.setNeutralButton(R.string.later, (dialog, which) -> dialog.dismiss());
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> {
            pref.edit().putBoolean(KEY_OPT_OUT, true).apply();
            dialog.dismiss();
        });
        builder.show();
    }
}