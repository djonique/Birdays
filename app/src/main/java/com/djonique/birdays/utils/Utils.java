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
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private static final String STATUS_BAR_SCRIM_TAG = "STATUS_BAR_SCRIM";
    private static final String NAV_BAR_SCRIM_TAG = "NAV_BAR_SCRIM_";
    private static final String CUTOUT_SCRIM_LEFT_TAG = "CUTOUT_SCRIM_LEFT";
    private static final String CUTOUT_SCRIM_RIGHT_TAG = "CUTOUT_SCRIM_RIGHT";
    private static final Calendar today = Calendar.getInstance();
    private static final Calendar dayOfBirthday = Calendar.getInstance();

    public static String getDate(long date) {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(date);
    }

    public static String getDateWithoutYear(long date) {
        Locale locale = Locale.getDefault();
        SimpleDateFormat sdf = ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT, locale));
        String pattern = sdf.toPattern();
        String noYearPattern = pattern.replaceAll("([^\\p{Alpha}']|('\\p{Alpha}+'))*y+([^\\p{Alpha}']|('\\p{Alpha}+'))*", "");
        SimpleDateFormat noYearFormat = new SimpleDateFormat(noYearPattern, locale);
        return noYearFormat.format(date);
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

        resId = switch (getMonth(dayOfBirthday)) {
            case Calendar.JANUARY ->
                    getDay(dayOfBirthday) < 21 ? R.string.capricorn : R.string.aquarius;
            case Calendar.FEBRUARY ->
                    getDay(dayOfBirthday) < 20 ? R.string.aquarius : R.string.pisces;
            case Calendar.MARCH -> getDay(dayOfBirthday) < 21 ? R.string.pisces : R.string.aries;
            case Calendar.APRIL -> getDay(dayOfBirthday) < 21 ? R.string.aries : R.string.taurus;
            case Calendar.MAY -> getDay(dayOfBirthday) < 22 ? R.string.taurus : R.string.gemini;
            case Calendar.JUNE -> getDay(dayOfBirthday) < 22 ? R.string.gemini : R.string.cancer;
            case Calendar.JULY -> getDay(dayOfBirthday) < 23 ? R.string.cancer : R.string.leo;
            case Calendar.AUGUST -> getDay(dayOfBirthday) < 23 ? R.string.leo : R.string.virgo;
            case Calendar.SEPTEMBER -> getDay(dayOfBirthday) < 24 ? R.string.virgo : R.string.libra;
            case Calendar.OCTOBER -> getDay(dayOfBirthday) < 24 ? R.string.libra : R.string.scorpio;
            case Calendar.NOVEMBER ->
                    getDay(dayOfBirthday) < 23 ? R.string.scorpio : R.string.sagittarius;
            case Calendar.DECEMBER ->
                    getDay(dayOfBirthday) < 22 ? R.string.sagittarius : R.string.capricorn;
            default -> resId;
        };
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

        if (dateArray[0].isEmpty()) {
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
        return dateArray[0].isEmpty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isPersonAlreadyInDb(Person person, List<Person> list) {
        if (person.getName() == null) {
            return false;
        }
        for (Person dbPerson : list) {
            // Person.equals() is identity by timestamp; contact deduplication is by display
            // name, so the comparison is explicit here instead of relying on equals().
            if (person.getName().equalsIgnoreCase(dbPerson.getName())) {
                return true;
            }
        }
        return false;
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

    @SuppressWarnings("deprecation")
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
     * Dynamically configures the status and navigation bar background colors and icon themes
     * based on light/dark mode and activity type.
     */
    @SuppressWarnings("deprecation")
    public static void setupSystemBarAppearance(Activity activity) {
        boolean isDarkTheme = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        android.view.Window window = activity.getWindow();
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.BLACK);
        window.setNavigationBarColor(androidx.core.content.ContextCompat.getColor(activity, R.color.md_theme_background));

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());

        // The status bar is always black, so its icons have to stay light in both schemes.
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(!isDarkTheme);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            addNavigationBarScrims(activity);
            addStatusBarScrim(activity);
        }
        addCutoutScrims(activity);
    }

    /**
     * On Android 15+ the window is edge-to-edge and the system navigation bar is transparent, so
     * the content behind it is visible. The bar is painted with the app background colour instead:
     * nothing shines through the navigation bar and it looks opaque on every screen.
     */
    private static void addNavigationBarScrims(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        int color = androidx.core.content.ContextCompat.getColor(activity, R.color.md_theme_background);
        addNavigationBarScrim(activity, content, Gravity.BOTTOM, color);
        addNavigationBarScrim(activity, content, Gravity.START, color);
        addNavigationBarScrim(activity, content, Gravity.END, color);
    }

    private static void addNavigationBarScrim(Activity activity, ViewGroup content,
                                              int gravity, int color) {
        String tag = NAV_BAR_SCRIM_TAG + gravity;
        if (content.findViewWithTag(tag) != null) return;

        View scrim = new View(activity);
        scrim.setTag(tag);
        scrim.setBackgroundColor(color);
        scrim.setClickable(false);
        scrim.setFocusable(false);
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        FrameLayout.LayoutParams params = gravity == Gravity.BOTTOM
                ? new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, gravity)
                : new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, gravity);
        content.addView(scrim, params);

        ViewCompat.setOnApplyWindowInsetsListener(scrim, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int size = gravity == Gravity.BOTTOM ? bars.bottom
                    : gravity == Gravity.START ? bars.left : bars.right;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            int current = gravity == Gravity.BOTTOM ? layoutParams.height : layoutParams.width;
            if (current != size) {
                if (gravity == Gravity.BOTTOM) {
                    layoutParams.height = size;
                } else {
                    layoutParams.width = size;
                }
                view.setLayoutParams(layoutParams);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scrim);
    }

    /**
     * On Android 15+ the window is edge-to-edge and the system status bar is transparent, so its
     * colour has to be painted by the app. A fixed black view is laid over the top of the activity
     * instead of colouring the app bar: the app bar scrolls (in fragments and on the detail screen),
     * while this scrim stays in place and keeps the status bar black everywhere.
     */
    private static void addStatusBarScrim(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.findViewWithTag(STATUS_BAR_SCRIM_TAG) != null) return;

        View scrim = new View(activity);
        scrim.setTag(STATUS_BAR_SCRIM_TAG);
        scrim.setBackgroundColor(android.graphics.Color.BLACK);
        scrim.setClickable(false);
        scrim.setFocusable(false);
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP);
        content.addView(scrim, params);

        ViewCompat.setOnApplyWindowInsetsListener(scrim, (view, windowInsets) -> {
            int top = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()).top;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams.height != top) {
                layoutParams.height = top;
                view.setLayoutParams(layoutParams);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scrim);
    }

    /**
     * In landscape the camera cutout sits on the left (or right) edge and the app cannot draw there,
     * so the strip next to the content would otherwise stay the window background colour. A fixed
     * black strip of exactly the cutout width is painted over it, which keeps the cutout area black
     * in both themes and in every activity.
     */
    private static void addCutoutScrims(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        addCutoutScrim(activity, content, Gravity.START, CUTOUT_SCRIM_LEFT_TAG);
        addCutoutScrim(activity, content, Gravity.END, CUTOUT_SCRIM_RIGHT_TAG);
    }

    private static void addCutoutScrim(Activity activity, ViewGroup content, int gravity, String tag) {
        if (content.findViewWithTag(tag) != null) return;

        View scrim = new View(activity);
        scrim.setTag(tag);
        scrim.setBackgroundColor(android.graphics.Color.BLACK);
        scrim.setClickable(false);
        scrim.setFocusable(false);
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, gravity);
        content.addView(scrim, params);

        ViewCompat.setOnApplyWindowInsetsListener(scrim, (view, windowInsets) -> {
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            int width = gravity == Gravity.START ? cutout.left : cutout.right;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams.width != width) {
                layoutParams.width = width;
                view.setLayoutParams(layoutParams);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scrim);
    }

    public static void applySystemBarsInsets(View rootView, View topView, View bottomMarginView, View bottomPaddingView) {
        if (rootView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            // Combine system bars and the display cutout: in landscape the navigation bar and
            // the camera cutout sit on the side, so the content has to keep clear of both.
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            if (topView != null) {
                Object tag = topView.getTag(R.id.tag_initial_top_padding);
                int initialTopPadding;
                if (tag instanceof Integer) {
                    initialTopPadding = (Integer) tag;
                } else {
                    initialTopPadding = topView.getPaddingTop();
                    topView.setTag(R.id.tag_initial_top_padding, initialTopPadding);
                }
                topView.setPadding(
                        topView.getPaddingLeft(),
                        initialTopPadding + insets.top,
                        topView.getPaddingRight(),
                        topView.getPaddingBottom()
                );
            }
            applySideInsets(rootView, insets);
            if (bottomMarginView != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomMarginView.getLayoutParams();
                if (params != null) {
                    Object tag = bottomMarginView.getTag(R.id.tag_initial_bottom_margin);
                    int initialMargin;
                    if (tag instanceof Integer) {
                        initialMargin = (Integer) tag;
                    } else {
                        initialMargin = params.bottomMargin;
                        bottomMarginView.setTag(R.id.tag_initial_bottom_margin, initialMargin);
                    }
                    params.bottomMargin = initialMargin + insets.bottom;
                    bottomMarginView.setLayoutParams(params);
                }
            }
            if (bottomPaddingView != null) {
                Object tag = bottomPaddingView.getTag(R.id.tag_initial_bottom_padding);
                int initialBottomPadding;
                if (tag instanceof Integer) {
                    initialBottomPadding = (Integer) tag;
                } else {
                    initialBottomPadding = bottomPaddingView.getPaddingBottom();
                    bottomPaddingView.setTag(R.id.tag_initial_bottom_padding, initialBottomPadding);
                }
                bottomPaddingView.setPadding(
                        bottomPaddingView.getPaddingLeft(),
                        bottomPaddingView.getPaddingTop(),
                        bottomPaddingView.getPaddingRight(),
                        initialBottomPadding + insets.bottom
                );
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private static void applySideInsets(View view, Insets insets) {
        Object leftTag = view.getTag(R.id.tag_initial_left_padding);
        Object rightTag = view.getTag(R.id.tag_initial_right_padding);
        int initialLeftPadding;
        int initialRightPadding;
        if (leftTag instanceof Integer && rightTag instanceof Integer) {
            initialLeftPadding = (Integer) leftTag;
            initialRightPadding = (Integer) rightTag;
        } else {
            initialLeftPadding = view.getPaddingLeft();
            initialRightPadding = view.getPaddingRight();
            view.setTag(R.id.tag_initial_left_padding, initialLeftPadding);
            view.setTag(R.id.tag_initial_right_padding, initialRightPadding);
        }
        view.setPadding(
                initialLeftPadding + insets.left,
                view.getPaddingTop(),
                initialRightPadding + insets.right,
                view.getPaddingBottom()
        );
    }
}