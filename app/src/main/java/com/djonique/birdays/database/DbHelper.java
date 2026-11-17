/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbHelper extends SQLiteOpenHelper implements AutoCloseable {

    public static final String COLUMN_NAME = "name";
    public static final String SELECTION_LIKE_NAME = COLUMN_NAME + " LIKE ?";

    static final String DB_PERSONS = "persons";
    static final String DB_FAMOUS = "famous";
    static final String COLUMN_DATE = "date";
    static final String COLUMN_IS_YEAR_KNOWN = "is_year_known";
    static final String COLUMN_PHONE_NUMBER = "phone";
    static final String COLUMN_EMAIL = "email";
    static final String COLUMN_TIME_STAMP = "time_stamp";
    static final String SELECTION_TIME_STAMP = COLUMN_TIME_STAMP + " = ?";

    private static final String DB_NAME = "my_db";
    private static final int DATABASE_VERSION = 2;
    private static final String INDEX_TIME_STAMP = "idx_persons_time_stamp";
    private static final String DB_PERSONS_CREATE = "CREATE TABLE " + DB_PERSONS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT, "
            + COLUMN_DATE + " INTEGER, "
            + COLUMN_IS_YEAR_KNOWN + " INTEGER, "
            + COLUMN_PHONE_NUMBER + " TEXT, "
            + COLUMN_EMAIL + " TEXT, "
            + COLUMN_TIME_STAMP + " INTEGER"
            + ");";
    private static final String DB_PERSONS_TIME_STAMP_INDEX_CREATE =
            "CREATE UNIQUE INDEX IF NOT EXISTS " + INDEX_TIME_STAMP + " ON "
                    + DB_PERSONS + " (" + COLUMN_TIME_STAMP + ");";
    private static final String DB_FAMOUS_CREATE = "CREATE TABLE " + DB_FAMOUS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT, "
            + COLUMN_DATE + " INTEGER"
            + ");";
    private final Context context;
    private DbQueryManager dbQueryManager;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    /**
     * Version 1 -> 2.
     *
     * <p>Version 1 stored wall-clock milliseconds in {@code time_stamp}, so records created in the
     * same millisecond share an identity. They have to be reassigned <em>before</em> the unique
     * index is created, otherwise the migration would abort on a real user database and leave the
     * app unable to open its database.</p>
     *
     * <p>This method must stay callable against a database created by the currently published
     * version (schema v1); the migration test builds exactly such a fixture.</p>
     */
    static void migrateToV2(SQLiteDatabase db, Context context) {
        db.beginTransaction();
        try {
            // The famous table is reference data, not user data: refresh it as before.
            db.execSQL("DROP TABLE IF EXISTS " + DB_FAMOUS);
            db.execSQL(DB_FAMOUS_CREATE);
            DbFamous.createFamousDb(context, db);

            reassignDuplicateTimeStamps(db);
            db.execSQL(DB_PERSONS_TIME_STAMP_INDEX_CREATE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Gives every duplicate {@code time_stamp} a fresh identity. The earliest inserted row of a
     * group keeps its value (so existing alarms/notifications still resolve) and the later rows
     * are reassigned with the same monotonic generator the model uses.
     */
    private static void reassignDuplicateTimeStamps(SQLiteDatabase db) {
        // Read the identities first: updating a table while a cursor over it is open is
        // fragile, and the migration must be predictable on every SQLite build.
        List<long[]> rows = new ArrayList<>();
        try (Cursor cursor = db.query(DB_PERSONS,
                new String[]{BaseColumns._ID, COLUMN_TIME_STAMP},
                null, null, null, null, BaseColumns._ID + " ASC")) {
            while (cursor.moveToNext()) {
                rows.add(new long[]{cursor.getLong(0), cursor.getLong(1)});
            }
        }

        Set<Long> seen = new HashSet<>();
        for (long[] row : rows) {
            long id = row[0];
            long timeStamp = row[1];

            if (seen.add(timeStamp)) {
                continue;
            }

            long replacement = Person.nextTimeStamp();
            while (!seen.add(replacement)) {
                replacement = Person.nextTimeStamp();
            }

            ContentValues cv = new ContentValues();
            cv.put(COLUMN_TIME_STAMP, replacement);
            db.update(DB_PERSONS, cv, BaseColumns._ID + " = ?",
                    new String[]{Long.toString(id)});
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_PERSONS_CREATE);
        db.execSQL(DB_PERSONS_TIME_STAMP_INDEX_CREATE);
        db.execSQL(DB_FAMOUS_CREATE);
        DbFamous.createFamousDb(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            migrateToV2(db, context);
        }
    }

    public synchronized DbQueryManager query() {
        if (dbQueryManager == null) {
            dbQueryManager = new DbQueryManager(getWritableDatabase());
        }
        return dbQueryManager;
    }

    public void addRecord(Person person) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, person.getName());
        cv.put(COLUMN_DATE, person.getDate());
        cv.put(COLUMN_IS_YEAR_KNOWN, Utils.boolToInt(person.isYearUnknown()));
        cv.put(COLUMN_PHONE_NUMBER, person.getPhoneNumber());
        cv.put(COLUMN_EMAIL, person.getEmail());
        cv.put(COLUMN_TIME_STAMP, person.getTimeStamp());
        getWritableDatabase().insert(DB_PERSONS, null, cv);
    }

    public void updateRecord(Person person) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, person.getName());
        cv.put(COLUMN_DATE, person.getDate());
        cv.put(COLUMN_IS_YEAR_KNOWN, Utils.boolToInt(person.isYearUnknown()));
        cv.put(COLUMN_PHONE_NUMBER, person.getPhoneNumber());
        cv.put(COLUMN_EMAIL, person.getEmail());
        getWritableDatabase().update(DB_PERSONS, cv, SELECTION_TIME_STAMP,
                new String[]{String.valueOf(person.getTimeStamp())});
    }

    public void removeRecord(long timeStamp) {
        getWritableDatabase().delete(DB_PERSONS, SELECTION_TIME_STAMP, new String[]{Long.toString(timeStamp)});
    }

    /**
     * Deletes every stored person. Used by the Drive restore, which replaces the table with the
     * backup inside a single transaction.
     */
    public void removeAllRecords() {
        getWritableDatabase().delete(DB_PERSONS, null, null);
    }

    @Override
    public synchronized void close() {
        dbQueryManager = null;
        super.close();
    }
}
