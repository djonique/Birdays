/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.DetailActivity;
import com.djonique.birdays.activities.MainActivity;
import com.djonique.birdays.ad.NativeAdHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonthFragmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PERSON = 0;
    private static final int TYPE_AD = 1;
    private final List<Person> persons;
    private Context context;
    private NativeAd nativeAd;

    public MonthFragmentAdapter() {
        persons = new ArrayList<>();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setNativeAd(NativeAd nativeAd) {
        this.nativeAd = nativeAd;
        notifyDataSetChanged();
    }

    private int getAdPosition() {
        return Math.min(2, persons.size());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addPerson(Person person) {
        if (Utils.isCurrentMonth(person.getDate())) {
            persons.add(person);
            Collections.sort(persons);
            notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addPersons(List<Person> personList) {
        for (Person person : personList) {
            if (Utils.isCurrentMonth(person.getDate())) {
                persons.add(person);
            }
        }
        Collections.sort(persons);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (nativeAd != null && position == getAdPosition()) {
            return TYPE_AD;
        }
        return TYPE_PERSON;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        if (viewType == TYPE_AD) {
            View view = LayoutInflater.from(context).inflate(R.layout.native_ad_card, parent, false);
            return new AdViewHolder(view);
        }

        View view = LayoutInflater.from(context).inflate(
                R.layout.description_card_view, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_AD) {
            NativeAdHelper.bindNativeAd((NativeAdView) holder.itemView, nativeAd);
            holder.itemView.setVisibility(View.VISIBLE);
            return;
        }

        int personIndex = (nativeAd != null && position > getAdPosition()) ? position - 1 : position;
        if (personIndex < 0 || personIndex >= persons.size()) return;

        final Person person = persons.get(personIndex);
        long date = person.getDate();
        final String email = person.getEmail();
        final String phoneNumber = person.getPhoneNumber();

        CardViewHolder cardViewHolder = (CardViewHolder) holder;

        cardViewHolder.tvName.setText(person.getName());

        if (person.isYearUnknown()) {
            cardViewHolder.tvDate.setText(Utils.getDateWithoutYear(date));
            cardViewHolder.tvAge.setVisibility(View.GONE);
            changeCardViewBackgroundColor(date, cardViewHolder.cardView, cardViewHolder.tvDaysLeft);
        } else {
            cardViewHolder.tvDate.setText(Utils.getDate(date));
            cardViewHolder.tvAge.setVisibility(View.VISIBLE);
            String age = context.getString(R.string.age) + Utils.getCurrentAge(date);
            cardViewHolder.tvAge.setText(age);
            changeCardViewBackgroundColor(date, cardViewHolder.cardView, cardViewHolder.tvDaysLeft);
        }

        cardViewHolder.relativeLayout.setOnClickListener(v -> {
            context.startActivity(new Intent(context, DetailActivity.class)
                    .putExtra(Constants.TIME_STAMP, person.getTimeStamp()));
            if (context instanceof MainActivity) {
                Utils.overrideOpenTransition((MainActivity) context,
                        R.anim.activity_secondary_in, R.anim.activity_primary_out);
            }
        });

        if (email != null && !email.isEmpty()) {
            enableButton(cardViewHolder.btnEmail);
            cardViewHolder.btnEmail.setOnClickListener(v ->
                    context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SENDTO)
                            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.happy_birthday))
                            .setData(Uri.parse(Constants.MAILTO + email)), null)));
        } else {
            disableButton(cardViewHolder.btnEmail);
        }

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            enableButton(cardViewHolder.btnCall);
            enableButton(cardViewHolder.btnChat);
            cardViewHolder.btnCall.setOnClickListener(v ->
                    context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_DIAL,
                            Uri.parse(Constants.TEL + phoneNumber)), null)));

            cardViewHolder.btnChat.setOnClickListener(v ->
                    context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SENDTO)
                            .setData(Uri.parse(Constants.SMSTO + phoneNumber)), null)));
        } else {
            disableButton(cardViewHolder.btnCall);
            disableButton(cardViewHolder.btnChat);
        }
    }

    @Override
    public int getItemCount() {
        return persons.size() + (nativeAd != null ? 1 : 0);
    }

    public void removePerson(long timeStamp) {
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);

            if (person.getTimeStamp() == timeStamp) {
                persons.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void removeAllPersons() {
        int oldSize = persons.size();
        if (oldSize > 0) {
            persons.clear();
            notifyItemRangeRemoved(0, oldSize);
        }
    }

    private void enableButton(ImageButton button) {
        button.setColorFilter(ContextCompat.getColor(context, R.color.md_theme_secondary));
        button.setClickable(true);
    }

    private void disableButton(ImageButton button) {
        button.setColorFilter(ContextCompat.getColor(context, R.color.md_theme_outline_variant));
        button.setClickable(false);
    }

    private void changeCardViewBackgroundColor(long date, CardView cardView, TextView textView) {
        String daysLeft = Utils.daysLeft(context, date);
        if (Utils.isBirthdayPassed(date)) {
            textView.setVisibility(View.GONE);
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cardview_background));
        } else {
            textView.setVisibility(View.VISIBLE);
            String today = context.getString(R.string.today);
            if (daysLeft.equals(today)) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cardview_birthday));
                textView.setText(today);
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cardview_background));
                String summary = context.getString(R.string.days_left) + ": " + daysLeft;
                textView.setText(summary);
            }
        }
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        RelativeLayout relativeLayout;
        TextView tvName, tvDate, tvAge, tvDaysLeft;
        ImageButton btnEmail, btnChat, btnCall;

        CardViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardview_card);
            relativeLayout = itemView.findViewById(R.id.relativelayout_card);
            tvName = itemView.findViewById(R.id.textview_card_name);
            tvDate = itemView.findViewById(R.id.textview_card_date);
            tvAge = itemView.findViewById(R.id.textview_card_age);
            tvDaysLeft = itemView.findViewById(R.id.textview_card_left);
            btnEmail = itemView.findViewById(R.id.imagebutton_card_email);
            btnChat = itemView.findViewById(R.id.imagebutton_card_chat);
            btnCall = itemView.findViewById(R.id.imagebutton_card_call);
        }
    }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        AdViewHolder(View itemView) {
            super(itemView);
        }
    }
}
