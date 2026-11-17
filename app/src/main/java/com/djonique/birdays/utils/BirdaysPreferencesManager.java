/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class BirdaysPreferencesManager {

    private BirdaysPreferencesManager() {
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
}
