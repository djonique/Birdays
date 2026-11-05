/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.djonique.birdays.R;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.widget.WidgetProvider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    private static final int MILLISECONDS_IN_A_DAY = 86400000;
    private static Calendar today = Calendar.getInstance();
    private static Calendar dayOfBirthday = Calendar.getInstance();

    public static String getDate(long date) {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(date);
    }

    public static String getDateWithoutYear(long date) {
        Locale locale = Locale.getDefault();
        SimpleDateFormat sdf = ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT, locale));
        String pattern = sdf.toPattern();
        String yearlessPattern = pattern.replaceAll("([^\\p{Alpha}']|('[\\p{Alpha}]+'))*y+([^\\p{Alpha}']|('[\\p{Alpha}]+'))*", "");
        SimpleDateFormat yearlessFormat = new SimpleDateFormat(yearlessPattern, locale);
        return yearlessFormat.format(date);
    }

    private static int getYear(Calendar calendar) {
        return calendar.get(Calendar.YEAR);
    }

    private static int getMonth(Calendar calendar) {
        return calendar.get(Calendar.MONTH);
    }

    private static int getDay(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getMonth(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        return calendar.get(Calendar.MONTH);
    }

    public static int getDay(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static long getTimeOffset() {
        return TimeZone.getDefault().getOffset(today.getTimeInMillis());
    }

    public static int getCurrentAge(long date) {
        dayOfBirthday.setTimeInMillis(date);
        int age = getYear(today) - getYear(dayOfBirthday);
        if (getMonth(today) < getMonth(dayOfBirthday)) {
            age--;
        } else if (getMonth(today) == getMonth(dayOfBirthday) &&
                getDay(today) < getDay(dayOfBirthday)) {
            age--;
        }
        return age;
    }

    public static int getFutureAge(long date) {
        dayOfBirthday.setTimeInMillis(date);
        int age = getYear(today) - getYear(dayOfBirthday);
        if (getMonth(today) < getMonth(dayOfBirthday)) {
            age--;
        } else if (getMonth(today) == getMonth(dayOfBirthday) &&
                getDay(today) <= getDay(dayOfBirthday)) {
            age--;
        }
        return age + 1;
    }

    public static String daysLeft(Context context, long date) {
        Calendar birthday = Calendar.getInstance();
        birthday.setTimeInMillis(date);
        if (getMonth(today) == getMonth(birthday) && getDay(today) == getDay(birthday)) {
            return context.getString(R.string.today);
        }
        birthday.set(Calendar.HOUR_OF_DAY, 10);
        today.set(Calendar.HOUR_OF_DAY, 9);
        if (getMonth(today) < getMonth(birthday)
                || (getMonth(today) == getMonth(birthday) && getDay(today) <= getDay(birthday))) {
            birthday.set(Calendar.YEAR, today.get(Calendar.YEAR));
        } else {
            birthday.set(Calendar.YEAR, today.get(Calendar.YEAR) + 1);
        }
        long diffDays = (birthday.getTimeInMillis() - today.getTimeInMillis()) / MILLISECONDS_IN_A_DAY;
        return String.valueOf(diffDays);
    }

    public static String daysSinceBirthday(long date) {
        Calendar birthday = Calendar.getInstance();
        birthday.setTimeInMillis(date);
        birthday.set(Calendar.HOUR_OF_DAY, 9);
        today.set(Calendar.HOUR_OF_DAY, 10);
        long diffDays = (today.getTimeInMillis() - birthday.getTimeInMillis()) / MILLISECONDS_IN_A_DAY;
        return String.valueOf(diffDays);
    }

    public static boolean isEmptyDate(EditText editText) {
        return editText == null || TextUtils.isEmpty(editText.getText().toString());
    }

    public static boolean isRightDate(Calendar calendar) {
        return Calendar.getInstance().getTimeInMillis() >= calendar.getTimeInMillis();
    }

    public static boolean isCurrentMonth(long date) {
        boolean thisMonth = false;

        dayOfBirthday.setTimeInMillis(date);

        if (getMonth(dayOfBirthday) == getMonth(today)) {
            thisMonth = true;
        }
        return thisMonth;
    }

    public static boolean isBirthdayPassed(long date) {
        return getDay(today) > getDay(date);
    }

    public static int getZodiacId(long date) {
        int resId = 0;
        dayOfBirthday.setTimeInMillis(date);

        switch (getMonth(dayOfBirthday)) {
            case Calendar.JANUARY:
                resId = getDay(dayOfBirthday) < 21 ? R.string.capricorn : R.string.aquarius;
                break;
            case Calendar.FEBRUARY:
                resId = getDay(dayOfBirthday) < 20 ? R.string.aquarius : R.string.pisces;
                break;
            case Calendar.MARCH:
                resId = getDay(dayOfBirthday) < 21 ? R.string.pisces : R.string.aries;
                break;
            case Calendar.APRIL:
                resId = getDay(dayOfBirthday) < 21 ? R.string.aries : R.string.taurus;
                break;
            case Calendar.MAY:
                resId = getDay(dayOfBirthday) < 22 ? R.string.taurus : R.string.gemini;
                break;
            case Calendar.JUNE:
                resId = getDay(dayOfBirthday) < 22 ? R.string.gemini : R.string.cancer;
                break;
            case Calendar.JULY:
                resId = getDay(dayOfBirthday) < 23 ? R.string.cancer : R.string.leo;
                break;
            case Calendar.AUGUST:
                resId = getDay(dayOfBirthday) < 23 ? R.string.leo : R.string.virgo;
                break;
            case Calendar.SEPTEMBER:
                resId = getDay(dayOfBirthday) < 24 ? R.string.virgo : R.string.libra;
                break;
            case Calendar.OCTOBER:
                resId = getDay(dayOfBirthday) < 24 ? R.string.libra : R.string.scorpio;
                break;
            case Calendar.NOVEMBER:
                resId = getDay(dayOfBirthday) < 23 ? R.string.scorpio : R.string.sagittarius;
                break;
            case Calendar.DECEMBER:
                resId = getDay(dayOfBirthday) < 22 ? R.string.sagittarius : R.string.capricorn;
                break;
        }
        return resId;
    }

    public static int getZodiacImage(int resId) {
        if (resId == R.string.aries) {
            return R.drawable.ic_aries;
        } else if (resId == R.string.taurus) {
            return R.drawable.ic_taurus;
        } else if (resId == R.string.gemini) {
            return R.drawable.ic_gemini;
        } else if (resId == R.string.cancer) {
            return R.drawable.ic_cancer;
        } else if (resId == R.string.leo) {
            return R.drawable.ic_leo;
        } else if (resId == R.string.virgo) {
            return R.drawable.ic_virgo;
        } else if (resId == R.string.libra) {
            return R.drawable.ic_libra;
        } else if (resId == R.string.scorpio) {
            return R.drawable.ic_scorpio;
        } else if (resId == R.string.sagittarius) {
            return R.drawable.ic_sagittarius;
        } else if (resId == R.string.capricorn) {
            return R.drawable.ic_capricorn;
        } else if (resId == R.string.aquarius) {
            return R.drawable.ic_aquarius;
        } else if (resId == R.string.pisces) {
            return R.drawable.ic_pisces;
        }
        return 0;
    }

    public static int boolToInt(boolean isYearKnown) {
        return isYearKnown ? 1 : 0;
    }

    static long formatDateToLong(String dateString) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        String[] dateArray = dateString.split("-");

        if (dateArray[0].equals("")) {
            calendar.set(Calendar.MONTH, Integer.parseInt(dateArray[2]) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateArray[3]));
        } else if (Integer.parseInt(dateArray[2]) < 32) {
            calendar.set(Calendar.YEAR, Integer.parseInt(dateArray[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(dateArray[1]) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateArray[2]));
        }
        return calendar.getTimeInMillis();
    }

    static boolean isYearUnknown(String dateString) {
        String[] dateArray = dateString.split("-");
        return dateArray[0].equals("");
    }

    public static boolean isPersonAlreadyInDb(Person person, List<Person> list) {
        boolean found = false;
        for (Person dbPerson : list) {
            if (person.equals(dbPerson)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static void setupDayNightTheme(SharedPreferences preferences) {
        boolean nightMode = preferences.getBoolean(Constants.NIGHT_MODE_KEY, false);
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static void openBrowser(Context context, String link) {
        try {
            context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(link)), null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.web_search_error, Toast.LENGTH_SHORT).show();
        }
    }

    public static void notifyWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listview_widget);
    }

    /**
     * Applies the custom transition for an activity that is opening another one.
     */
    @SuppressWarnings("deprecation")
    public static void overrideOpenTransition(Activity activity, int enterAnim, int exitAnim) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim);
        } else {
            activity.overridePendingTransition(enterAnim, exitAnim);
        }
    }

    /**
     * Applies the custom transition for an activity that is closing.
     */
    @SuppressWarnings("deprecation")
    public static void overrideCloseTransition(Activity activity, int enterAnim, int exitAnim) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim);
        } else {
            activity.overridePendingTransition(enterAnim, exitAnim);
        }
    }

    /**
     * Keeps the status and navigation bar icons readable on Android 15+, where the app is drawn
     * edge-to-edge and the bars are transparent. AppCompat already insets the content, so only the
     * icon colour has to follow the current day/night theme.
     */
    public static void setupSystemBarAppearance(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return;
        boolean nightMode = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!nightMode);
        controller.setAppearanceLightNavigationBars(!nightMode);
    }
}