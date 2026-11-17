/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.MainActivity;
import com.djonique.birdays.ad.NativeAdCache;
import com.djonique.birdays.adapters.MonthFragmentAdapter;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Utils;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;

import java.util.List;

public class MonthFragment extends Fragment {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private MainActivity activity;
    private RecyclerView recyclerView;
    private MonthFragmentAdapter adapter;
    private NativeAd mNativeAd;
    /**
     * UTC day number of the last refresh; ages/days-left only change when the date does.
     */
    private long lastRefreshDay = Long.MIN_VALUE;
    /**
     * Guards against an older list load or search landing after a newer one. Both paths rewrite
     * the same adapter, so they share a single sequence number.
     */
    private int dataToken;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            activity = (MainActivity) getActivity();
            addMonthPersonsFromDb();
            if (mNativeAd != null && areAdsEnabled()) {
                // The view was recreated while the fragment survived; reuse the already loaded ad
                // instead of requesting a second one.
                adapter.setNativeAd(mNativeAd);
            } else {
                loadNativeAd();
            }
            // The rows are freshly loaded, so the first onResume() does not need to rebind them.
            lastRefreshDay = System.currentTimeMillis() / DAY_MILLIS;
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

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null) return;
        // Ads are switched off on the settings screen, which does not recreate this fragment, so
        // the preference is re-read every time the screen comes back. Without this the ad row
        // would stay bound until the process is restarted.
        syncNativeAdWithPreference();
        // Ages and the days-left counter depend on the current date, so the rows are rebound —
        // but only when the day actually changed, not on every return to this screen.
        long today = System.currentTimeMillis() / DAY_MILLIS;
        if (today != lastRefreshDay) {
            lastRefreshDay = today;
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        // The RecyclerView (and its adapter) must not outlive the view hierarchy; the ad
        // reference itself is released in onDestroy().
        if (adapter != null) adapter.clearNativeAd();
        recyclerView = null;
        adapter = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        releaseNativeAd();
        super.onDestroy();
    }

    /**
     * NativeAd in Yandex Mobile Ads 8.6.0 has no destroy(); detaching the listener and dropping
     * the reference is the documented release.
     */
    private void releaseNativeAd() {
        if (mNativeAd != null) {
            mNativeAd.setNativeAdEventListener(null);
            mNativeAd = null;
        }
    }

    private void loadNativeAd() {
        if (getActivity() == null) return;
        if (!areAdsEnabled()) {
            clearNativeAd();
            return;
        }

        NativeAdCache.getInstance().loadAd(requireContext(), new NativeAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd nativeAd) {
                // The preference can change while the request is in flight, so the result is
                // dropped instead of binding an ad the user has already turned off.
                if (!isAdded() || adapter == null || !areAdsEnabled()) return;
                releaseNativeAd();
                mNativeAd = nativeAd;
                adapter.setNativeAd(nativeAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                if (isAdded() && adapter != null) {
                    adapter.setNativeAd(null);
                }
            }
        });
    }

    /**
     * Aligns the ad row with {@code show_ads_key}: an ad that is already bound is dropped, and a
     * missing one is requested again (for example after ads were re-enabled in the settings).
     */
    private void syncNativeAdWithPreference() {
        if (!areAdsEnabled()) {
            clearNativeAd();
        } else if (mNativeAd == null) {
            loadNativeAd();
        }
    }

    private boolean areAdsEnabled() {
        if (getActivity() == null) return false;
        SharedPreferences preferences =
                BirdaysPreferencesManager.getDefaultSharedPreferences(getActivity());
        return preferences.getBoolean(getString(R.string.show_ads_key), true);
    }

    private void clearNativeAd() {
        if (mNativeAd != null) {
            mNativeAd.setNativeAdEventListener(null);
            mNativeAd = null;
        }
        if (adapter != null) {
            adapter.setNativeAd(null);
        }
    }

    public void addPerson(Person newPerson) {
        if (adapter != null) adapter.addPerson(newPerson);
    }

    public void addMonthPersonsFromDb() {
        if (adapter == null || activity == null) return;

        final int token = ++dataToken;

        Parcelable listState = null;
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            listState = recyclerView.getLayoutManager().onSaveInstanceState();
        }

        final Parcelable savedState = listState;

        BirdaysRepository.getInstance(activity).getThisMonthPersons(
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(List<Person> persons) {
                        if (token != dataToken || !isAdded() || adapter == null) return;

                        // Same as in AllFragment: the rows arrive after the first layout pass, so
                        // the fall-down animation is re-armed for the first batch only.
                        boolean firstPopulation = adapter.getItemCount() == 0 && recyclerView != null;

                        adapter.replacePersons(persons);

                        if (firstPopulation) {
                            Utils.playListEnterAnimation(recyclerView);
                        }

                        if (savedState != null && recyclerView != null
                                && recyclerView.getLayoutManager() != null) {
                            recyclerView.getLayoutManager().onRestoreInstanceState(savedState);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Keep the previous list; the next refresh will retry.
                    }
                });
    }

    public void deleteRecord(long timeStamp) {
        if (adapter != null) adapter.removePerson(timeStamp);
    }

    public void findPerson(String name) {
        if (adapter == null || activity == null) return;

        final int token = ++dataToken;

        BirdaysRepository.getInstance(activity).searchPersons(name, true,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(List<Person> persons) {
                        if (token != dataToken || !isAdded() || adapter == null) return;
                        adapter.replacePersons(persons);
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Keep the previous list; the next keystroke retries.
                    }
                });
    }
}
