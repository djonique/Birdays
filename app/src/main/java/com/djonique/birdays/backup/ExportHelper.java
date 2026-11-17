/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import android.content.Context;
import android.util.Xml;

import androidx.annotation.NonNull;

import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ExportHelper {

    private static final String RECORDS = "records";
    private static final String PERSON = "person";
    private static final String NAME = "name";
    private static final String DATE = "date";
    private static final String YEAR_UNKNOWN = "year_unknown";
    private static final String PHONE_NUMBER = "phone_number";
    private static final String EMAIL = "email";
    private static final String UTF_8 = "UTF-8";
    /**
     * Distinct from the download file, so a backup and a restore cannot overwrite each other.
     */
    private static final String UPLOAD_FILE_NAME = "backup_upload.xml";

    public static Backup createBackupFile(Context context) throws IOException {
        File file = new File(context.getCacheDir(), UPLOAD_FILE_NAME);
        List<Person> persons;

        try (DbHelper dbHelper = new DbHelper(context)) {
            persons = dbHelper.query().getPersons();
        }

        String xml = writeXml(persons);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(xml.getBytes(StandardCharsets.UTF_8));
        }
        return new Backup(file, persons);
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

    /**
     * A written backup file together with the count of the records that were serialized into it.
     * The count is uploaded to Drive as {@code appProperties} next to the file, so the restore
     * dialog can show it without parsing the whole document.
     */
    public static final class Backup {

        private final File file;
        private final int recordCount;
        private final Map<String, String> appProperties;

        Backup(@NonNull File file, @NonNull List<Person> persons) {
            this.file = file;
            // Derived from the very same list that writeXml() serializes: they cannot drift apart.
            this.recordCount = persons.size();
            this.appProperties = BackupMetadata.createAppProperties(recordCount);
        }

        @NonNull
        public File getFile() {
            return file;
        }

        public int getRecordCount() {
            return recordCount;
        }

        /**
         * {@code appProperties} to store on the Drive file.
         */
        @NonNull
        public Map<String, String> getAppProperties() {
            return appProperties;
        }
    }
}
