/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

/**
 * The donation page exists in Russian and English only. These tests pin the mapping from the app
 * language to the page language; the theme/locale lookup itself needs a real {@code Context}.
 */
public class DonateHelperTest {

    @Test
    public void languageParam_russianBelarusianKazakh_returnsRussian() {
        assertEquals("ru", DonateHelper.languageParam(Locale.forLanguageTag("ru")));
        assertEquals("ru", DonateHelper.languageParam(Locale.forLanguageTag("be")));
        assertEquals("ru", DonateHelper.languageParam(Locale.forLanguageTag("kk")));
    }

    @Test
    public void languageParam_otherLanguages_returnsEnglish() {
        assertEquals("en", DonateHelper.languageParam(Locale.ENGLISH));
        assertEquals("en", DonateHelper.languageParam(Locale.forLanguageTag("uk")));
        assertEquals("en", DonateHelper.languageParam(Locale.forLanguageTag("de")));
        // A region-specific tag is reduced to its language by Locale itself.
        assertEquals("ru", DonateHelper.languageParam(Locale.forLanguageTag("ru-KZ")));
    }

    @Test
    public void languageParam_nullLocale_returnsEnglish() {
        assertEquals("en", DonateHelper.languageParam(null));
    }
}
