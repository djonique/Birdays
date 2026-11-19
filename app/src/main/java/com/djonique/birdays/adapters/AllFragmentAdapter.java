/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.DetailActivity;
import com.djonique.birdays.activities.MainActivity;
import com.djonique.birdays.fragments.AllFragment;
import com.djonique.birdays.models.Item;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.models.Separator;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AllFragmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PERSON = 0;
    private static final int TYPE_SEPARATOR = 1;
    private final List<Item> items;
    private final AllFragment allFragment;
    public boolean containsSeparatorJanuary;
    public boolean containsSeparatorFebruary;
    public boolean containsSeparatorMarch;
    public boolean containsSeparatorApril;
    public boolean containsSeparatorMay;
    public boolean containsSeparatorJune;
    public boolean containsSeparatorJuly;
    public boolean containsSeparatorAugust;
    public boolean containsSeparatorSeptember;
    public boolean containsSeparatorOctober;
    public boolean containsSeparatorNovember;
    public boolean containsSeparatorDecember;
    private Context context;
    private String displayedAge;

    public AllFragmentAdapter(AllFragment allFragment) {
        this.allFragment = allFragment;
        items = new ArrayList<>();
    }

    private AllFragment getAllFragment() {
        return allFragment;
    }

    public Item getItem(int position) {
        return items.get(position);
    }

    public void addItem(Item item) {
        items.add(item);
        notifyItemInserted(getItemCount() - 1);
    }

    public void addItem(int location, Item item) {
        items.add(location, item);
        notifyItemInserted(location);
    }

    /**
     * Appends an already grouped batch and refreshes it with a single range notification.
     */
    public void addItems(List<Item> newItems) {
        if (newItems.isEmpty()) return;
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void removePerson(int location) {
        if (location >= 0 && location < getItemCount()) {
            items.remove(location);
            notifyItemRemoved(location);

            if (location - 1 >= 0 && !getItem(location - 1).isPerson()) {

                if (location != getItemCount() && !getItem(location).isPerson() && !getItem(location - 1).isPerson()) {
                    Separator separator = (Separator) getItem(location - 1);
                    checkSeparator(separator.type());
                    items.remove(location - 1);
                    notifyItemRemoved(location - 1);

                } else if (location == getItemCount() && !getItem(location - 1).isPerson()) {
                    Separator separator = (Separator) getItem(location - 1);
                    checkSeparator(separator.type());
                    items.remove(location - 1);
                    notifyItemRemoved(location - 1);
                }

            } else if (getItemCount() - 1 >= 0 && !getItem(getItemCount() - 1).isPerson()) {
                Separator separator = (Separator) getItem(getItemCount() - 1);
                checkSeparator(separator.type());
                int locationTemp = getItemCount() - 1;
                items.remove(locationTemp);
                notifyItemRemoved(locationTemp);
            }
        }
    }

    private void checkSeparator(int type) {
        switch (type) {
            case Separator.TYPE_JANUARY:
                containsSeparatorJanuary = false;
                break;
            case Separator.TYPE_FEBRUARY:
                containsSeparatorFebruary = false;
                break;
            case Separator.TYPE_MARCH:
                containsSeparatorMarch = false;
                break;
            case Separator.TYPE_APRIL:
                containsSeparatorApril = false;
                break;
            case Separator.TYPE_MAY:
                containsSeparatorMay = false;
                break;
            case Separator.TYPE_JUNE:
                containsSeparatorJune = false;
                break;
            case Separator.TYPE_JULY:
                containsSeparatorJuly = false;
                break;
            case Separator.TYPE_AUGUST:
                containsSeparatorAugust = false;
                break;
            case Separator.TYPE_SEPTEMBER:
                containsSeparatorSeptember = false;
                break;
            case Separator.TYPE_OCTOBER:
                containsSeparatorOctober = false;
                break;
            case Separator.TYPE_NOVEMBER:
                containsSeparatorNovember = false;
                break;
            case Separator.TYPE_DECEMBER:
                containsSeparatorDecember = false;
                break;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        displayedAge = BirdaysPreferencesManager.getDefaultSharedPreferences(context)
                .getString(Constants.DISPLAYED_AGE_KEY, "0");
        if (viewType == TYPE_PERSON) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_view_all_fragment, parent, false);
            return new ListViewHolder(view);
        } else {
            View separator = LayoutInflater.from(context).inflate(R.layout.model_separator, parent, false);
            return new SeparatorViewHolder(separator);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);

        String[] months = holder.itemView.getResources().getStringArray(R.array.months);

        if (item.isPerson()) {
            holder.itemView.setEnabled(true);
            View itemView = holder.itemView;
            final Person person = ((Person) item);
            final ListViewHolder listViewHolder = ((ListViewHolder) holder);

            listViewHolder.tvName.setText(person.getName());

            long date = person.getDate();

            if (person.isYearUnknown()) {
                listViewHolder.tvAge.setVisibility(View.GONE);
                listViewHolder.tvDate.setText(Utils.getDateWithoutYear(date));
            } else {
                listViewHolder.tvAge.setVisibility(View.VISIBLE);
                final int age = (displayedAge.equals("0") ? Utils.getCurrentAge(date) : Utils.getFutureAge(date));
                listViewHolder.tvDate.setText(Utils.getDate(date));
                GradientDrawable ageCircle = (GradientDrawable) listViewHolder.tvAge.getBackground();
                if (ageCircle != null) {
                    ageCircle.setColor(ContextCompat.getColor(context, getAgeCircleColor(age)));
                }
                listViewHolder.tvAge.setText(String.valueOf(age));
            }

            itemView.setOnLongClickListener(v -> {
                getAllFragment().removePersonDialog(listViewHolder.getLayoutPosition());
                return true;
            });

            itemView.setOnClickListener(v -> {
                allFragment.startActivity(new Intent(context, DetailActivity.class).
                        putExtra(Constants.TIME_STAMP, person.getTimeStamp()));
                if (context instanceof MainActivity) {
                    Utils.overrideOpenTransition((MainActivity) context, R.anim.activity_secondary_in,
                            R.anim.activity_primary_out);
                }
            });
        } else if (item instanceof Separator separator) {
            SeparatorViewHolder separatorViewHolder = (SeparatorViewHolder) holder;
            separatorViewHolder.separatorView.setText(months[separator.type()]);
            if (separatorViewHolder.dividerLine != null) {
                separatorViewHolder.dividerLine.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeAllPersons() {
        int oldSize = getItemCount();
        if (oldSize != 0) {
            items.clear();
            notifyItemRangeRemoved(0, oldSize);
        }

        // The flags describe the contents, so an empty list always resets them — even when the
        // adapter was already empty but a separator flag had not been cleared.
        containsSeparatorJanuary = false;
        containsSeparatorFebruary = false;
        containsSeparatorMarch = false;
        containsSeparatorApril = false;
        containsSeparatorMay = false;
        containsSeparatorJune = false;
        containsSeparatorJuly = false;
        containsSeparatorAugust = false;
        containsSeparatorSeptember = false;
        containsSeparatorOctober = false;
        containsSeparatorNovember = false;
        containsSeparatorDecember = false;
    }

    private int getAgeCircleColor(int age) {
        int colorResId;
        if (age < 10) colorResId = R.color.age1;
        else if (age < 20) colorResId = R.color.age2;
        else if (age < 30) colorResId = R.color.age3;
        else if (age < 40) colorResId = R.color.age4;
        else if (age < 50) colorResId = R.color.age5;
        else if (age < 60) colorResId = R.color.age6;
        else if (age < 70) colorResId = R.color.age7;
        else colorResId = R.color.age8;
        return colorResId;
    }

    @Override
    public int getItemViewType(int position) {
        Item item = getItem(position);
        return item.isPerson() ? TYPE_PERSON : TYPE_SEPARATOR;
    }

    private static class ListViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDate;
        final TextView tvAge;

        ListViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textview_all_name);
            tvDate = itemView.findViewById(R.id.textview_all_date);
            tvAge = itemView.findViewById(R.id.textview_all_age);
        }
    }

    private static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        final TextView separatorView;
        final View dividerLine;

        SeparatorViewHolder(View itemView) {
            super(itemView);
            separatorView = itemView.findViewById(R.id.textview_separator);
            dividerLine = itemView.findViewById(R.id.view_separator_line);
        }
    }
}
