/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.models;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

public class Person implements Item, Comparable<Person> {

    private String name, phoneNumber, email;
    private long date, timeStamp;
    private boolean yearUnknown;

    /**
     * Calendar instances are mutable and not thread-safe. A single shared instance used to be
     * reused by every caller; a per-thread instance keeps the allocation cost low without
     * sharing state between the main thread and background work.
     */
    private static final ThreadLocal<Calendar> CALENDAR = new ThreadLocal<Calendar>() {
        @Override
        protected Calendar initialValue() {
            return Calendar.getInstance();
        }
    };

    public Person() {
        this.timeStamp = new Date().getTime();
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
        this.timeStamp = new Date().getTime();
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
        if (!(obj instanceof Person)) {
            return false;
        }
        Person person = (Person) obj;
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