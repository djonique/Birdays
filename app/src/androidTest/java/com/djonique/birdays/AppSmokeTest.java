/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.djonique.birdays.utils.BirdaysApplication;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke tests that prove the test process is really running inside the Birdays app on the
 * connected device, not in a bare JVM.
 */
@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void targetContext_packageName_equalsApplicationId() {
        assertEquals("com.djonique.birdays", targetContext().getPackageName());
    }

    @Test
    public void targetContext_application_isBirdaysApplication() {
        assertTrue(targetContext().getApplicationContext() instanceof BirdaysApplication);
    }

    @Test
    public void targetContext_appNameResource_equalsBirdays() {
        assertEquals("Birdays", targetContext().getString(R.string.app_name));
    }
}
