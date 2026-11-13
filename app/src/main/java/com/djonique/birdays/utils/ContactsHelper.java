/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsHelper {

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Only the application context is retained. An Activity is passed to {@link #loadContacts}
     * for the permission prompt and the progress dialog and is not stored on the instance, so a
     * helper cannot keep a destroyed Activity alive.
     */
    public ContactsHelper(Context context) {
        this.appContext = context.getApplicationContext();
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
     * Returns all contacts with Birthdays.
     *
     * <p>The previous implementation queried phone numbers and e-mail addresses once per
     * birthday row (2N+1 queries). Both are now fetched in a single query each and joined in
     * memory by contact id, so the import costs three queries in total.</p>
     */
    private List<Person> getAllContactsWithBirthdays(ContentResolver contentResolver) {

        List<Person> contacts = new ArrayList<>();

        Map<String, String> phoneNumbers = getPhoneNumbers(contentResolver);
        Map<String, String> emails = getEmails(contentResolver);

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
                String phoneNumber = phoneNumbers.containsKey(id) ? phoneNumbers.get(id) : "";
                String email = emails.containsKey(id) ? emails.get(id) : "";

                Person person = new Person(name, date, yearUnknown, phoneNumber, email);
                contacts.add(person);
            }
        }
        return contacts;
    }

    private Map<String, String> getPhoneNumbers(ContentResolver contentResolver) {
        Map<String, String> phoneNumbers = new HashMap<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,
                null, null, null)) {
            if (cursor == null) return phoneNumbers;
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String number = cursor.getString(1);
                if (id != null && number != null && !phoneNumbers.containsKey(id)) {
                    phoneNumbers.put(id, number);
                }
            }
        }
        return phoneNumbers;
    }

    private Map<String, String> getEmails(ContentResolver contentResolver) {
        Map<String, String> emails = new HashMap<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.DATA
        };
        try (Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection,
                null, null, null)) {
            if (cursor == null) return emails;
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String email = cursor.getString(1);
                if (id != null && email != null && !emails.containsKey(id)) {
                    emails.put(id, email);
                }
            }
        }
        return emails;
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
    public void loadContacts(Activity activity, SharedPreferences preferences) {
        if (PermissionManager.readingContactsPermissionGranted(activity)) {
            try {
                LoadingContactsListener listener = activity instanceof LoadingContactsListener
                        ? (LoadingContactsListener) activity
                        : null;
                loadContactsInBackground(activity, preferences, listener);
            } catch (Exception e) {
                preferences.edit().putBoolean(Constants.WRONG_CONTACTS_FORMAT, true).apply();
                Toast.makeText(appContext, R.string.loading_contacts_error, Toast.LENGTH_LONG).show();
            }
        } else {
            PermissionManager.requestReadingContactsPermission(activity);
        }
    }

    private void loadContactsInBackground(Activity activity, SharedPreferences preferences,
                                          LoadingContactsListener loadingContactsListener) {
        if (activity == null || activity.isFinishing()) return;

        final ProgressDialogHelper progressDialogHelper = new ProgressDialogHelper(activity);
        progressDialogHelper.startProgressDialog(activity.getString(R.string.loading_contacts));

        //noinspection resource
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final ContentResolver contentResolver = appContext.getContentResolver();

        executor.execute(() -> {
            boolean isSuccess = false;

            try (DbHelper dbHelper = new DbHelper(appContext)) {
                AlarmHelper alarmHelper = new AlarmHelper(appContext);

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
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                progressDialogHelper.dismissProgressDialog();

                if (finalIsSuccess) {
                    preferences.edit().putBoolean(Constants.CONTACTS_UPLOADED, true).apply();
                    if (loadingContactsListener != null) {
                        loadingContactsListener.onContactsUploaded();
                    }
                    Utils.notifyWidget(appContext);
                    Toast.makeText(appContext, R.string.contacts_uploaded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(appContext, R.string.recover_records_error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    public interface LoadingContactsListener {
        void onContactsUploaded();
    }
}
