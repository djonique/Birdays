/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import static org.junit.Assert.assertEquals;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.djonique.birdays.models.Person;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

/**
 * Adapter-position tests for the remove paths. The native ad row is a real Yandex object that
 * cannot be constructed outside the SDK, so the test subclass only overrides the "is there an ad
 * row" seam; all position math is the production code.
 */
@RunWith(AndroidJUnit4.class)
public class MonthFragmentAdapterTest {

    private static final class AdapterWithAdRow extends MonthFragmentAdapter {
        @Override
        boolean isAdRowPresent() {
            return true;
        }
    }

    private static Person personInCurrentMonth(String name) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Person(name, calendar.getTimeInMillis(), false, "", "");
    }

    @Test
    public void removePerson_afterAdRow_notifiesShiftedAdapterPosition() {
        AdapterWithAdRow adapter = new AdapterWithAdRow();
        adapter.addPerson(personInCurrentMonth("Alice"));
        adapter.addPerson(personInCurrentMonth("Bob"));
        Person third = personInCurrentMonth("Carol");
        adapter.addPerson(third);

        RecordingObserver observer = new RecordingObserver();
        adapter.registerAdapterDataObserver(observer);

        // getAdPosition() == min(2, 3) == 2, so the third person lives at adapter position 3.
        adapter.removePerson(third.getTimeStamp());

        assertEquals(3, observer.lastPositionStart);
        assertEquals(1, observer.lastItemCount);
    }

    @Test
    public void removePerson_beforeAdRow_notifiesSameAdapterPosition() {
        AdapterWithAdRow adapter = new AdapterWithAdRow();
        adapter.addPerson(personInCurrentMonth("Alice"));
        Person second = personInCurrentMonth("Bob");
        adapter.addPerson(second);
        adapter.addPerson(personInCurrentMonth("Carol"));

        RecordingObserver observer = new RecordingObserver();
        adapter.registerAdapterDataObserver(observer);

        // Person index 1 is still adapter position 1 because the ad sits at 2.
        adapter.removePerson(second.getTimeStamp());

        assertEquals(1, observer.lastPositionStart);
        assertEquals(1, observer.lastItemCount);
    }

    @Test
    public void removeAllPersons_withAdRow_refreshesWholeList() {
        AdapterWithAdRow adapter = new AdapterWithAdRow();
        adapter.addPerson(personInCurrentMonth("Alice"));
        adapter.addPerson(personInCurrentMonth("Bob"));

        RecordingObserver observer = new RecordingObserver();
        adapter.registerAdapterDataObserver(observer);

        adapter.removeAllPersons();

        assertEquals("the whole list must be refreshed once", 1, observer.changedCount);
        // Only the ad row is left; no person index leaked into a range notification.
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void getItemCount_withAdRow_includesAdRow() {
        AdapterWithAdRow adapter = new AdapterWithAdRow();
        adapter.addPerson(personInCurrentMonth("Alice"));
        adapter.addPerson(personInCurrentMonth("Bob"));

        assertEquals(3, adapter.getItemCount());
    }

    private static final class RecordingObserver extends RecyclerView.AdapterDataObserver {
        private int lastPositionStart = -1;
        private int lastItemCount = -1;
        private int changedCount;

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            lastPositionStart = positionStart;
            lastItemCount = itemCount;
        }

        @Override
        public void onChanged() {
            changedCount++;
        }
    }
}
