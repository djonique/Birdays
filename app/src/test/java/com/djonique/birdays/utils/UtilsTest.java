/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.djonique.birdays.R;
import com.djonique.birdays.models.Person;

import org.junit.Assume;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * JVM tests for the pure date/validation helpers in {@link Utils}.
 *
 * <p>Every method under test here only uses {@link Calendar}/strings, so no Android framework
 * method is invoked. {@link Context} is mocked for {@link Utils#daysLeft(Context, long)}.</p>
 */
public class UtilsTest {

    private static long dateOf(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 12, 0, 0);
        return calendar.getTimeInMillis();
    }

    /** Today shifted by whole years and days; keeps month/day of today when {@code days == 0}. */
    private static long shiftedToday(int years, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -years);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTimeInMillis();
    }

    private static boolean isLeapDayToday() {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.MONTH) == Calendar.FEBRUARY
                && today.get(Calendar.DAY_OF_MONTH) == 29;
    }

    // ------------------------------------------------------------------ zodiac

    @Test
    public void getZodiacId_januaryBoundary_returnsCapricornThenAquarius() {
        assertEquals(R.string.capricorn, Utils.getZodiacId(dateOf(1990, Calendar.JANUARY, 20)));
        assertEquals(R.string.aquarius, Utils.getZodiacId(dateOf(1990, Calendar.JANUARY, 21)));
    }

    @Test
    public void getZodiacId_februaryBoundary_returnsAquariusThenPisces() {
        assertEquals(R.string.aquarius, Utils.getZodiacId(dateOf(1990, Calendar.FEBRUARY, 19)));
        assertEquals(R.string.pisces, Utils.getZodiacId(dateOf(1990, Calendar.FEBRUARY, 20)));
    }

    @Test
    public void getZodiacId_marchBoundary_returnsPiscesThenAries() {
        assertEquals(R.string.pisces, Utils.getZodiacId(dateOf(1990, Calendar.MARCH, 20)));
        assertEquals(R.string.aries, Utils.getZodiacId(dateOf(1990, Calendar.MARCH, 21)));
    }

    @Test
    public void getZodiacId_aprilBoundary_returnsAriesThenTaurus() {
        assertEquals(R.string.aries, Utils.getZodiacId(dateOf(1990, Calendar.APRIL, 20)));
        assertEquals(R.string.taurus, Utils.getZodiacId(dateOf(1990, Calendar.APRIL, 21)));
    }

    @Test
    public void getZodiacId_mayBoundary_returnsTaurusThenGemini() {
        assertEquals(R.string.taurus, Utils.getZodiacId(dateOf(1990, Calendar.MAY, 21)));
        assertEquals(R.string.gemini, Utils.getZodiacId(dateOf(1990, Calendar.MAY, 22)));
    }

    @Test
    public void getZodiacId_juneBoundary_returnsGeminiThenCancer() {
        assertEquals(R.string.gemini, Utils.getZodiacId(dateOf(1990, Calendar.JUNE, 21)));
        assertEquals(R.string.cancer, Utils.getZodiacId(dateOf(1990, Calendar.JUNE, 22)));
    }

    @Test
    public void getZodiacId_julyBoundary_returnsCancerThenLeo() {
        assertEquals(R.string.cancer, Utils.getZodiacId(dateOf(1990, Calendar.JULY, 22)));
        assertEquals(R.string.leo, Utils.getZodiacId(dateOf(1990, Calendar.JULY, 23)));
    }

    @Test
    public void getZodiacId_augustBoundary_returnsLeoThenVirgo() {
        assertEquals(R.string.leo, Utils.getZodiacId(dateOf(1990, Calendar.AUGUST, 22)));
        assertEquals(R.string.virgo, Utils.getZodiacId(dateOf(1990, Calendar.AUGUST, 23)));
    }

    @Test
    public void getZodiacId_septemberBoundary_returnsVirgoThenLibra() {
        assertEquals(R.string.virgo, Utils.getZodiacId(dateOf(1990, Calendar.SEPTEMBER, 23)));
        assertEquals(R.string.libra, Utils.getZodiacId(dateOf(1990, Calendar.SEPTEMBER, 24)));
    }

    @Test
    public void getZodiacId_octoberBoundary_returnsLibraThenScorpio() {
        assertEquals(R.string.libra, Utils.getZodiacId(dateOf(1990, Calendar.OCTOBER, 23)));
        assertEquals(R.string.scorpio, Utils.getZodiacId(dateOf(1990, Calendar.OCTOBER, 24)));
    }

    @Test
    public void getZodiacId_novemberBoundary_returnsScorpioThenSagittarius() {
        assertEquals(R.string.scorpio, Utils.getZodiacId(dateOf(1990, Calendar.NOVEMBER, 22)));
        assertEquals(R.string.sagittarius, Utils.getZodiacId(dateOf(1990, Calendar.NOVEMBER, 23)));
    }

    @Test
    public void getZodiacId_decemberBoundary_returnsSagittariusThenCapricorn() {
        assertEquals(R.string.sagittarius, Utils.getZodiacId(dateOf(1990, Calendar.DECEMBER, 21)));
        assertEquals(R.string.capricorn, Utils.getZodiacId(dateOf(1990, Calendar.DECEMBER, 22)));
    }

    @Test
    public void getZodiacImage_knownSign_returnsMatchingDrawable() {
        assertEquals(R.drawable.ic_aries, Utils.getZodiacImage(R.string.aries));
        assertEquals(R.drawable.ic_pisces, Utils.getZodiacImage(R.string.pisces));
        assertEquals(R.drawable.ic_capricorn, Utils.getZodiacImage(R.string.capricorn));
    }

    @Test
    public void getZodiacImage_unknownResourceId_returnsZero() {
        assertEquals(0, Utils.getZodiacImage(Integer.MIN_VALUE));
    }

    // --------------------------------------------------------------- month/day

    @Test
    public void getMonth_decemberDate_returnsDecember() {
        assertEquals(Calendar.DECEMBER, Utils.getMonth(dateOf(2001, Calendar.DECEMBER, 31)));
    }

    @Test
    public void getDay_lastDayOfFebruary_returns28InNonLeapYear() {
        assertEquals(28, Utils.getDay(dateOf(2001, Calendar.FEBRUARY, 28)));
    }

    @Test
    public void getDay_leapDay_returns29() {
        assertEquals(29, Utils.getDay(dateOf(2000, Calendar.FEBRUARY, 29)));
    }

    // ------------------------------------------------------------------- ages

    @Test
    public void getCurrentAge_birthdayExactlyThirtyYearsAgo_returns30() {
        Assume.assumeFalse("Leap-day birth dates roll into March", isLeapDayToday());
        assertEquals(30, Utils.getCurrentAge(shiftedToday(30, 0)));
    }

    @Test
    public void getCurrentAge_birthdayTomorrow_returns29() {
        assertEquals(29, Utils.getCurrentAge(shiftedToday(30, 1)));
    }

    @Test
    public void getCurrentAge_birthdayYesterday_returns30() {
        assertEquals(30, Utils.getCurrentAge(shiftedToday(30, -1)));
    }

    @Test
    public void getFutureAge_birthdayTomorrow_returns30() {
        assertEquals(30, Utils.getFutureAge(shiftedToday(30, 1)));
    }

    @Test
    public void getFutureAge_birthdayYesterday_returns31() {
        assertEquals(31, Utils.getFutureAge(shiftedToday(30, -1)));
    }

    @Test
    public void daysSinceBirthday_today_returnsZero() {
        assertEquals("0", Utils.daysSinceBirthday(System.currentTimeMillis()));
    }

    // ------------------------------------------------------------------ dates

    @Test
    public void isRightDate_yesterday_returnsTrue() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        assertTrue(Utils.isRightDate(yesterday));
    }

    @Test
    public void isRightDate_tomorrow_returnsFalse() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        assertFalse(Utils.isRightDate(tomorrow));
    }

    @Test
    public void isCurrentMonth_today_returnsTrue() {
        assertTrue(Utils.isCurrentMonth(System.currentTimeMillis()));
    }

    @Test
    public void isCurrentMonth_sixMonthsFromNow_returnsFalse() {
        Calendar sixMonths = Calendar.getInstance();
        sixMonths.add(Calendar.MONTH, 6);
        assertFalse(Utils.isCurrentMonth(sixMonths.getTimeInMillis()));
    }

    @Test
    public void isCurrentMonth_sameMonthOneYearAgo_returnsTrue() {
        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);
        assertTrue(Utils.isCurrentMonth(oneYearAgo.getTimeInMillis()));
    }

    @Test
    public void daysLeft_birthdayToday_returnsTodayString() {
        Context context = mock(Context.class);
        when(context.getString(R.string.today)).thenReturn("Today");

        assertEquals("Today", Utils.daysLeft(context, System.currentTimeMillis()));
    }

    @Test
    public void daysLeft_birthdayTomorrow_returnsOneDay() {
        Context context = mock(Context.class);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        assertEquals("1", Utils.daysLeft(context, tomorrow.getTimeInMillis()));
    }

    // ------------------------------------------------------------ conversions

    @Test
    public void boolToInt_true_returnsOne() {
        assertEquals(1, Utils.boolToInt(true));
    }

    @Test
    public void boolToInt_false_returnsZero() {
        assertEquals(0, Utils.boolToInt(false));
    }

    // ------------------------------------------------------- contacts parsing

    @Test
    public void formatDateToLong_fullDate_returnsSameCalendarDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Utils.formatDateToLong("1990-05-15"));

        assertEquals(1990, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, calendar.get(Calendar.MONTH));
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void formatDateToLong_yearUnknown_returnsMonthAndDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Utils.formatDateToLong("--05-15"));

        assertEquals(Calendar.MAY, calendar.get(Calendar.MONTH));
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void formatDateToLong_malformedString_throwsNumberFormatException() {
        try {
            Utils.formatDateToLong("not-a-date");
        } catch (NumberFormatException expected) {
            return;
        }
        throw new AssertionError("NumberFormatException was expected for a malformed date");
    }

    @Test
    public void isYearUnknown_contactsDateWithoutYear_returnsTrue() {
        assertTrue(Utils.isYearUnknown("--05-15"));
    }

    @Test
    public void isYearUnknown_contactsDateWithYear_returnsFalse() {
        assertFalse(Utils.isYearUnknown("1990-05-15"));
    }

    @Test
    public void isYearUnknown_malformedString_returnsFalse() {
        assertFalse(Utils.isYearUnknown("not-a-date"));
    }

    // ---------------------------------------------------------------- duplicates

    @Test
    public void isPersonAlreadyInDb_sameNameAndDateDifferentCase_returnsTrue() {
        Person candidate = new Person("Alice", dateOf(1990, Calendar.MAY, 15));
        List<Person> db = Collections.singletonList(
                new Person("aLiCe", dateOf(1990, Calendar.MAY, 15)));

        assertTrue(Utils.isPersonAlreadyInDb(candidate, db));
    }

    @Test
    public void isPersonAlreadyInDb_sameNameDifferentDate_returnsFalse() {
        Person candidate = new Person("Alice", dateOf(1990, Calendar.MAY, 15));
        List<Person> db = Collections.singletonList(
                new Person("Alice", dateOf(1990, Calendar.MAY, 16)));

        assertFalse(Utils.isPersonAlreadyInDb(candidate, db));
    }

    @Test
    public void isPersonAlreadyInDb_nameNotPresent_returnsFalse() {
        Person candidate = new Person("Alice", dateOf(1990, Calendar.MAY, 15));
        List<Person> db = Arrays.asList(
                new Person("Bob", dateOf(1991, Calendar.JUNE, 1)),
                new Person("Carol", dateOf(1992, Calendar.JULY, 2)));

        assertFalse(Utils.isPersonAlreadyInDb(candidate, db));
    }

    @Test
    public void isPersonAlreadyInDb_emptyDatabase_returnsFalse() {
        Person candidate = new Person("Alice", dateOf(1990, Calendar.MAY, 15));

        assertFalse(Utils.isPersonAlreadyInDb(candidate, new ArrayList<>()));
    }

    @Test
    public void isPersonAlreadyInDb_nullName_returnsFalse() {
        Person candidate = new Person(null, dateOf(1990, Calendar.MAY, 15));
        List<Person> db = Collections.singletonList(
                new Person(null, dateOf(1991, Calendar.JUNE, 1)));

        assertFalse(Utils.isPersonAlreadyInDb(candidate, db));
    }

    @Test
    public void isPersonAlreadyInDb_databasePersonWithNullName_returnsFalse() {
        Person candidate = new Person("Alice", dateOf(1990, Calendar.MAY, 15));
        List<Person> db = Collections.singletonList(
                new Person(null, dateOf(1991, Calendar.JUNE, 1)));

        assertFalse(Utils.isPersonAlreadyInDb(candidate, db));
    }
}
