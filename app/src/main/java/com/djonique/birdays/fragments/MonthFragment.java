/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.fragments;

import android.content.Intent;
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
import com.djonique.birdays.activities.MainActivity;
import com.djonique.birdays.adapters.MonthFragmentAdapter;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;

import java.util.ArrayList;
import java.util.List;

public class MonthFragment extends Fragment {

    private MainActivity activity;
    private MonthFragmentAdapter adapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            activity = (MainActivity) getActivity();
            addMonthPersonsFromDb();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new MonthFragmentAdapter();
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    public void addPerson(Person newPerson) {
        int position = -1;

        for (int i = 0; i < adapter.getItemCount(); i++) {
            Person person = adapter.getPerson(i);
            if (newPerson.getDay() < person.getDay()) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            adapter.addPerson(position, newPerson);
        } else {
            adapter.addPerson(newPerson);
        }
    }

    public void addMonthPersonsFromDb() {
        if (adapter == null || activity == null) return;
        adapter.removeAllPersons();
        List<Person> persons = new ArrayList<>(activity.dbHelper.query().getThisMonthPersons());

        for (int i = 0; i < persons.size(); i++) {
            addPerson(persons.get(i));
        }
    }

    public void deleteRecord(long timeStamp) {
        if (adapter != null) adapter.removePerson(timeStamp);
    }

    public void findPerson(String name) {
        if (adapter == null || activity == null) return;
        adapter.removeAllPersons();
        List<Person> persons = new ArrayList<>(activity.dbHelper.query().getSearchMonthPerson(DbHelper.SELECTION_LIKE_NAME,
                new String[]{"%" + name + "%"}, DbHelper.COLUMN_NAME));

        for (int i = 0; i < persons.size(); i++) {
            addPerson(persons.get(i));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}