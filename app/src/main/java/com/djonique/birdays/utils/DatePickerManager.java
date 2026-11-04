/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Calendar;

public class DatePickerManager {

    private static final String DATE_PICKER_FRAGMENT_TAG = "DATE_PICKER_FRAGMENT_TAG";

    private Activity activity;
    private Calendar calendar;
    private SharedPreferences preferences;

    public DatePickerManager(Activity activity, Calendar calendar) {
        this.activity = activity;
        this.calendar = calendar;
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public void showDialog(DatePickerDialog.OnDateSetListener androidCallback,
                           com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener libCallback) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            showMdtpDialog(libCallback);
        } else {
            showDatePickerDialog(androidCallback);
        }
    }

    private void showDatePickerDialog(DatePickerDialog.OnDateSetListener callback) {
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                activity,
                setTheme(isNightMode()),
                callback,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showMdtpDialog(com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener callback) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        com.wdullaer.materialdatetimepicker.date.DatePickerDialog dpd =
                com.wdullaer.materialdatetimepicker.date.DatePickerDialog.newInstance(
                        callback,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
        dpd.setVersion(com.wdullaer.materialdatetimepicker.date.DatePickerDialog.Version.VERSION_1);
        dpd.setThemeDark(preferences.getBoolean(Constants.NIGHT_MODE_KEY, false));
        if (activity instanceof AppCompatActivity) {
            dpd.show(((AppCompatActivity) activity).getSupportFragmentManager(), DATE_PICKER_FRAGMENT_TAG);
        }
    }

    @SuppressWarnings("deprecation")
    private int setTheme(boolean nightMode) {
        if (nightMode) {
            return AlertDialog.THEME_HOLO_DARK;
        } else {
            return AlertDialog.THEME_HOLO_LIGHT;
        }
    }

    private boolean isNightMode() {
        return preferences.getBoolean(Constants.NIGHT_MODE_KEY, false);
    }
}