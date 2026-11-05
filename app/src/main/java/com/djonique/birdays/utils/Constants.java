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

    // Ads
    String INTERSTITIAL_LAST_SHOWN = "interstitial_last_shown";

    // Permission requests
    int READ_CONTACTS_PERMISSION_CODE = 1;
    int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 2;
    int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 3;
    int NOTIFICATIONS_PERMISSION_CODE = 4;

    // Alarm
    String NAME = "NAME";
    String WHEN = "WHEN";
    String TIME_STAMP = "TIME_STAMP";

    // Settings keys
    String NOTIFICATIONS_KEY = "notifications_key";
    String NOTIFICATION_TIME_KEY = "notification_time_key";
    String ADDITIONAL_NOTIFICATION_KEY = "additional_notification_key";
    String RINGTONE_KEY = "ringtone_key";
    String NIGHT_MODE_KEY = "night_mode_key";
    String START_PAGE = "start_page_key";
    String DISPLAYED_AGE_KEY = "displayed_age_key";

    // Intents
    String TYPE_EMAIL = "message/rfc822";
    String MAILTO = "mailto:";
    String TYPE_SMS = "vnd.android-dir/mms-sms";
    String ADDRESS = "address";
    String SMSTO = "smsto:";
    String TEL = "tel: ";
}