/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.djonique.birdays.models.Person;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

/**
 * Checks the real Android XML serializer used for backups. This cannot run on the JVM because
 * {@code android.util.Xml} is a framework class.
 */
@RunWith(AndroidJUnit4.class)
public class ExportHelperInstrumentedTest {

    @Test
    public void writeXml_fullyPopulatedPerson_containsAllFields() throws IOException {
        Person person = new Person("Alice", 123456789L, true, "+100", "alice@example.com", 42L);

        String xml = ExportHelper.writeXml(Collections.singletonList(person));

        assertTrue(xml.contains("<name>Alice</name>"));
        assertTrue(xml.contains("<date>123456789</date>"));
        assertTrue(xml.contains("<year_unknown>true</year_unknown>"));
        assertTrue(xml.contains("<phone_number>+100</phone_number>"));
        assertTrue(xml.contains("<email>alice@example.com</email>"));
    }

    @Test
    public void writeXml_emptyList_writesRecordsRootWithoutPersons() throws IOException {
        String xml = ExportHelper.writeXml(Collections.emptyList());

        assertTrue(xml.contains("<records"));
        assertFalse(xml.contains("<person"));
    }

    @Test
    public void writeXml_nullOptionalFields_doesNotSerializeNullText() throws IOException {
        Person person = new Person("Bob", 1L);

        String xml = ExportHelper.writeXml(Collections.singletonList(person));

        assertTrue(xml.contains("<name>Bob</name>"));
        assertTrue(xml.contains("<phone_number"));
        assertTrue(xml.contains("<email"));
        assertFalse(xml.contains("null"));
    }
}
