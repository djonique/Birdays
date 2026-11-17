/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.djonique.birdays.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Launches a real activity and checks the initial form state with Espresso.
 *
 * <p>{@link NewPersonActivity} is used instead of the launcher {@link MainActivity}: launching
 * MainActivity would request permissions and may import the user's contacts, which mutates real
 * data. This screen has no side effects until OK is pressed and that item stays hidden while the
 * form is incomplete.</p>
 */
@RunWith(AndroidJUnit4.class)
public class NewPersonActivityTest {

    @Test
    public void launch_emptyForm_showsEmptyNameAndDateFields() {
        try (ActivityScenario<NewPersonActivity> ignored =
                     ActivityScenario.launch(NewPersonActivity.class)) {
            onView(withId(R.id.edittext_new_person_name)).check(matches(withText("")));
            onView(withId(R.id.edittext_new_person_date)).check(matches(withText("")));
        }
    }

    @Test
    public void launch_emptyForm_hidesOkAction() {
        try (ActivityScenario<NewPersonActivity> scenario =
                     ActivityScenario.launch(NewPersonActivity.class)) {
            scenario.onActivity(activity -> assertFalse(okActionVisible(activity)));
        }
    }

    @Test
    public void typeName_withoutDate_displaysNameAndKeepsOkActionHidden() {
        try (ActivityScenario<NewPersonActivity> scenario =
                     ActivityScenario.launch(NewPersonActivity.class)) {
            onView(withId(R.id.edittext_new_person_name))
                    .perform(typeText("Alice"), closeSoftKeyboard());
            onView(withId(R.id.edittext_new_person_name)).check(matches(withText("Alice")));
            scenario.onActivity(activity -> assertFalse(okActionVisible(activity)));
        }
    }

    /**
     * The OK action lives in the options menu (the floating button was replaced by a menu item),
     * so it is not part of the view hierarchy that Espresso can query directly. A throw-away menu
     * is inflated through the activity's own {@code onCreateOptionsMenu} to read the same state
     * the toolbar renders.
     */
    private static boolean okActionVisible(NewPersonActivity activity) {
        android.view.Menu menu = new android.widget.PopupMenu(activity,
                new android.view.View(activity)).getMenu();
        activity.onCreateOptionsMenu(menu);
        android.view.MenuItem item = menu.findItem(R.id.menu_edit_ok);
        return item != null && item.isVisible();
    }

    @Test
    public void launch_toolbarTitle_equalsNewRecord() {
        try (ActivityScenario<NewPersonActivity> scenario =
                     ActivityScenario.launch(NewPersonActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.getSupportActionBar());
                assertEquals(activity.getString(R.string.new_record),
                        activity.getSupportActionBar().getTitle().toString());
            });
        }
    }
}
