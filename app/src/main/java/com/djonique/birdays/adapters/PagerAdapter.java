/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.djonique.birdays.R;
import com.djonique.birdays.fragments.AllFragment;
import com.djonique.birdays.fragments.FamousFragment;
import com.djonique.birdays.fragments.MonthFragment;
import com.djonique.birdays.models.Person;

import java.util.Calendar;

public class PagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_COUNT = 3;
    public static final int FAMOUS_FRAGMENT_POSITION = 2;

    private final Context context;
    private final FragmentManager fragmentManager;

    public PagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
        this.context = activity;
        this.fragmentManager = activity.getSupportFragmentManager();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> new MonthFragment();
            case 1 -> new AllFragment();
            default -> new FamousFragment();
        };
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    public CharSequence getPageTitle(int position) {
        return switch (position) {
            case 0 -> getMonth();
            case 1 -> context.getString(R.string.all);
            case 2 -> context.getString(R.string.famous);
            default -> null;
        };
    }

    private String getMonth() {
        String[] months = context.getResources().getStringArray(R.array.months);
        return months[Calendar.getInstance().get(Calendar.MONTH)];
    }

    public void search(String text) {
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof AllFragment allFragment) {
                allFragment.findPerson(text);
            } else if (fragment instanceof MonthFragment monthFragment) {
                monthFragment.findPerson(text);
            }
        }
    }

    public void addPersonsFromDb() {
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof MonthFragment monthFragment) {
                monthFragment.addMonthPersonsFromDb();
            } else if (fragment instanceof AllFragment allFragment) {
                allFragment.addAllPersonsFromDb();
            }
        }
    }

    public void addPerson(Person person) {
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof MonthFragment monthFragment) {
                monthFragment.addPerson(person);
            } else if (fragment instanceof AllFragment allFragment) {
                allFragment.addPerson(person, true);
            }
        }
    }

    public void deletePerson(long timeStamp) {
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof MonthFragment monthFragment) {
                monthFragment.deleteRecord(timeStamp);
            } else if (fragment instanceof AllFragment allFragment) {
                allFragment.deletePerson(timeStamp);
            }
        }
    }

    /**
     * Puts a record back after an undo. The repository has already re-inserted it, so both lists
     * only need the item: the adapters animate the insertion like any other add.
     */
    public void restorePerson(Person person) {
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof MonthFragment monthFragment) {
                monthFragment.addPerson(person);
            } else if (fragment instanceof AllFragment allFragment) {
                allFragment.addPerson(person, false);
            }
        }
    }
}
