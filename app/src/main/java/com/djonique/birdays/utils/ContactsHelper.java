/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.djonique.birdays.R;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsHelper {

    private final Activity activity;
    private final ContentResolver contentResolver;
    private final LoadingContactsListener loadingContactsListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ContactsHelper(Activity activity, ContentResolver contentResolver) {
        this.activity = activity;
        this.contentResolver = contentResolver;
        loadingContactsListener = activity instanceof LoadingContactsListener
                ? (LoadingContactsListener) activity
                : null;
    }

    /**
     * Returns name from certain contact
     */
    public String getContactName(ContentResolver contentResolver, String id) {
        String name = "";
        Cursor nameCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts._ID + " = ?",
                new String[]{id}, null);
        if (nameCursor != null && nameCursor.moveToFirst()) {
            name =
                    nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
        }
        if (nameCursor != null) {
            nameCursor.close();
        }
        return name;
    }

    /**
     * Returns phone number from certain contact
     */
    public String getContactPhoneNumber(ContentResolver contentResolver, String id) {
        String phoneNumber = "";
        Cursor phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{id}, null);
        if (phoneCursor != null && phoneCursor.moveToFirst()) {
            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow
                    (ContactsContract.CommonDataKinds.Phone.NUMBER));
        }
        if (phoneCursor != null) {
            phoneCursor.close();
        }
        return phoneNumber;
    }

    /**
     * Returns email from certain contact
     */
    public String getContactEmail(ContentResolver contentResolver, String id) {
        String email = "";
        Cursor emailCursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{id}, null);
        if (emailCursor != null && emailCursor.moveToFirst()) {
            email = emailCursor.getString(
                    emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA));
        }
        if (emailCursor != null) {
            emailCursor.close();
        }
        return email;
    }

    /**
     * Returns all contacts with Birthdays
     */
    private List<Person> getAllContactsWithBirthdays(ContentResolver contentResolver) {

        List<Person> contacts = new ArrayList<>();

        try (Cursor cursor = getContactsCursor(contentResolver)) {
            if (cursor == null) return contacts;

            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.CONTACT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                String dateString = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE));
                long date;
                try {
                    date = Utils.formatDateToLong(dateString);
                } catch (Exception e) {
                    continue;
                }
                if (date == 0) continue;

                boolean yearUnknown = Utils.isYearUnknown(dateString);
                String phoneNumber = getContactPhoneNumber(contentResolver, id);
                String email = getContactEmail(contentResolver, id);

                Person person = new Person(name, date, yearUnknown, phoneNumber, email);
                contacts.add(person);
            }
        }
        return contacts;
    }

    /**
     * Returns cursor with contacts with specified Birthdays
     */
    private Cursor getContactsCursor(ContentResolver contentResolver) {
        Uri uri = ContactsContract.Data.CONTENT_URI;

        String where =
                ContactsContract.Data.MIMETYPE
                        + "= ? AND "
                        + ContactsContract.CommonDataKinds.Event.TYPE
                        + "="
                        + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;
        String[] selectionArgs = new String[]{ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE};
        return contentResolver.query(uri, null, where, selectionArgs, null);
    }

    /**
     * Loads all persons with Birthdays from Contacts, compares them with persons from Database and
     * saves them into DB, sets alarm for added persons
     */
    public void loadContacts(SharedPreferences preferences) {
        if (PermissionManager.readingContactsPermissionGranted(activity)) {
            try {
                loadContactsInBackground(preferences);
            } catch (Exception e) {
                preferences.edit().putBoolean(Constants.WRONG_CONTACTS_FORMAT, true).apply();
                Toast.makeText(activity, R.string.loading_contacts_error, Toast.LENGTH_LONG).show();
            }
        } else {
            PermissionManager.requestReadingContactsPermission(activity);
        }
    }

    private void loadContactsInBackground(SharedPreferences preferences) {
        if (activity == null || activity.isFinishing()) return;

        final ProgressDialogHelper progressDialogHelper = new ProgressDialogHelper(activity);
        progressDialogHelper.startProgressDialog(activity.getString(R.string.loading_contacts));

        //noinspection resource
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            boolean isSuccess = false;

            try (DbHelper dbHelper = new DbHelper(activity)) {
                AlarmHelper alarmHelper = new AlarmHelper(activity);

                List<Person> dbPersons = dbHelper.query().getPersons();
                List<Person> contacts = getAllContactsWithBirthdays(contentResolver);

                for (Person person : contacts) {
                    if (!Utils.isPersonAlreadyInDb(person, dbPersons)) {
                        dbHelper.addRecord(person);
                        alarmHelper.setAlarms(person);
                    }
                }
                isSuccess = true;
            } catch (Exception e) {
                android.util.Log.e("LoadContacts", "Error loading contacts in background", e);
            } finally {
                executor.shutdown();
            }

            final boolean finalIsSuccess = isSuccess;
            mainHandler.post(() -> {
                //noinspection ConstantValue
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                progressDialogHelper.dismissProgressDialog();

                if (finalIsSuccess) {
                    preferences.edit().putBoolean(Constants.CONTACTS_UPLOADED, true).apply();
                    if (loadingContactsListener != null) {
                        loadingContactsListener.onContactsUploaded();
                    }
                    Utils.notifyWidget(activity);
                    Toast.makeText(activity, R.string.contacts_uploaded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, R.string.recover_records_error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    public interface LoadingContactsListener {
        void onContactsUploaded();
    }
}