/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class FamousFragmentAdapter extends RecyclerView.Adapter<FamousFragmentAdapter.ListViewHolder> {

    private final List<Person> famous;
    private Context context;

    public FamousFragmentAdapter() {
        famous = new ArrayList<>();
    }

    public void addPerson(Person person) {
        famous.add(person);
        notifyItemInserted(getItemCount() - 1);
    }

    public void removeAllPersons() {
        int oldSize = getItemCount();
        if (oldSize > 0) {
            famous.clear();
            notifyItemRangeRemoved(0, oldSize);
        }
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.list_view_famous_fragment,
                parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ListViewHolder holder, int position) {
        if (position < 0 || position >= famous.size()) return;

        holder.itemView.setEnabled(true);
        View itemView = holder.itemView;
        final Person person = famous.get(position);
        final String name = person.getName();

        holder.tvName.setText(name);
        holder.tvDate.setText(Utils.getDate(person.getDate()));

        itemView.setOnClickListener(view -> Utils.openBrowser(context, context.getString(R.string.google_search) + name));
    }

    @Override
    public int getItemCount() {
        return famous.size();
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDate;

        ListViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textview_famous_name);
            tvDate = itemView.findViewById(R.id.textview_famous_date);
        }
    }
}
