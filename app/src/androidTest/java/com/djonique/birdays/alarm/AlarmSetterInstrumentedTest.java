/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.alarm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

/**
 * Verifies that an app update (MY_PACKAGE_REPLACED) re-arms the stored reminders, which the old
 * receiver ignored (it only handled BOOT_COMPLETED).
 */
@RunWith(AndroidJUnit4.class)
public class AlarmSetterInstrumentedTest {

    private static final long ALARM_POLL_TIMEOUT_MS = 5000L;
    private static final long ALARM_POLL_INTERVAL_MS = 100L;

    private Context context;
    private Person person;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Calendar birthday = Calendar.getInstance();
        birthday.add(Calendar.DAY_OF_YEAR, 10);
        person = new Person("Birdays alarm test", birthday.getTimeInMillis(), false, "", "");

        try (DbHelper dbHelper = new DbHelper(context)) {
            dbHelper.addRecord(person);
        }
    }

    @After
    public void tearDown() {
        if (person == null) return;
        try (DbHelper dbHelper = new DbHelper(context)) {
            dbHelper.removeRecord(person.getTimeStamp());
        }
        new AlarmHelper(context).removeAlarms(person.getTimeStamp());
    }

    @Test
    public void myPackageReplaced_isAHandledAction() {
        assertTrue(AlarmSetter.isHandledAction(Intent.ACTION_MY_PACKAGE_REPLACED));
    }

    @Test
    public void myPackageReplaced_rearmsStoredPerson() throws InterruptedException {
        // Direct invocation is the same onReceive() the framework calls for the broadcast.
        new AlarmSetter().onReceive(context, new Intent(Intent.ACTION_MY_PACKAGE_REPLACED));

        PendingIntent alarm = null;
        long deadline = System.currentTimeMillis() + ALARM_POLL_TIMEOUT_MS;
        while (alarm == null && System.currentTimeMillis() < deadline) {
            alarm = PendingIntent.getBroadcast(context, Long.hashCode(person.getTimeStamp()),
                    new Intent(context, AlarmReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (alarm == null) {
                Thread.sleep(ALARM_POLL_INTERVAL_MS);
            }
        }

        assertNotNull("AlarmSetter did not schedule an alarm for the stored person", alarm);
    }
}
