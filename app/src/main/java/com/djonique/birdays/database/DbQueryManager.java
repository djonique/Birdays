/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class DbQueryManager {

    private final SQLiteDatabase database;

    DbQueryManager(SQLiteDatabase database) {
        this.database = database;
    }

    public Person getPerson(long timeStamp) {
        Person foundPerson = null;
        try (Cursor cursor = database.query(DbHelper.DB_PERSONS, null, DbHelper.SELECTION_TIME_STAMP,
                new String[]{Long.toString(timeStamp)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                String name = getName(cursor);
                long date = getDate(cursor);
                boolean isYearKnown = getYearUnknown(cursor);
                String phoneNumber = getPhoneNumber(cursor);
                String email = getEmail(cursor);

                foundPerson = new Person(name, date, isYearKnown, phoneNumber, email, timeStamp);
            }
        }
        return foundPerson;
    }

    public List<Person> getPersons() {
        List<Person> personList = new ArrayList<>();
        try (Cursor cursor = database.query(DbHelper.DB_PERSONS, null, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String name = getName(cursor);
                    long date = getDate(cursor);
                    boolean isYearKnown = getYearUnknown(cursor);
                    String phoneNumber = getPhoneNumber(cursor);
                    String email = getEmail(cursor);
                    long timeStamp = getTimeStamp(cursor);

                    personList.add(new Person(name, date, isYearKnown, phoneNumber, email, timeStamp));
                } while (cursor.moveToNext());
            }
        }
        return personList;
    }

    public List<Person> getSearchPerson(String selection, String[] selectionArgs, String orderBy) {
        List<Person> personList = new ArrayList<>();
        try (Cursor cursor = database.query(DbHelper.DB_PERSONS, null, selection, selectionArgs, null, null, orderBy)) {
            if (cursor.moveToFirst()) {
                do {
                    String name = getName(cursor);
                    long date = getDate(cursor);
                    boolean isYearKnown = getYearUnknown(cursor);
                    String phoneNumber = getPhoneNumber(cursor);
                    String email = getEmail(cursor);
                    long timeStamp = getTimeStamp(cursor);

                    personList.add(new Person(name, date, isYearKnown, phoneNumber, email, timeStamp));
                } while (cursor.moveToNext());
            }
        }
        return personList;
    }

    public List<Person> getThisMonthPersons() {
        List<Person> personList = new ArrayList<>();
        try (Cursor cursor = database.query(DbHelper.DB_PERSONS, null, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String name = getName(cursor);
                    long date = getDate(cursor);
                    boolean isYearKnown = getYearUnknown(cursor);
                    String phoneNumber = getPhoneNumber(cursor);
                    String email = getEmail(cursor);
                    long timeStamp = getTimeStamp(cursor);

                    if (Utils.isCurrentMonth(date)) {
                        personList.add(new Person(name, date, isYearKnown, phoneNumber, email, timeStamp));
                    }
                } while (cursor.moveToNext());
            }
        }
        return personList;
    }

    public List<Person> getSearchMonthPerson(String selection, String[] selectionArgs, String orderBy) {
        List<Person> personList = new ArrayList<>();
        try (Cursor cursor = database.query(DbHelper.DB_PERSONS, null, selection, selectionArgs, null, null, orderBy)) {
            if (cursor.moveToFirst()) {
                do {
                    String name = getName(cursor);
                    long date = getDate(cursor);
                    boolean isYearKnown = getYearUnknown(cursor);
                    String phoneNumber = getPhoneNumber(cursor);
                    String email = getEmail(cursor);
                    long timeStamp = getTimeStamp(cursor);

                    if (Utils.isCurrentMonth(date)) {
                        personList.add(new Person(name, date, isYearKnown, phoneNumber, email, timeStamp));
                    }
                } while (cursor.moveToNext());
            }
        }
        return personList;
    }

    public List<Person> getFamousBornThisDay(long dayOfBirthday) {
        List<Person> personList = new ArrayList<>();
        try (Cursor cursor = database.query(DbHelper.DB_FAMOUS, null, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String name = getName(cursor);
                    long date = getDate(cursor);

                    if (Utils.getMonth(date) == Utils.getMonth(dayOfBirthday) &&
                            Utils.getDay(date) == Utils.getDay(dayOfBirthday)) {
                        personList.add(new Person(name, date));
                    }
                } while (cursor.moveToNext());
            }
        }
        return personList;
    }

    private String getName(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_NAME));
    }

    private long getDate(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DATE));
    }

    private boolean getYearUnknown(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_IS_YEAR_KNOWN)) == 1;
    }

    private String getPhoneNumber(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_PHONE_NUMBER));
    }

    private String getEmail(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_EMAIL));
    }

    private long getTimeStamp(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_STAMP));
    }
}
