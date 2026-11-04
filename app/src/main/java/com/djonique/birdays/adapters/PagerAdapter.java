/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.djonique.birdays.R;
import com.djonique.birdays.fragments.AllFragment;
import com.djonique.birdays.fragments.FamousFragment;
import com.djonique.birdays.fragments.MonthFragment;
import com.djonique.birdays.models.Person;

import java.util.Calendar;

public class PagerAdapter extends FragmentPagerAdapter {

    public static final int FAMOUS_FRAGMENT_POSITION = 2;

    private Context context;
    private MonthFragment monthFragment;
    private AllFragment allFragment;

    @SuppressWarnings("deprecation")
    public PagerAdapter(FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return switch (position) {
            case 0 -> new MonthFragment();
            case 1 -> new AllFragment();
            default -> new FamousFragment();
        };
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return switch (position) {
            case 0 -> getMonth();
            case 1 -> context.getString(R.string.all);
            case 2 -> context.getString(R.string.famous);
            default -> null;
        };
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        switch (position) {
            case 0:
                monthFragment = ((MonthFragment) createdFragment);
                break;
            case 1:
                allFragment = ((AllFragment) createdFragment);
                break;
        }
        return createdFragment;
    }

    private String getMonth() {
        String[] months = context.getResources().getStringArray(R.array.months);
        return months[Calendar.getInstance().get(Calendar.MONTH)];
    }

    public void search(String text) {
        if (allFragment != null) {
            allFragment.findPerson(text);
        }
        if (monthFragment != null) {
            monthFragment.findPerson(text);
        }
    }

    public void addPersonsFromDb() {
        if (monthFragment != null) {
            monthFragment.addMonthPersonsFromDb();
        }
        if (allFragment != null) {
            allFragment.addAllPersonsFromDb();
        }
    }

    public void addPerson(Person person) {
        if (monthFragment != null) {
            monthFragment.addPerson(person);
        }
        if (allFragment != null) {
            allFragment.addPerson(person, true);
        }
    }

    public void deletePerson(long timeStamp) {
        if (monthFragment != null) {
            monthFragment.deleteRecord(timeStamp);
        }
    }
}