/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.djonique.birdays.activities.DetailActivity;
import com.djonique.birdays.utils.BirdaysApplication;
import com.djonique.birdays.utils.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for the notification tap: the content intent must always target
 * DetailActivity, even while the list screen is visible. The old code reused the broadcast
 * intent in that case, so tapping the notification did nothing.
 */
@RunWith(AndroidJUnit4.class)
public class AlarmReceiverResultIntentTest {

    private static final long TIME_STAMP = 123456789L;

    private Context context;
    private boolean wasActivityVisible;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        wasActivityVisible = BirdaysApplication.isActivityVisible();
        BirdaysApplication.activityResumed();
    }

    @After
    public void tearDown() {
        if (!wasActivityVisible) {
            BirdaysApplication.activityPaused();
        }
    }

    @Test
    public void getResultIntent_withVisibleActivity_targetsDetailActivity() {
        Intent result = new AlarmReceiver().getResultIntent(context, TIME_STAMP);

        assertNotNull(result.getComponent());
        assertEquals(DetailActivity.class.getName(), result.getComponent().getClassName());
        assertEquals(TIME_STAMP, result.getLongExtra(Constants.TIME_STAMP, -1L));
        assertTrue("CLEAR_TOP is required by the SINGLE_TOP detail screen",
                (result.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertTrue("SINGLE_TOP is required by the onNewIntent() reload",
                (result.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
    }

    @Test
    public void getResultIntent_isAlwaysAFreshIntent() {
        Intent first = new AlarmReceiver().getResultIntent(context, TIME_STAMP);
        Intent second = new AlarmReceiver().getResultIntent(context, TIME_STAMP);

        assertNotSame(first, second);
    }
}
