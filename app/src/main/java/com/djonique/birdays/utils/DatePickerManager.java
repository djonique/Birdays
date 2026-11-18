/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;

import com.djonique.birdays.R;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Shows the Material date picker. The platform {@code DatePickerDialog} is not used:
 * it inherits text colors from private framework attributes that AppCompat-based
 * themes do not resolve, which left the calendar numbers invisible in the dark theme.
 * {@link MaterialDatePicker} is drawn with the Material theme roles instead, so it
 * follows the app palette in both schemes.
 */
public class DatePickerManager {

    private static final String DATE_PICKER_TAG = "DATE_PICKER_TAG";

    private final Activity activity;
    private final Calendar calendar;

    public DatePickerManager(Activity activity, Calendar calendar) {
        this.activity = activity;
        this.calendar = calendar;
    }

    public void showDialog(OnDatePicked listener) {
        if (!(activity instanceof AppCompatActivity)) return;

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.birthday)
                .setSelection(toUtc(calendar))
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // MaterialDatePicker works with UTC midnight, the app stores local dates.
            Calendar selected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selected.setTimeInMillis(selection);
            listener.onDatePicked(selected.get(Calendar.YEAR),
                    selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH));
        });
        datePicker.show(((AppCompatActivity) activity).getSupportFragmentManager(),
                DATE_PICKER_TAG);
    }

    private static long toUtc(Calendar calendar) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    public interface OnDatePicked {
        void onDatePicked(int year, int month, int dayOfMonth);
    }
}
