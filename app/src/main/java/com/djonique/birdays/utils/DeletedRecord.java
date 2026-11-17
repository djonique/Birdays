/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.djonique.birdays.models.Person;

/**
 * Process-scoped hand-off of a record deleted on the detail screen to the list screen.
 *
 * <p>The deletion is committed before the detail screen closes, so the list only needs the
 * deleted record to offer "Undo" (which re-inserts it). The hand-off expires quickly: when the
 * detail screen was opened from a notification or a widget there is no list screen to show the
 * snackbar on, and a stale record must not pop up an undo message later.</p>
 */
public final class DeletedRecord {

    private static final long MAX_AGE_MS = 5_000;

    private static Person record;
    private static long storedAt;

    private DeletedRecord() {
    }

    public static synchronized void set(@Nullable Person deleted) {
        record = deleted;
        storedAt = SystemClock.elapsedRealtime();
    }

    /**
     * @return the deleted record if the list screen came back right after the deletion,
     * otherwise {@code null} (nothing to undo).
     */
    @Nullable
    public static synchronized Person take() {
        Person result = record;
        record = null;
        if (result == null) {
            return null;
        }
        return SystemClock.elapsedRealtime() - storedAt <= MAX_AGE_MS ? result : null;
    }
}
