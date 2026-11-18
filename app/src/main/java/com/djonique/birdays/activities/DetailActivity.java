/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.BuildConfig;
import com.djonique.birdays.R;
import com.djonique.birdays.ad.NativeAdHelper;
import com.djonique.birdays.adapters.FamousFragmentAdapter;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.DeletedRecord;
import com.djonique.birdays.utils.ReviewHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdView;

import java.util.Calendar;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> editActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) return;
                Toast.makeText(this, R.string.record_edited, Toast.LENGTH_SHORT).show();
                // Recreate instead of launching a second DetailActivity on top of this one: the
                // screen reloads the edited person and the back stack keeps a single detail entry.
                recreate();
            });
    private ImageView ivSeasonPicture;
    private TextView tvAge;
    private TextView tvDate;
    private TextView tvDaysLeft;
    private RelativeLayout rlDaysSinceBirthday;
    private TextView tvDaysSinceBirthday;
    private ImageView ivZodiacSign;
    private TextView tvZodiacSign;
    private CardView cardViewInfo;
    private RelativeLayout rlPhoneNumber;
    private TextView tvPhoneNumber;
    private RelativeLayout rlEmail;
    private TextView tvEmail;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private ImageButton btnPhone;
    private ImageButton btnChat;
    private ImageButton btnEmail;
    private com.yandex.mobile.ads.nativeads.NativeAd mNativeAd;
    private Person person;
    private long timeStamp, date;
    private String phoneNumber, email, displayedAge;
    private boolean yearUnknown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Utils.setupSystemBarAppearance(this);

        CoordinatorLayout container = findViewById(R.id.container_detail);
        toolbar = findViewById(R.id.toolbar_detail);
        ivSeasonPicture = findViewById(R.id.imageview_detail_picture);
        tvAge = findViewById(R.id.textview_detail_age);
        tvDate = findViewById(R.id.textview_detail_date);
        tvDaysLeft = findViewById(R.id.textview_detail_left);
        rlDaysSinceBirthday = findViewById(R.id.relativelayout_detail_since);
        tvDaysSinceBirthday = findViewById(R.id.textview_detail_since);
        ivZodiacSign = findViewById(R.id.imageview_detail_zodiac);
        tvZodiacSign = findViewById(R.id.textview_detail_zodiac);
        cardViewInfo = findViewById(R.id.cardview_detail_info);
        rlPhoneNumber = findViewById(R.id.relativelayout_detail_phone);
        tvPhoneNumber = findViewById(R.id.textview_detail_phone);
        rlEmail = findViewById(R.id.relativelayout_detail_email);
        tvEmail = findViewById(R.id.textview_detail_email);
        recyclerView = findViewById(R.id.recyclerview_detail);
        FloatingActionButton fabDetail = findViewById(R.id.fab_detail);
        btnPhone = findViewById(R.id.imagebutton_detail_phone);
        btnChat = findViewById(R.id.imagebutton_detail_chat);
        btnEmail = findViewById(R.id.imagebutton_detail_email);

        SharedPreferences preferences = BirdaysPreferencesManager.getDefaultSharedPreferences(this);
        boolean adEnabled = preferences.getBoolean(getString(R.string.show_ads_key), true);
        if (BuildConfig.DEBUG) {
            Log.d("DetailActivity", "adEnabled = " + adEnabled);
        }
        displayedAge = preferences.getString(Constants.DISPLAYED_AGE_KEY, "0");

        NativeAdView nativeAdView = findViewById(R.id.native_ad_card);
        if (nativeAdView != null) {
            nativeAdView.setVisibility(View.GONE);
            if (adEnabled) {
                loadNativeAd(nativeAdView);
            }
        }


        Utils.setupDayNightTheme(preferences);

        Intent intent = getIntent();
        timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Utils.applySystemBarsInsets(container, findViewById(R.id.appbar_detail), null, container);

        recyclerView.setFocusable(false);

        requestInAppReview();

        if (fabDetail != null) fabDetail.setOnClickListener(v -> starEditActivity());

        loadPerson();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        });
    }

    /**
     * Loads the record identified by {@link #timeStamp}. Called on creation and again from
     * {@link #onNewIntent(Intent)} when a notification opens another person on top of this screen
     * (the activity is launched with SINGLE_TOP | CLEAR_TOP).
     */
    private void loadPerson() {
        // The record is read on a worker thread; every view is touched only after the lifecycle
        // re-check below, so a slow read cannot update a finished screen.
        BirdaysRepository.getInstance(this).getPerson(timeStamp,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Person result) {
                        if (isFinishing() || isDestroyed()) return;
                        if (result == null) {
                            Toast.makeText(DetailActivity.this, R.string.record_deleted, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        person = result;
                        date = person.getDate();
                        yearUnknown = person.isYearUnknown();
                        phoneNumber = person.getPhoneNumber();
                        email = person.getEmail();

                        toolbar.setTitle(person.getName());

                        setupUI();
                        loadBornThisDay();

                        if (btnPhone != null) btnPhone.setOnClickListener(v -> makeCall());
                        if (btnChat != null) btnChat.setOnClickListener(v -> sendMessage());
                        if (btnEmail != null) btnEmail.setOnClickListener(v -> sendEmail());
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(DetailActivity.this, R.string.recover_records_error, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);
        loadPerson();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            handleBackPressed();
            return true;
        } else if (itemId == R.id.menu_detail_delete) {
            deletePersonDialog(person);
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleBackPressed() {
        closeActivity();
    }

    private void closeActivity() {
        if (isFinishing()) return;
        finish();
        Utils.overrideCloseTransition(this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
    }

    @Override
    protected void onDestroy() {
        releaseNativeAd();
        super.onDestroy();
    }

    /**
     * Yandex Mobile Ads 8.6.0 has no {@code destroy()} on {@code NativeAd} (verified against the
     * AAR's class file): the SDK's lifecycle contract is to detach the listener and drop every
     * reference the app holds. The ad then becomes eligible for collection together with this
     * screen instead of keeping the previous activity's view hierarchy reachable.
     */
    private void releaseNativeAd() {
        if (mNativeAd != null) {
            mNativeAd.setNativeAdEventListener(null);
            mNativeAd = null;
        }
    }

    private void loadNativeAd(@NonNull NativeAdView nativeAdView) {
        nativeAdView.setVisibility(View.GONE);
        com.djonique.birdays.ad.NativeAdCache.getInstance().loadAd(this, new NativeAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd nativeAd) {
                // The request outlives the screen when the user backs out while it is in flight.
                if (isFinishing() || isDestroyed()) return;
                releaseNativeAd();
                mNativeAd = nativeAd;
                if (NativeAdHelper.bindNativeAd(nativeAdView, nativeAd)) {
                    nativeAdView.setVisibility(View.VISIBLE);
                } else {
                    releaseNativeAd();
                    nativeAdView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                if (isFinishing() || isDestroyed()) return;
                if (BuildConfig.DEBUG) {
                    Log.e("DetailActivity", "Native ad failed to load: code=" + adRequestError.getCode() + ", desc=" + adRequestError.getDescription());
                }
                nativeAdView.setVisibility(View.GONE);
            }
        });
    }

    private void setupUI() {
        setSeasonImage();

        tvDaysLeft.setText(Utils.daysLeft(this, date));

        if (yearUnknown) {
            tvDate.setText(Utils.getDateWithoutYear(date));
            tvAge.setVisibility(View.GONE);
            rlDaysSinceBirthday.setVisibility(View.GONE);
        } else {
            tvDate.setText(Utils.getDate(date));
            tvAge.setVisibility(View.VISIBLE);
            tvAge.setText(String.valueOf(displayedAge.equals("0") ? Utils.getCurrentAge(date) : Utils.getFutureAge(date)));
            rlDaysSinceBirthday.setVisibility(View.VISIBLE);
            tvDaysSinceBirthday.setText(Utils.daysSinceBirthday(date));
        }

        int zodiacId = Utils.getZodiacId(date);
        tvZodiacSign.setText(getString(zodiacId));
        ivZodiacSign.setImageResource(Utils.getZodiacImage(zodiacId));

        // Every visibility is reset explicitly: onNewIntent() reuses these views for another
        // record, so a branch left over from the previous person must not leak into the next.
        if (isEmpty(phoneNumber) && isEmpty(email)) {
            cardViewInfo.setVisibility(View.GONE);
        } else {
            cardViewInfo.setVisibility(View.VISIBLE);
        }

        if (isEmpty(phoneNumber)) {
            rlPhoneNumber.setVisibility(View.GONE);
        } else {
            rlPhoneNumber.setVisibility(View.VISIBLE);
            tvPhoneNumber.setText(String.valueOf(person.getPhoneNumber()));
        }

        if (isEmpty(email)) {
            rlEmail.setVisibility(View.GONE);
        } else {
            rlEmail.setVisibility(View.VISIBLE);
            tvEmail.setText(person.getEmail());
        }
    }

    private boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    private void loadBornThisDay() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);

        FamousFragmentAdapter adapter = new FamousFragmentAdapter();
        recyclerView.setAdapter(adapter);

        BirdaysRepository.getInstance(this).getFamousBornThisDay(date,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(List<Person> famousPersons) {
                        if (isFinishing() || isDestroyed()) return;
                        for (int i = 0; i < famousPersons.size(); i++) {
                            adapter.addPerson(famousPersons.get(i));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // The "born this day" strip is decorative; an empty list is a valid state.
                    }
                });
    }

    private void starEditActivity() {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra(Constants.TIME_STAMP, timeStamp);
        editActivityLauncher.launch(intent);
        Utils.overrideOpenTransition(this, R.anim.activity_secondary_in, R.anim.activity_primary_out);
    }

    private void setSeasonImage() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        int month = calendar.get(Calendar.MONTH);
        if (month < 2 || month == 11) {
            ivSeasonPicture.setImageResource(R.drawable.img_winter);
        } else if (month < 5) {
            ivSeasonPicture.setImageResource(R.drawable.img_spring);
        } else if (month < 8) {
            ivSeasonPicture.setImageResource(R.drawable.img_summer);
        } else {
            ivSeasonPicture.setImageResource(R.drawable.img_autumn);
        }
    }

    private void deletePersonDialog(final Person person) {
        if (person == null) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getString(R.string.delete_record_confirm, person.getName()));
        builder.setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
            dialog.dismiss();
            deletePerson();
        });
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Deletes the record and closes the screen. The deleted record is handed to the list screen,
     * which shows the "Record deleted" snackbar with Undo once this screen is gone. The write is
     * awaited before closing so the list reload behind it cannot see the record anymore.
     */
    private void deletePerson() {
        BirdaysRepository.getInstance(this).removePerson(timeStamp,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        // The widget and the list mirror the database, so they are updated even
                        // when this screen is already closing.
                        Utils.notifyWidget(getApplicationContext());
                        DeletedRecord.set(person);
                        if (isFinishing() || isDestroyed()) return;
                        finish();
                        Utils.overrideCloseTransition(DetailActivity.this,
                                R.anim.activity_primary_in, R.anim.activity_secondary_out);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(DetailActivity.this, R.string.recover_records_error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void requestInAppReview() {
        if (!ReviewHelper.shouldRequestReview(this)) return;

        ReviewManager manager = ReviewManagerFactory.create(this);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            ReviewInfo reviewInfo = task.getResult();
            if (isFinishing() || isDestroyed()) return;

            manager.launchReviewFlow(this, reviewInfo)
                    .addOnCompleteListener(flowTask -> ReviewHelper.markReviewRequested(this));
        });
    }

    private void makeCall() {
        if (phoneNumber == null || phoneNumber.isEmpty()) return;
        try {
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            Intent intent = new Intent(Intent.ACTION_DIAL,
                    Uri.parse(Constants.TEL + (cleanNumber.isEmpty() ? phoneNumber : cleanNumber)));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        if (phoneNumber == null || phoneNumber.isEmpty()) return;
        try {
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            Intent intent = new Intent(Intent.ACTION_SENDTO)
                    .putExtra(Constants.ADDRESS, phoneNumber)
                    .setData(Uri.parse(Constants.SMSTO + (cleanNumber.isEmpty() ? phoneNumber : cleanNumber)));
            startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail() {
        if (email == null || email.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO)
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{email})
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.happy_birthday))
                    .setData(Uri.parse(Constants.MAILTO + email));
            startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found_error, Toast.LENGTH_SHORT).show();
        }
    }
}
