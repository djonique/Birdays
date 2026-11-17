/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.djonique.birdays.models.Person;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Parses the published backup format with the real Android pull parser. Nothing is written to the
 * database, so the user's records are untouched.
 */
@RunWith(AndroidJUnit4.class)
public class BackupParsingInstrumentedTest {

    private static final String HEADER =
            "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><records>";

    @Test
    public void parseBackup_birthdayBefore1970_keepsDate() throws Exception {
        String xml = HEADER
                + "<person>"
                + "<name>Ирина Анатольевна</name>"
                + "<date>-281750400000</date>"
                + "<year_unknown>false</year_unknown>"
                + "<phone_number></phone_number>"
                + "<email></email>"
                + "</person></records>";

        List<Person> persons = BirdaysRepository.parseBackup(stream(xml));

        assertEquals(1, persons.size());
        assertEquals(-281750400000L, persons.get(0).getDate());
        assertEquals("Ирина Анатольевна", persons.get(0).getName());
    }

    @Test
    public void parseBackup_fullyPopulatedPerson_readsEveryField() throws Exception {
        String xml = HEADER
                + "<person>"
                + "<name>Alice</name>"
                + "<date>123456789</date>"
                + "<year_unknown>true</year_unknown>"
                + "<phone_number>+100</phone_number>"
                + "<email>alice@example.com</email>"
                + "</person></records>";

        List<Person> persons = BirdaysRepository.parseBackup(stream(xml));

        assertEquals(1, persons.size());
        Person person = persons.get(0);
        assertEquals("Alice", person.getName());
        assertEquals(123456789L, person.getDate());
        assertTrue(person.isYearUnknown());
        assertEquals("+100", person.getPhoneNumber());
        assertEquals("alice@example.com", person.getEmail());
    }

    @Test
    public void parseBackup_recordWithoutDate_isSkipped() throws Exception {
        String xml = HEADER
                + "<person><name>Без даты</name></person>"
                + "<person><name>С датой</name><date>1000</date></person>"
                + "</records>";

        List<Person> persons = BirdaysRepository.parseBackup(stream(xml));

        assertEquals(1, persons.size());
        assertEquals("С датой", persons.get(0).getName());
    }

    @Test
    public void parseBackup_blankName_isSkipped() throws Exception {
        String xml = HEADER
                + "<person><name></name><date>1000</date></person>"
                + "<person><name>   </name><date>1000</date></person>"
                + "<person><date>1000</date></person>"
                + "<person><name>Нормальное</name><date>1000</date></person>"
                + "</records>";

        List<Person> persons = BirdaysRepository.parseBackup(stream(xml));

        assertEquals(1, persons.size());
        assertEquals("Нормальное", persons.get(0).getName());
    }

    @Test
    public void parseBackup_emptyRecordsRoot_isEmpty() throws Exception {
        List<Person> persons = BirdaysRepository.parseBackup(stream(HEADER + "</records>"));

        assertTrue(persons.isEmpty());
    }

    @Test
    public void parseBackup_truncatedDocument_keepsCompleteRecords() throws Exception {
        String xml = HEADER
                + "<person><name>Готов</name><date>1000</date></person>"
                + "<person><name>Обрыв";
        try {
            List<Person> persons = BirdaysRepository.parseBackup(stream(xml));
            assertFalse("A truncated record must not be restored", persons.size() > 1);
        } catch (org.xmlpull.v1.XmlPullParserException | java.io.IOException expected) {
            // The caller reports the file as unreadable and keeps the existing data.
        }
    }

    private static ByteArrayInputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
