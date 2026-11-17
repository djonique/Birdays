/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.LocaleList;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.djonique.birdays.R;

import java.util.Locale;

/**
 * Opens the donation page with the two pieces of state the page can use: the language the app is
 * currently showing (including a per-app language on Android 13+) and the light/dark theme.
 *
 * <p>The page itself is bilingual, so every language other than Russian, Belarusian and Kazakh
 * opens the English version.</p>
 */
public final class DonateHelper {

    private static final String LANGUAGE_RUSSIAN = "ru";
    private static final String LANGUAGE_ENGLISH = "en";

    private DonateHelper() {
    }

    public static void openDonatePage(Context context) {
        if (context == null) return;

        Uri donateUri = Uri.parse(context.getString(R.string.donate_url))
                .buildUpon()
                .appendQueryParameter("theme", themeParam(context))
                .appendQueryParameter("lang", languageParam(currentLocale(context)))
                .build();

        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, donateUri));
        } catch (ActivityNotFoundException e) {
            // The same message the other external links use; no browser is installed.
            Toast.makeText(context, R.string.web_search_error, Toast.LENGTH_SHORT).show();
        }
    }

    public static void openPrivacyPolicy(Context context) {
        if (context == null) return;

        Uri privacyUri = Uri.parse(context.getString(R.string.privacy_policy_link))
                .buildUpon()
                .appendQueryParameter("lang", languageParam(currentLocale(context)))
                .build();

        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, privacyUri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.web_search_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * The per-app language wins over the system one, exactly like the rest of the UI.
     */
    private static Locale currentLocale(Context context) {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty()) {
            Locale appLocale = appLocales.get(0);
            if (appLocale != null) return appLocale;
        }

        LocaleList systemLocales = context.getResources().getConfiguration().getLocales();
        if (!systemLocales.isEmpty()) return systemLocales.get(0);
        return Locale.getDefault();
    }

    private static String themeParam(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES ? "dark" : "light";
    }

    /**
     * Maps the app language to the page language. Package-private so it can be unit-tested.
     */
    static String languageParam(@Nullable Locale locale) {
        if (locale == null) return LANGUAGE_ENGLISH;

        String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        if ("ru".equals(language) || "be".equals(language) || "kk".equals(language)) {
            return LANGUAGE_RUSSIAN;
        }
        return LANGUAGE_ENGLISH;
    }
}
