/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the v1 -> v2 migration against a fixture database that has exactly the schema of the
 * currently published version and contains duplicate {@code time_stamp} values (which the old
 * wall-clock generator could produce). Verifies that the unique index can be created and that no
 * user row is lost.
 */
@RunWith(AndroidJUnit4.class)
public class DbHelperMigrationInstrumentedTest {

    private static final String TEST_DB_NAME = "birdays_migration_test_db";
    private static final long DUPLICATE_TIME_STAMP = 1000L;
    private static final long UNIQUE_TIME_STAMP = 2000L;

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(TEST_DB_NAME);
    }

    @Test
    public void migrateToV2_withDuplicateTimeStamps_createsIndexAndKeepsRows() {
        File dbFile = context.getDatabasePath(TEST_DB_NAME);

        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
            // The exact v1 schema, written out instead of using DbHelper so the fixture cannot
            // silently follow the new code.
            db.execSQL("CREATE TABLE persons (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT, date INTEGER, is_year_known INTEGER, phone TEXT, email TEXT, "
                    + "time_stamp INTEGER)");
            insert(db, "Alice", DUPLICATE_TIME_STAMP);
            insert(db, "Alice again", DUPLICATE_TIME_STAMP);
            insert(db, "Bob", UNIQUE_TIME_STAMP);

            DbHelper.migrateToV2(db, context);

            assertTrue("unique time_stamp index was not created",
                    indexExists(db, "idx_persons_time_stamp"));

            List<Long> timeStamps = readTimeStamps(db);
            assertEquals("the migration lost or added rows", 3, timeStamps.size());
            assertEquals("the earliest row must keep its identity",
                    DUPLICATE_TIME_STAMP, timeStamps.get(0).longValue());
            assertEquals("a non-duplicate identity must be untouched",
                    UNIQUE_TIME_STAMP, timeStamps.get(2).longValue());
            assertNotEquals("the duplicate row was not reassigned",
                    DUPLICATE_TIME_STAMP, timeStamps.get(1).longValue());
            assertNotEquals("the duplicate row kept another row's identity",
                    UNIQUE_TIME_STAMP, timeStamps.get(1).longValue());

            Set<Long> distinct = new HashSet<>(timeStamps);
            assertEquals("time_stamp is still not unique", timeStamps.size(), distinct.size());
        }
    }

    private static void insert(SQLiteDatabase db, String name, long timeStamp) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("date", 631152000000L);
        cv.put("is_year_known", 1);
        cv.put("phone", "");
        cv.put("email", "");
        cv.put("time_stamp", timeStamp);
        db.insertOrThrow("persons", null, cv);
    }

    private static List<Long> readTimeStamps(SQLiteDatabase db) {
        List<Long> result = new ArrayList<>();
        try (Cursor cursor = db.query("persons", new String[]{"time_stamp"},
                null, null, null, null, "_id ASC")) {
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(0));
            }
        }
        return result;
    }

    private static boolean indexExists(SQLiteDatabase db, String name) {
        try (Cursor cursor = db.query("sqlite_master", new String[]{"name"},
                "type = ? AND name = ?", new String[]{"index", name}, null, null, null)) {
            return cursor.moveToFirst();
        }
    }
}
