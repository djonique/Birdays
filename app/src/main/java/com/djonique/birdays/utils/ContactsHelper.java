/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.djonique.birdays.models.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless contacts reader.
 *
 * <p>Nothing here holds a Context or an Activity: every method takes the resolver it needs and
 * returns plain data. Bulk imports run on the repository pool
 * ({@code BirdaysRepository.importContacts}); this class only does the reading.</p>
 */
public final class ContactsHelper {

    private ContactsHelper() {
    }

    /**
     * Returns name from certain contact
     */
    public static String getContactName(ContentResolver contentResolver, String id) {
        String name = "";
        try (Cursor nameCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts._ID + " = ?",
                new String[]{id}, null)) {
            if (nameCursor != null && nameCursor.moveToFirst()) {
                name = nameCursor.getString(
                        nameCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            }
        }
        return name;
    }

    /**
     * Returns phone number from certain contact
     */
    public static String getContactPhoneNumber(ContentResolver contentResolver, String id) {
        String phoneNumber = "";
        try (Cursor phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{id}, null)) {
            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow
                        (ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
        }
        return phoneNumber;
    }

    /**
     * Returns email from certain contact
     */
    public static String getContactEmail(ContentResolver contentResolver, String id) {
        String email = "";
        try (Cursor emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{id}, null)) {
            if (emailCursor != null && emailCursor.moveToFirst()) {
                email = emailCursor.getString(
                        emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA));
            }
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
    public static List<Person> readContactsWithBirthdays(ContentResolver contentResolver) {

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

    private static Map<String, String> getPhoneNumbers(ContentResolver contentResolver) {
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

    private static Map<String, String> getEmails(ContentResolver contentResolver) {
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
    private static Cursor getContactsCursor(ContentResolver contentResolver) {
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
}
