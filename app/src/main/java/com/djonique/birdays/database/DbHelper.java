/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Utils;

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
    private static final int DATABASE_VERSION = 1;
    private static final String DB_PERSONS_CREATE = "CREATE TABLE " + DB_PERSONS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT, "
            + COLUMN_DATE + " INTEGER, "
            + COLUMN_IS_YEAR_KNOWN + " INTEGER, "
            + COLUMN_PHONE_NUMBER + " TEXT, "
            + COLUMN_EMAIL + " TEXT, "
            + COLUMN_TIME_STAMP + " INTEGER"
            + ");";
    private static final String DB_FAMOUS_CREATE = "CREATE TABLE " + DB_FAMOUS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT, "
            + COLUMN_DATE + " INTEGER"
            + ");";

    private final DbQueryManager dbQueryManager;
    private final Context context;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.context = context;
        dbQueryManager = new DbQueryManager(getWritableDatabase());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_PERSONS_CREATE);
        db.execSQL(DB_FAMOUS_CREATE);
        DbFamous.createFamousDb(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.beginTransaction();
            try {
                db.execSQL("DROP TABLE IF EXISTS " + DB_FAMOUS);
                db.execSQL(DB_FAMOUS_CREATE);
                DbFamous.createFamousDb(context, db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public DbQueryManager query() {
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

    @Override
    public void close() {
        super.close();
    }
}
