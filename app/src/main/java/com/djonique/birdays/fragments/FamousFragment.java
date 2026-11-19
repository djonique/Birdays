/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.adapters.FamousFragmentAdapter;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;

import java.util.Calendar;
import java.util.List;

public class FamousFragment extends Fragment {

    private FamousFragmentAdapter adapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            addFamousPersonsFromDb();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_view_layout, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new FamousFragmentAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
    }

    private void addFamousPersonsFromDb() {
        if (adapter == null || getActivity() == null) return;
        adapter.removeAllPersons();

        BirdaysRepository.getInstance(requireContext()).getFamousBornThisDay(
                Calendar.getInstance().getTimeInMillis(),
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(List<Person> famous) {
                        if (!isAdded() || adapter == null) return;
                        for (int i = 0; i < famous.size(); i++) {
                            adapter.addPerson(famous.get(i));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // The famous list is decorative; an empty list is a valid state.
                    }
                });
    }
}
