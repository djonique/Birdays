/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.djonique.birdays.R;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private DbHelper dbHelper;
    private List<Person> widgetList;

    WidgetViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        dbHelper = new DbHelper(context);
        widgetList = new ArrayList<>();
    }

    @Override
    public void onDataSetChanged() {
        widgetList.clear();

        List<Person> persons = dbHelper.query().getPersons();
        Collections.sort(persons);

        Calendar today = Calendar.getInstance();
        int month = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DAY_OF_MONTH);

        int position = 0;

        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (person.getMonth() == month && person.getDay() >= day || person.getMonth() > month) {
                position = i;
                break;
            }
        }

        for (int i = position; i < persons.size(); i++) {
            widgetList.add(persons.get(i));
        }
        for (int i = 0; i < position; i++) {
            widgetList.add(persons.get(i));
        }
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    @Override
    public int getCount() {
        return widgetList.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews view = new RemoteViews(context.getPackageName(),
                R.layout.description_widget_list_view);

        if (i < 0 || i >= widgetList.size()) return view;

        String name = widgetList.get(i).getName();
        long date = widgetList.get(i).getDate();
        boolean yearUnknown = widgetList.get(i).isYearUnknown();

        if (!yearUnknown) {
            String displayedAge = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.DISPLAYED_AGE_KEY, "0");
            int age = (displayedAge.equals("0") ? Utils.getCurrentAge(date) : Utils.getFutureAge(date));
            view.setTextViewText(R.id.textview_widget_age, String.valueOf(age));
        } else {
            view.setTextViewText(R.id.textview_widget_age, "");
        }

        view.setTextViewText(R.id.textview_widget_name, name);

        String today = context.getString(R.string.today);
        if (Utils.daysLeft(context, date).equals(today)) {
            view.setTextViewText(R.id.textview_widget_date, today);
            setTextColor(view, ContextCompat.getColor(context, R.color.red_alert));
        } else {
            view.setTextViewText(R.id.textview_widget_date, Utils.getDateWithoutYear(date));
            setTextColor(view, ContextCompat.getColor(context, android.R.color.white));
        }

        Intent clickIntent = new Intent();
        clickIntent.putExtra(Constants.TIME_STAMP, widgetList.get(i).getTimeStamp());
        view.setOnClickFillInIntent(R.id.relativelayout_widget, clickIntent);

        return view;
    }

    private void setTextColor(RemoteViews views, int color) {
        views.setTextColor(R.id.textview_widget_age, color);
        views.setTextColor(R.id.textview_widget_name, color);
        views.setTextColor(R.id.textview_widget_date, color);
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
