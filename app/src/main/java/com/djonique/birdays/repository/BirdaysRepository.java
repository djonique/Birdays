/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.alarm.AlarmSetter;
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

    /**
     * Reads the XML produced by {@link ExportHelper#writeXml(java.util.List)}.
     *
     * <p>A record that carries no {@code <date>} element or has a blank name cannot be shown by
     * any screen, so it is skipped instead of aborting the restore: one damaged entry used to
     * reject the whole file. The date itself is not range-checked — birthdays before 1970 are
     * stored as negative milliseconds and are perfectly valid.</p>
     */
    static List<Person> parseBackup(InputStream input) throws Exception {
        List<Person> result = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new InputStreamReader(input, StandardCharsets.UTF_8));

        Person person = null;
        boolean hasDate = false;
        int skipped = 0;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if ("person".equals(name)) {
                    person = new Person();
                    hasDate = false;
                } else if (person != null) {
                    switch (name) {
                        case "name" -> person.setName(parser.nextText());
                        case "date" -> {
                            person.setDate(Long.parseLong(parser.nextText()));
                            hasDate = true;
                        }
                        case "year_unknown" ->
                                person.setYearUnknown(Boolean.parseBoolean(parser.nextText()));
                        case "phone_number" -> person.setPhoneNumber(parser.nextText());
                        case "email" -> person.setEmail(parser.nextText());
                        default -> {
                        }
                    }
                }
            } else if (event == XmlPullParser.END_TAG
                    && "person".equals(parser.getName())
                    && person != null) {
                if (hasDate && isUsable(person)) {
                    result.add(person);
                } else {
                    skipped++;
                }
                person = null;
            }
            event = parser.next();
        }
        if (skipped > 0) {
            Log.w(TAG, "Skipped " + skipped + " backup record(s) without a name or a date");
        }
        return result;
    }

    // ---------------------------------------------------------------- reads

    private static boolean isUsable(Person person) {
        return person.getName() != null && !person.getName().trim().isEmpty();
    }

    /**
     * An empty file is almost always a truncated download, so it is rejected and the current data
     * is kept. Individual records are filtered out while parsing (see
     * {@link #parseBackup(InputStream)}); nothing is deleted before this check.
     */
    static void validateBackup(List<Person> persons) {
        if (persons.isEmpty()) {
            throw new IllegalStateException("Backup contains no readable records");
        }
    }

    private static void deleteTempFile(@NonNull Uri uri) {
        if (!"file".equals(uri.getScheme())) return;
        if (uri.getPath() == null) return;
        //noinspection ResultOfMethodCallIgnored
        new File(uri.getPath()).delete();
    }

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

    // --------------------------------------------------------------- writes

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

    public void addPerson(@NonNull Person person, @Nullable Callback<Person> callback) {
        execute(() -> {
            try (DbHelper db = new DbHelper(appContext)) {
                db.addRecord(person);
            }
            new AlarmHelper(appContext).setAlarms(person);
            return person;
        }, callback);
    }

    // --------------------------------------------------------------- backup

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

    /**
     * Cancels and re-creates every alarm. Used as a safety net on app start and after a restore.
     */
    public void rescheduleAllAlarms(@Nullable Callback<Void> callback) {
        execute(() -> {
            AlarmSetter.rescheduleAll(appContext);
            return null;
        }, callback);
    }

    public void createBackupFile(@Nullable Callback<ExportHelper.Backup> callback) {
        execute(() -> ExportHelper.createBackupFile(appContext), callback);
    }

    /**
     * Parses and validates a downloaded backup without touching the stored records, so the user
     * can be asked before anything is replaced. The temporary file is removed immediately; the
     * parsed records stay in memory until {@link #restoreBackup(PreparedBackup, Callback)}.
     *
     * <p>Fails with {@link IllegalStateException} for an empty or unreadable file, which lets the
     * caller show its error message instead of a confirmation dialog.</p>
     */
    public void prepareBackup(@NonNull Uri uri, @Nullable Callback<PreparedBackup> callback) {
        execute(() -> {
            try {
                List<Person> persons = readBackup(uri);
                validateBackup(persons);
                return new PreparedBackup(persons);
            } finally {
                // The downloaded XML is a temporary file in the cache directory. It is deleted on
                // both success and failure so it cannot leave PII behind.
                deleteTempFile(uri);
            }
        }, callback);
    }

    /**
     * Applies a backup the user has confirmed. The table is replaced, never merged.
     */
    public void restoreBackup(@NonNull PreparedBackup backup, @Nullable Callback<Integer> callback) {
        execute(() -> replaceWithBackup(backup.getPersons()), callback);
    }

    private List<Person> readBackup(Uri uri) throws Exception {
        ContentResolver resolver = appContext.getContentResolver();
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Cannot open backup: " + uri);
            }
            return parseBackup(input);
        }
    }

    // -------------------------------------------------------------- contacts

    /**
     * Reads the picked contact's name, phone number and e-mail. The three content-provider
     * queries used to run on the main thread right after the picker returned, which could freeze
     * the screen on a large contact's database.
     */
    public void getContactDetails(@NonNull Uri contactUri,
                                  @Nullable Callback<ContactDetails> callback) {
        execute(() -> {
            ContentResolver resolver = appContext.getContentResolver();
            String name = "";
            String phoneNumber = "";
            String email = "";
            try (Cursor cursor = resolver.query(contactUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String id = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    name = ContactsHelper.getContactName(resolver, id);
                    phoneNumber = ContactsHelper.getContactPhoneNumber(resolver, id);
                    email = ContactsHelper.getContactEmail(resolver, id);
                }
            }
            return new ContactDetails(name, phoneNumber, email);
        }, callback);
    }

    public void importContacts(@Nullable Callback<Integer> callback) {
        execute(() -> {
            List<Person> contacts =
                    ContactsHelper.readContactsWithBirthdays(appContext.getContentResolver());
            return insertNewPersons(contacts);
        }, callback);
    }

    /**
     * Contact import path: adds only records that are not already stored. Deduplication is by
     * name AND date, so two different people with the same display name are both imported while a
     * repeated import of the same contact is not.
     */
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

    /**
     * Drive restore path: the backup is authoritative, so the persons table is replaced rather
     * than merged. Validation happens before anything is deleted, and the delete + insert run in
     * one transaction so a failure leaves the previous data untouched.
     */
    private int replaceWithBackup(List<Person> persons) {
        validateBackup(persons);

        try (DbHelper db = new DbHelper(appContext)) {
            SQLiteDatabase database = db.getWritableDatabase();
            AlarmHelper alarmHelper = new AlarmHelper(appContext);

            // Cancel alarms of the records that are about to disappear so one cannot fire for a
            // person that no longer exists. A scheduling failure is logged, not fatal: the data
            // itself is still restorable.
            for (Person person : db.query().getPersons()) {
                try {
                    alarmHelper.removeAlarms(person.getTimeStamp());
                } catch (RuntimeException e) {
                    Log.w(TAG, "Failed to cancel an alarm before restoring", e);
                }
            }

            database.beginTransaction();
            try {
                db.removeAllRecords();
                for (Person person : persons) {
                    db.addRecord(person);
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            // The rows are committed at this point, so a pending intent that cannot be scheduled
            // must not turn a successful restore into an error.
            for (Person person : persons) {
                try {
                    alarmHelper.setAlarms(person);
                } catch (RuntimeException e) {
                    Log.w(TAG, "Failed to schedule an alarm after restoring", e);
                }
            }
            return persons.size();
        }
    }

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

    // -------------------------------------------------------------- plumbing

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

    /**
     * Main-thread result sink. Never retains an Activity by itself.
     */
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Throwable t);
    }

    /**
     * Parsed and validated backup records waiting for the user's confirmation.
     */
    public static final class PreparedBackup {

        private final List<Person> persons;

        private PreparedBackup(@NonNull List<Person> persons) {
            this.persons = persons;
        }

        public int getRecordCount() {
            return persons.size();
        }

        @NonNull
        List<Person> getPersons() {
            return persons;
        }
    }

    /**
     * Plain snapshot of a picked contact; the activity never sees a Cursor.
     */
    public static final class ContactDetails {
        private final String name;
        private final String phoneNumber;
        private final String email;

        ContactDetails(String name, String phoneNumber, String email) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getEmail() {
            return email;
        }
    }
}
