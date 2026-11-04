/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.djonique.birdays.R;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import java.util.ArrayList;
import java.util.List;

public class AlarmSetter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            DbHelper dbHelper = new DbHelper(context);

            AlarmHelper alarmHelper = new AlarmHelper(context);

            List<Person> persons = new ArrayList<>();
            persons.addAll(dbHelper.query().getPersons());

            for (Person person : persons) {
                try {
                    alarmHelper.setAlarms(person);
                } catch (SecurityException e) {
                    Toast.makeText(context, R.string.security_exception, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}