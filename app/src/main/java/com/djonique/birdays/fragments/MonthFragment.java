/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.fragments;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.MainActivity;
import com.djonique.birdays.ad.NativeAdCache;
import com.djonique.birdays.adapters.MonthFragmentAdapter;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;

import java.util.List;

public class MonthFragment extends Fragment {

    private MainActivity activity;
    private RecyclerView recyclerView;
    private MonthFragmentAdapter adapter;
    private NativeAd mNativeAd;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            activity = (MainActivity) getActivity();
            addMonthPersonsFromDb();
            loadNativeAd();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new MonthFragmentAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        if (mNativeAd != null) {
            mNativeAd.setNativeAdEventListener(null);
        }
        super.onDestroy();
    }

    private void loadNativeAd() {
        if (getActivity() == null) return;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean adEnabled = preferences.getBoolean(getString(R.string.show_ads_key), true);
        if (!adEnabled) {
            if (adapter != null) {
                adapter.setNativeAd(null);
            }
            return;
        }

        NativeAdCache.getInstance().loadAd(requireContext(), new NativeAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd nativeAd) {
                if (isAdded() && adapter != null) {
                    mNativeAd = nativeAd;
                    adapter.setNativeAd(nativeAd);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                if (isAdded() && adapter != null) {
                    adapter.setNativeAd(null);
                }
            }
        });
    }

    public void addPerson(Person newPerson) {
        adapter.addPerson(newPerson);
    }

    public void addMonthPersonsFromDb() {
        if (adapter == null || activity == null) return;

        Parcelable listState = null;
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            listState = recyclerView.getLayoutManager().onSaveInstanceState();
        }

        adapter.removeAllPersons();
        List<Person> persons = activity.dbHelper.query().getThisMonthPersons();
        adapter.addPersons(persons);

        if (mNativeAd != null) {
            adapter.setNativeAd(mNativeAd);
        }

        if (listState != null && recyclerView != null && recyclerView.getLayoutManager() != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }
    }

    public void deleteRecord(long timeStamp) {
        if (adapter != null) adapter.removePerson(timeStamp);
    }

    public void findPerson(String name) {
        if (adapter == null || activity == null) return;
        adapter.removeAllPersons();
        List<Person> persons = activity.dbHelper.query().getSearchMonthPerson(
                DbHelper.SELECTION_LIKE_NAME, new String[]{"%" + name + "%"}, DbHelper.COLUMN_NAME);
        adapter.addPersons(persons);

        if (mNativeAd != null) {
            adapter.setNativeAd(mNativeAd);
        }
    }
}
