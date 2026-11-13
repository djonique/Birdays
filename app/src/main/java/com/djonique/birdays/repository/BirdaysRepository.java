/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.backup.ExportHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.ContactsHelper;
import com.djonique.birdays.utils.NamedDaemonThreadFactory;
import com.djonique.birdays.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Process-scoped data access point.
 *
 * <p>Every database, file and contacts operation runs on a small daemon pool. Results and errors
 * are delivered on the main thread. The repository never holds an {@code Activity}; callers pass
 * IDs and plain data in, and must re-check their own lifecycle inside {@link Callback}.</p>
 */
public final class BirdaysRepository {

    private static final String TAG = "BirdaysRepository";
    private static final int POOL_SIZE = 2;

    private static volatile BirdaysRepository instance;

    private final Context appContext;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BirdaysRepository(@NonNull Context context) {
        Context application = context.getApplicationContext();
        this.appContext = application != null ? application : context;
        this.executor = Executors.newFixedThreadPool(POOL_SIZE,
                new NamedDaemonThreadFactory("birdays-db"));
    }

    public static BirdaysRepository getInstance(@NonNull Context context) {
        BirdaysRepository local = instance;
        if (local == null) {
            synchronized (BirdaysRepository.class) {
                local = instance;
                if (local == null) {
                    local = new BirdaysRepository(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Main-thread result sink. Never retains an Activity by itself. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Throwable t);
    }

    // ---------------------------------------------------------------- reads

    public void getPersons(@Nullable Callback<List<Person>> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                return db.query().getPersons();
            }
        }, callback);
    }

    public void getPerson(long timeStamp, @Nullable Callback<Person> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                return db.query().getPerson(timeStamp);
            }
        }, callback);
    }

    public void getThisMonthPersons(@Nullable Callback<List<Person>> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                return db.query().getThisMonthPersons();
            }
        }, callback);
    }

    public void searchPersons(@Nullable String query, boolean currentMonthOnly,
                              @Nullable Callback<List<Person>> callback) {
        final String like = "%" + (query == null ? "" : query.trim()) + "%";
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                if (currentMonthOnly) {
                    return db.query().getSearchMonthPerson(DbHelper.SELECTION_LIKE_NAME,
                            new String[]{like}, DbHelper.COLUMN_NAME);
                }
                return db.query().getSearchPerson(DbHelper.SELECTION_LIKE_NAME,
                        new String[]{like}, DbHelper.COLUMN_NAME);
            }
        }, callback);
    }

    public void getFamousBornThisDay(long day, @Nullable Callback<List<Person>> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                return db.query().getFamousBornThisDay(day);
            }
        }, callback);
    }

    // --------------------------------------------------------------- writes

    public void addPerson(@NonNull Person person, @Nullable Callback<Person> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                db.addRecord(person);
            }
            new AlarmHelper(appContext).setAlarms(person);
            return person;
        }, callback);
    }

    public void updatePerson(@NonNull Person person, @Nullable Callback<Person> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                db.updateRecord(person);
            }
            AlarmHelper alarmHelper = new AlarmHelper(appContext);
            alarmHelper.removeAlarms(person.getTimeStamp());
            alarmHelper.setAlarms(person);
            return person;
        }, callback);
    }

    public void removePerson(long timeStamp, @Nullable Callback<Boolean> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                db.removeRecord(timeStamp);
            }
            new AlarmHelper(appContext).removeAlarms(timeStamp);
            return Boolean.TRUE;
        }, callback);
    }

    // --------------------------------------------------------------- backup

    public void createBackupFile(@Nullable Callback<File> callback) {
        execute(() -> ExportHelper.createBackupFile(appContext), callback);
    }

    public void restoreBackup(@NonNull Uri uri, @Nullable Callback<Integer> callback) {
        execute(() -> insertNewPersons(readBackup(uri)), callback);
    }

    private List<Person> readBackup(Uri uri) throws Exception {
        List<Person> result = new ArrayList<>();
        ContentResolver resolver = appContext.getContentResolver();
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Cannot open backup: " + uri);
            }
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new InputStreamReader(input, StandardCharsets.UTF_8));

            Person person = null;
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("person".equals(name)) {
                        person = new Person();
                    } else if (person != null) {
                        switch (name) {
                            case "name" -> person.setName(parser.nextText());
                            case "date" -> person.setDate(Long.parseLong(parser.nextText()));
                            case "year_unknown" -> person.setYearUnknown(Boolean.parseBoolean(parser.nextText()));
                            case "phone_number" -> person.setPhoneNumber(parser.nextText());
                            case "email" -> person.setEmail(parser.nextText());
                            default -> {
                            }
                        }
                    }
                } else if (event == XmlPullParser.END_TAG
                        && "person".equals(parser.getName())
                        && person != null) {
                    result.add(person);
                    person = null;
                }
                event = parser.next();
            }
        }
        return result;
    }

    // -------------------------------------------------------------- contacts

    public void importContacts(@Nullable Callback<Integer> callback) {
        execute(() -> {
            List<Person> contacts =
                    ContactsHelper.readContactsWithBirthdays(appContext.getContentResolver());
            return insertNewPersons(contacts);
        }, callback);
    }

    private int insertNewPersons(List<Person> persons) {
        int inserted = 0;
        try (DbHelper db = new DbHelper(appContext)) {
            List<Person> existing = db.query().getPersons();
            SQLiteDatabase database = db.getWritableDatabase();
            database.beginTransaction();
            try {
                AlarmHelper alarmHelper = new AlarmHelper(appContext);
                for (Person person : persons) {
                    if (!Utils.isPersonAlreadyInDb(person, existing)) {
                        db.addRecord(person);
                        existing.add(person);
                        alarmHelper.setAlarms(person);
                        inserted++;
                    }
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
        return inserted;
    }

    // -------------------------------------------------------------- plumbing

    private <T> void execute(Callable<T> work, @Nullable Callback<T> callback) {
        try {
            executor.execute(() -> {
                try {
                    T result = work.call();
                    postSuccess(callback, result);
                } catch (Throwable t) {
                    Log.e(TAG, "Repository task failed", t);
                    postError(callback, t);
                }
            });
        } catch (RejectedExecutionException e) {
            postError(callback, e);
        }
    }

    private <T> void postSuccess(@Nullable Callback<T> callback, T result) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void postError(@Nullable Callback<?> callback, Throwable t) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onError(t));
    }

    /**
     * Drains the pool. Called from {@code BirdaysApplication.onTerminate()} and from tests only.
     * Production correctness does not depend on it: the threads are daemons and die with the
     * process. Do not call it from an Activity — other screens may still have work queued.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
