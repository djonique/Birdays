/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.TaskStackBuilder;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.DetailActivity;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;

import java.util.Calendar;

public class WidgetProvider extends AppWidgetProvider {

    private static final String ACTION_ON_CLICK = "ACTION_ON_CLICK";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int i : appWidgetIds) {

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            Intent serviceIntent = new Intent(context, WidgetService.class);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setRemoteAdapter(R.id.listview_widget, serviceIntent);

            remoteViews.setTextViewText(R.id.textview_widget_header_date,
                    Utils.getDateWithoutYear(Calendar.getInstance().getTimeInMillis()));

            Intent clickIntent = new Intent(context, WidgetProvider.class);
            clickIntent.setAction(ACTION_ON_CLICK);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0,
                    clickIntent, flags);
            remoteViews.setPendingIntentTemplate(R.id.listview_widget, clickPendingIntent);

            appWidgetManager.updateAppWidget(i, remoteViews);
            appWidgetManager.notifyAppWidgetViewDataChanged(i, R.id.listview_widget);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction() != null && intent.getAction().equals(ACTION_ON_CLICK)) {
            long timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);
            Intent resultIntent = new Intent(context, DetailActivity.class);
            resultIntent.putExtra(Constants.TIME_STAMP, timeStamp);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder.create(context).addNextIntentWithParentStack(resultIntent).startActivities();
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}