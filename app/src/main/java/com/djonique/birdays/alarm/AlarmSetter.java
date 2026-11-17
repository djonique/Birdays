package com.djonique.birdays.alarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.NamedDaemonThreadFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Re-arms every stored birthday after a system event that invalidates the scheduled alarms.
 *
 * <p>The pending intents survive most of these events, but their trigger times are absolute, so
 * the loud ones (boot, package replacement, clock/time-zone changes) have to be rebuilt. The
 * exact-alarm grant changes whether {@link AlarmManager#setExactAndAllowWhileIdle} is allowed at
 * all, so those alarms are rebuilt too.</p>
 */
public class AlarmSetter extends BroadcastReceiver {

    private static final String ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";

    private static final String[] HANDLED_ACTIONS = {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    };

    /**
     * The receiver only reacts to the actions it declares in the manifest. Kept separate from
     * {@link #onReceive} so it can be verified without a real broadcast dispatch.
     */
    static boolean isHandledAction(String action) {
        for (String handled : HANDLED_ACTIONS) {
            if (handled.equals(action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels and re-creates the alarms of every person. Cancelling first is required because the
     * old PendingIntent is found by request code, not by trigger time: {@code setAlarms} would
     * otherwise update the existing alarm with the same identity.
     */
    public static void rescheduleAll(Context context) {
        try (DbHelper dbHelper = new DbHelper(context)) {
            AlarmHelper alarmHelper = new AlarmHelper(context);
            List<Person> persons = dbHelper.query().getPersons();
            for (Person person : persons) {
                try {
                    alarmHelper.removeAlarms(person.getTimeStamp());
                    alarmHelper.setAlarms(person);
                } catch (SecurityException ignored) {
                    // Exact alarms are unavailable and the inexact fallback is blocked too; the
                    // next app launch retries once the permission state changes.
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isHandledAction(intent.getAction())) return;

        final PendingResult pendingResult = goAsync();
        // The receiver is short-lived; the database must be opened with an application-scoped
        // context so it cannot outlive (and leak) the broadcast context.
        final Context appContext = context.getApplicationContext();
        //noinspection resource
        final ExecutorService executor =
                Executors.newSingleThreadExecutor(new NamedDaemonThreadFactory("alarm-setter"));

        executor.execute(() -> {
            try {
                rescheduleAll(appContext);
            } finally {
                executor.shutdown();
                // goAsync() returns null when the receiver is invoked directly (tests); the
                // broadcast has no pending result to finish in that case.
                if (pendingResult != null) {
                    pendingResult.finish();
                }
            }
        });
    }
}
