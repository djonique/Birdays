/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.adapters;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
        // addMonthPersonsFromDb()/findPerson() re-apply the same ad after every load; refreshing
        // the whole list for an unchanged reference caused a second full rebind per search.
        if (this.nativeAd == nativeAd) return;
        this.nativeAd = nativeAd;
        notifyDataSetChanged();
    }

    /**
     * Drops the reference without touching the list; used when the fragment view is destroyed.
     */
    public void clearNativeAd() {
        nativeAd = null;
    }

    private int getAdPosition() {
        return Math.min(2, persons.size());
    }

    /**
     * The adapter list contains an extra ad row when a native ad is shown, so a person index and
     * an adapter position are not the same thing. Kept package-private so the position fix can be
     * exercised without a real ad view.
     */
    boolean isAdRowPresent() {
        return nativeAd != null;
    }

    /**
     * Maps a person index to its adapter position. Everything at or after the ad row is shifted
     * down by one; the ad itself sits between the first and the remaining persons.
     */
    private int adapterPositionFor(int personIndex) {
        if (isAdRowPresent() && personIndex >= getAdPosition()) {
            return personIndex + 1;
        }
        return personIndex;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addPerson(Person person) {
        if (Utils.isCurrentMonth(person.getDate())) {
            persons.add(person);
            Collections.sort(persons);
            notifyDataSetChanged();
        }
    }

    /**
     * Replaces the list in one pass. The previous clear-then-add sequence produced two full
     * refreshes for every load and briefly blanked the screen while the read was in flight.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void replacePersons(List<Person> personList) {
        persons.clear();
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
        if (isAdRowPresent() && position == getAdPosition()) {
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
            cardViewHolder.btnEmail.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_SENDTO)
                            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.happy_birthday))
                            .setData(Uri.parse(Constants.MAILTO + email));
                    context.startActivity(Intent.createChooser(intent, null));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            disableButton(cardViewHolder.btnEmail);
        }

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            enableButton(cardViewHolder.btnCall);
            enableButton(cardViewHolder.btnChat);
            cardViewHolder.btnCall.setOnClickListener(v -> {
                try {
                    String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
                    Intent intent = new Intent(Intent.ACTION_DIAL,
                            Uri.parse(Constants.TEL + (cleanNumber.isEmpty() ? phoneNumber : cleanNumber)));
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
                }
            });

            cardViewHolder.btnChat.setOnClickListener(v -> {
                try {
                    String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
                    Intent intent = new Intent(Intent.ACTION_SENDTO)
                            .setData(Uri.parse(Constants.SMSTO + (cleanNumber.isEmpty() ? phoneNumber : cleanNumber)));
                    context.startActivity(Intent.createChooser(intent, null));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            disableButton(cardViewHolder.btnCall);
            disableButton(cardViewHolder.btnChat);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder.getItemViewType() == TYPE_AD) {
            // A detached ad row is hidden so a recycled holder never shows a stale creative
            // before the next bind. NativeAd in 8.6.0 exposes no destroy(): the owning screen is
            // responsible for dropping its reference (see MonthFragment.onDestroyView()).
            holder.itemView.setVisibility(View.GONE);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return persons.size() + (isAdRowPresent() ? 1 : 0);
    }

    public void removePerson(long timeStamp) {
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);

            if (person.getTimeStamp() == timeStamp) {
                // The adapter position depends on the list size, so it has to be computed before
                // the person is removed.
                int adapterPosition = adapterPositionFor(i);
                persons.remove(i);
                notifyItemRemoved(adapterPosition);
                break;
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void removeAllPersons() {
        if (persons.isEmpty()) return;
        // A range update would mix person indexes with adapter indexes (the ad row is still
        // present), so the whole list is refreshed once instead.
        persons.clear();
        notifyDataSetChanged();
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
        final CardView cardView;
        final RelativeLayout relativeLayout;
        final TextView tvName;
        final TextView tvDate;
        final TextView tvAge;
        final TextView tvDaysLeft;
        final ImageButton btnEmail;
        final ImageButton btnChat;
        final ImageButton btnCall;

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
