/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

public interface Constants {

    // Preferences
    String CONTACTS_UPLOADED = "CONTACTS_UPLOADED";
    String WRONG_CONTACTS_FORMAT = "WRONG_CONTACTS_FORMAT";

    // Permission requests
    int READ_CONTACTS_PERMISSION_CODE = 1;
    int NOTIFICATIONS_PERMISSION_CODE = 2;

    // Alarm
    String NAME = "NAME";
    String WHEN = "WHEN";
    String TIME_STAMP = "TIME_STAMP";
    /**
     * Distinguishes the additional reminder from the main alarm. The request code alone is not
     * unique (it is {@code timeStamp + 99}), so the action is what keeps the two PendingIntents
     * apart for any pair of records.
     */
    String ACTION_ADDITIONAL_ALARM = "com.djonique.birdays.ADDITIONAL_ALARM";

    // Settings keys
    String NOTIFICATIONS_KEY = "notifications_key";
    String NOTIFICATION_TIME_KEY = "notification_time_key";
    /**
     * Stored as a local "HH:mm" string; the legacy value was an absolute timestamp.
     */
    String DEFAULT_NOTIFICATION_TIME = "10:00";
    String ADDITIONAL_NOTIFICATION_KEY = "additional_notification_key";
    String RINGTONE_KEY = "ringtone_key";
    String NIGHT_MODE_KEY = "night_mode_key";
    String START_PAGE = "start_page_key";
    String DISPLAYED_AGE_KEY = "displayed_age_key";

    // Intents
    String MAILTO = "mailto:";
    String ADDRESS = "address";
    String SMSTO = "smsto:";
    String TEL = "tel:";
}
