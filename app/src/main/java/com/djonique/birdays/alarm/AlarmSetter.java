package com.djonique.birdays.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmSetter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final PendingResult pendingResult = goAsync();

            //noinspection resource
            final ExecutorService executor = Executors.newSingleThreadExecutor();

            executor.execute(() -> {
                try (DbHelper dbHelper = new DbHelper(context)) {
                    AlarmHelper alarmHelper = new AlarmHelper(context);

                    List<Person> persons = dbHelper.query().getPersons();
                    for (Person person : persons) {
                        try {
                            alarmHelper.setAlarms(person);
                        } catch (SecurityException ignored) {
                        }
                    }
                } finally {
                    executor.shutdown();
                    pendingResult.finish();
                }
            });
        }
    }
}