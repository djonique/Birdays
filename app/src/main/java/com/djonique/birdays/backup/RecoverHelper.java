/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.djonique.birdays.R;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RecoverHelper {

    private static final String PERSON = "person";
    private static final String NAME = "name";
    private static final String DATE = "date";
    private static final String YEAR_UNKNOWN = "year_unknown";
    private static final String PHONE_NUMBER = "phone_number";
    private static final String EMAIL = "email";

    private final Context context;

    public RecoverHelper(Context context) {
        this.context = context;
    }

    public void recoverRecords(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(context, R.string.recover_records_error, Toast.LENGTH_LONG).show();
                return;
            }
            XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
            pullParserFactory.setNamespaceAware(true);
            XmlPullParser parser = pullParserFactory.newPullParser();
            parser.setInput(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            parseXml(parser);
            Utils.notifyWidget(context);
            Toast.makeText(context, R.string.records_recovered, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, R.string.recover_records_error, Toast.LENGTH_LONG).show();
        }
    }

    private void parseXml(XmlPullParser parser) {
        AlarmHelper alarmHelper = new AlarmHelper(context);

        try (DbHelper dbHelper = new DbHelper(context)) {
            List<Person> dbPersons = dbHelper.query().getPersons();
            Person person = null;

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals(PERSON)) {
                            person = new Person();
                        } else if (person != null) {
                            switch (name) {
                                case NAME:
                                    person.setName(parser.nextText());
                                    break;
                                case DATE:
                                    person.setDate(Long.parseLong(parser.nextText()));
                                    break;
                                case YEAR_UNKNOWN:
                                    person.setYearUnknown(Boolean.parseBoolean(parser.nextText()));
                                    break;
                                case PHONE_NUMBER:
                                    person.setPhoneNumber(parser.nextText());
                                    break;
                                case EMAIL:
                                    person.setEmail(parser.nextText());
                                    break;
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equals(PERSON) && person != null) {
                            if (!Utils.isPersonAlreadyInDb(person, dbPersons)) {
                                dbHelper.addRecord(person);
                                alarmHelper.setAlarms(person);
                            }
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Toast.makeText(context, R.string.recover_records_error, Toast.LENGTH_LONG).show();
        }
    }
}
