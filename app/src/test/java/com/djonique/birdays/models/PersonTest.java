/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/**
 * JVM tests for {@link Person}. Identity is defined by {@code timeStamp}; the ordering used by
 * the lists is month first, then day of month.
 */
public class PersonTest {

    private static long dateOf(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 12, 0, 0);
        return calendar.getTimeInMillis();
    }

    private static Person person(long timeStamp, long date) {
        return new Person("Alice", date, false, "+100", "alice@example.com", timeStamp);
    }

    // --------------------------------------------------------------- equality

    @Test
    public void equals_sameTimeStamp_returnsTrue() {
        Person first = person(42L, dateOf(1990, Calendar.MAY, 15));
        Person second = person(42L, dateOf(2001, Calendar.JANUARY, 1));

        assertEquals(first, second);
    }

    @Test
    public void equals_differentTimeStamp_returnsFalse() {
        Person first = person(42L, dateOf(1990, Calendar.MAY, 15));
        Person second = person(43L, dateOf(1990, Calendar.MAY, 15));

        assertNotEquals(first, second);
    }

    @Test
    public void equals_null_returnsFalse() {
        assertFalse(person(42L, dateOf(1990, Calendar.MAY, 15)).equals(null));
    }

    @Test
    public void equals_unrelatedType_returnsFalse() {
        assertFalse(person(42L, dateOf(1990, Calendar.MAY, 15)).equals("Alice"));
    }

    @Test
    public void equals_sameInstance_returnsTrue() {
        Person person = person(42L, dateOf(1990, Calendar.MAY, 15));

        assertTrue(person.equals(person));
    }

    @Test
    public void hashCode_sameTimeStamp_returnsSameValue() {
        assertEquals(person(42L, dateOf(1990, Calendar.MAY, 15)).hashCode(),
                person(42L, dateOf(2001, Calendar.JANUARY, 1)).hashCode());
    }

    @Test
    public void hashCode_differentTimeStamp_returnsDifferentValue() {
        assertNotEquals(person(42L, dateOf(1990, Calendar.MAY, 15)).hashCode(),
                person(43L, dateOf(1990, Calendar.MAY, 15)).hashCode());
    }

    @Test
    public void hashCode_fiftyDaysApartTimeStamps_doNotCollide() {
        long firstTimeStamp = 1700000000000L;
        long fiftyDaysLater = firstTimeStamp + 4294967296L; // 2^32 ms (~49.7 days)

        // Raw (int) cast collides due to 32-bit truncation:
        assertEquals((int) firstTimeStamp, (int) fiftyDaysLater);

        // Long.hashCode mixes higher and lower 32 bits, avoiding the collision:
        assertNotEquals(Long.hashCode(firstTimeStamp), Long.hashCode(fiftyDaysLater));
    }

    // ---------------------------------------------------------------- ordering

    @Test
    public void compareTo_earlierMonth_returnsNegative() {
        Person may = person(1L, dateOf(1990, Calendar.MAY, 31));
        Person june = person(2L, dateOf(1990, Calendar.JUNE, 1));

        assertTrue(may.compareTo(june) < 0);
    }

    @Test
    public void compareTo_laterMonth_returnsPositive() {
        Person may = person(1L, dateOf(1990, Calendar.MAY, 31));
        Person april = person(2L, dateOf(1990, Calendar.APRIL, 1));

        assertTrue(may.compareTo(april) > 0);
    }

    @Test
    public void compareTo_sameMonthEarlierDay_returnsNegative() {
        Person first = person(1L, dateOf(1990, Calendar.MAY, 14));
        Person second = person(2L, dateOf(2001, Calendar.MAY, 15));

        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    public void compareTo_sameMonthLaterDay_returnsPositive() {
        Person first = person(1L, dateOf(1990, Calendar.MAY, 16));
        Person second = person(2L, dateOf(2001, Calendar.MAY, 15));

        assertTrue(first.compareTo(second) > 0);
    }

    @Test
    public void compareTo_sameMonthAndDay_returnsZero() {
        Person first = person(1L, dateOf(1990, Calendar.MAY, 15));
        Person second = person(2L, dateOf(2001, Calendar.MAY, 15));

        assertEquals(0, first.compareTo(second));
    }

    // ------------------------------------------------------------ month / day

    @Test
    public void getMonth_marchDate_returnsMarch() {
        assertEquals(Calendar.MARCH, person(1L, dateOf(1990, Calendar.MARCH, 1)).getMonth());
    }

    @Test
    public void getDay_lastDayOfMonth_returnsThirtyFirst() {
        assertEquals(31, person(1L, dateOf(1990, Calendar.JANUARY, 31)).getDay());
    }

    @Test
    public void getDay_leapDay_returnsTwentyNine() {
        assertEquals(29, person(1L, dateOf(2000, Calendar.FEBRUARY, 29)).getDay());
    }

    // ----------------------------------------------------------- model basics

    @Test
    public void isPerson_personInstance_returnsTrue() {
        assertTrue(person(1L, dateOf(1990, Calendar.MAY, 15)).isPerson());
    }

    @Test
    public void getters_fullConstructor_returnsAllValues() {
        Person person = new Person("Bob", 123456789L, true, "+200", "bob@example.com", 7L);

        assertEquals("Bob", person.getName());
        assertEquals(123456789L, person.getDate());
        assertTrue(person.isYearUnknown());
        assertEquals("+200", person.getPhoneNumber());
        assertEquals("bob@example.com", person.getEmail());
        assertEquals(7L, person.getTimeStamp());
    }

    @Test
    public void setters_updatedValues_areReturnedByGetters() {
        Person person = new Person("Bob", 1L);
        person.setName("Robert");
        person.setDate(2L);
        person.setYearUnknown(true);
        person.setPhoneNumber("+300");
        person.setEmail("robert@example.com");

        assertEquals("Robert", person.getName());
        assertEquals(2L, person.getDate());
        assertTrue(person.isYearUnknown());
        assertEquals("+300", person.getPhoneNumber());
        assertEquals("robert@example.com", person.getEmail());
    }

    @Test
    public void constructors_generatedTimeStamps_areUnique() {
        // Wall-clock milliseconds used to collide when two records were created in the same
        // millisecond; the monotonic sequence must never produce the same identity twice.
        Person first = new Person();
        Person second = new Person("Bob", 1L, false, "", "");
        Person third = new Person();

        assertNotEquals(first.getTimeStamp(), second.getTimeStamp());
        assertNotEquals(first.getTimeStamp(), third.getTimeStamp());
        assertNotEquals(second.getTimeStamp(), third.getTimeStamp());
    }
}
