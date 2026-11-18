/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.models;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

public class Person implements Item, Comparable<Person> {

    /**
     * {@code time_stamp} is the record identity (edits, deletes, notification IDs and alarm
     * request codes). Wall-clock milliseconds are not unique: two records created within the same
     * millisecond received the same identity. A process-wide monotonic sequence removes the
     * collision while keeping the same magnitude and type.
     */
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(System.currentTimeMillis());
    /**
     * Calendar instances are mutable and not thread-safe. A single shared instance used to be
     * reused by every caller; a per-thread instance keeps the allocation cost low without
     * sharing state between the main thread and background work.
     */
    private static final ThreadLocal<Calendar> CALENDAR = ThreadLocal.withInitial(Calendar::getInstance);
    private String name, phoneNumber, email;
    private long date, timeStamp;
    private boolean yearUnknown;

    public Person() {
        this.timeStamp = nextTimeStamp();
    }

    public Person(String name, long date) {
        this.name = name;
        this.date = date;
    }

    public Person(String name, long date, boolean yearUnknown, String phoneNumber, String email) {
        this.name = name;
        this.date = date;
        this.yearUnknown = yearUnknown;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.timeStamp = nextTimeStamp();
    }

    public Person(String name, long date, boolean yearUnknown, String phoneNumber, String email,
                  long timeStamp) {
        this.name = name;
        this.date = date;
        this.yearUnknown = yearUnknown;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.timeStamp = timeStamp;
    }

    /**
     * Next value for {@code time_stamp}. Also used by the v1 -> v2 migration to reassign
     * duplicate identities, so newly generated values can never collide with kept ones.
     */
    public static long nextTimeStamp() {
        return ID_SEQUENCE.incrementAndGet();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isYearUnknown() {
        return yearUnknown;
    }

    public void setYearUnknown(boolean yearUnknown) {
        this.yearUnknown = yearUnknown;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public boolean isPerson() {
        return true;
    }

    public int getMonth() {
        Calendar calendar = CALENDAR.get();
        calendar.setTimeInMillis(this.date);
        return calendar.get(Calendar.MONTH);
    }

    public int getDay() {
        Calendar calendar = CALENDAR.get();
        calendar.setTimeInMillis(this.date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Person person)) {
            return false;
        }
        return timeStamp == person.timeStamp;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(timeStamp);
    }

    @Override
    public int compareTo(@NonNull Person person) {
        if (this.getMonth() < person.getMonth()) {
            return -1;
        } else if (this.getMonth() == person.getMonth()) {
            return this.getDay() - person.getDay();
        } else {
            return 1;
        }
    }
}
