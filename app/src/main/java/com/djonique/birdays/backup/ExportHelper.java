/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import android.content.Context;
import android.util.Xml;

import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportHelper {

    private static final String RECORDS = "records";
    private static final String PERSON = "person";
    private static final String NAME = "name";
    private static final String DATE = "date";
    private static final String YEAR_UNKNOWN = "year_unknown";
    private static final String PHONE_NUMBER = "phone_number";
    private static final String EMAIL = "email";
    private static final String UTF_8 = "UTF-8";

    public static File createBackupFile(Context context) throws IOException {
        File file = new File(context.getCacheDir(), "database_backup.xml");
        List<Person> persons;

        try (DbHelper dbHelper = new DbHelper(context)) {
            persons = dbHelper.query().getPersons();
        }

        String xml = writeXml(persons);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(xml.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    public static String writeXml(List<Person> persons) throws IOException {
        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter stringWriter = new StringWriter();
        xmlSerializer.setOutput(stringWriter);
        xmlSerializer.startDocument(UTF_8, true);
        xmlSerializer.startTag(null, RECORDS);
        for (Person person : persons) {
            xmlSerializer.startTag(null, PERSON);
            xmlSerializer.startTag(null, NAME);
            xmlSerializer.text(person.getName() != null ? person.getName() : "");
            xmlSerializer.endTag(null, NAME);
            xmlSerializer.startTag(null, DATE);
            xmlSerializer.text(String.valueOf(person.getDate()));
            xmlSerializer.endTag(null, DATE);
            xmlSerializer.startTag(null, YEAR_UNKNOWN);
            xmlSerializer.text(String.valueOf(person.isYearUnknown()));
            xmlSerializer.endTag(null, YEAR_UNKNOWN);
            xmlSerializer.startTag(null, PHONE_NUMBER);
            String phoneNumber = person.getPhoneNumber() == null ? "" : person.getPhoneNumber();
            xmlSerializer.text(phoneNumber);
            xmlSerializer.endTag(null, PHONE_NUMBER);
            xmlSerializer.startTag(null, EMAIL);
            String email = person.getEmail() == null ? "" : person.getEmail();
            xmlSerializer.text(email);
            xmlSerializer.endTag(null, EMAIL);
            xmlSerializer.endTag(null, PERSON);
        }
        xmlSerializer.endTag(null, RECORDS);
        xmlSerializer.endDocument();
        xmlSerializer.flush();
        return stringWriter.toString();
    }
}
