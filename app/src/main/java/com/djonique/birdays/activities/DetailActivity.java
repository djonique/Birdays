/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.BuildConfig;
import com.djonique.birdays.R;
import com.djonique.birdays.ad.NativeAdHelper;
import com.djonique.birdays.adapters.FamousFragmentAdapter;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.ReviewHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DetailActivity extends AppCompatActivity {

    private static final long INTERSTITIAL_INTERVAL = TimeUnit.DAYS.toMillis(1);

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

    private InterstitialAdLoader interstitialAdLoader;
    private InterstitialAd mInterstitialAd;
    private com.yandex.mobile.ads.nativeads.NativeAd mNativeAd;
    private boolean interstitialShown;
    private DbHelper dbHelper;
    private Person person;
    private long timeStamp, date;
    private String phoneNumber, email, displayedAge;
    private boolean yearUnknown;

    private final ActivityResultLauncher<Intent> editActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, R.string.record_edited, Toast.LENGTH_SHORT).show();
                    Intent refresh = new Intent(this, DetailActivity.class);
                    refresh.putExtra(Constants.TIME_STAMP, timeStamp);
                    startActivity(refresh);
                    this.finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Utils.setupSystemBarAppearance(this);

        CoordinatorLayout container = findViewById(R.id.container_detail);
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
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
        ImageButton btnPhone = findViewById(R.id.imagebutton_detail_phone);
        ImageButton btnChat = findViewById(R.id.imagebutton_detail_chat);
        ImageButton btnEmail = findViewById(R.id.imagebutton_detail_email);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean adEnabled = preferences.getBoolean(getString(R.string.show_ads_key), true);
        if (BuildConfig.DEBUG) {
            Log.d("DetailActivity", "adEnabled = " + adEnabled);
        }
        displayedAge = preferences.getString(Constants.DISPLAYED_AGE_KEY, "0");

        incrementDetailVisitCount(preferences);

        if (adEnabled && canShowInterstitial(preferences)) {
            loadInterstitialAd();
        }

        NativeAdView nativeAdView = findViewById(R.id.native_ad_card);
        if (nativeAdView != null) {
            nativeAdView.setVisibility(View.GONE);
            if (adEnabled) {
                loadNativeAd(nativeAdView);
            }
        }

        dbHelper = new DbHelper(this);

        Utils.setupDayNightTheme(preferences);

        Intent intent = getIntent();
        timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);
        person = dbHelper.query().getPerson(timeStamp);
        if (person == null) {
            Toast.makeText(this, R.string.record_deleted, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        date = person.getDate();
        yearUnknown = person.isYearUnknown();
        phoneNumber = person.getPhoneNumber();
        email = person.getEmail();

        toolbar.setTitle(person.getName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Utils.applySystemBarsInsets(container, findViewById(R.id.appbar_detail), null, container);

        setupUI();

        loadBornThisDay();

        recyclerView.setFocusable(false);

        requestInAppReview();

        if (fabDetail != null) fabDetail.setOnClickListener(v -> starEditActivity());
        if (btnPhone != null) btnPhone.setOnClickListener(v -> makeCall());
        if (btnChat != null) btnChat.setOnClickListener(v -> sendMessage());
        if (btnEmail != null) btnEmail.setOnClickListener(v -> sendEmail());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        });
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
        if (mInterstitialAd != null && !interstitialShown) {
            interstitialShown = true;
            mInterstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                @Override
                public void onAdShown() {
                    PreferenceManager.getDefaultSharedPreferences(DetailActivity.this)
                            .edit()
                            .putLong(Constants.INTERSTITIAL_LAST_SHOWN, System.currentTimeMillis())
                            .apply();
                }

                @Override
                public void onAdFailedToShow(@NonNull AdError adError) {
                    closeActivity();
                }

                @Override
                public void onAdDismissed() {
                    closeActivity();
                }

                @Override
                public void onAdClicked() {
                }

                @Override
                public void onAdImpression(@Nullable ImpressionData impressionData) {
                }
            });
            mInterstitialAd.show(this);
        } else {
            closeActivity();
        }
    }

    private void closeActivity() {
        if (isFinishing()) return;
        finish();
        Utils.overrideCloseTransition(this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
    }

    @Override
    protected void onDestroy() {
        if (interstitialAdLoader != null) {
            interstitialAdLoader.cancelLoading();
        }
        if (mInterstitialAd != null) {
            mInterstitialAd.setAdEventListener(null);
        }
        if (mNativeAd != null) {
            mNativeAd.setNativeAdEventListener(null);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    private boolean canShowInterstitial(SharedPreferences preferences) {
        long lastShown = preferences.getLong(Constants.INTERSTITIAL_LAST_SHOWN, 0);
        boolean intervalPassed = System.currentTimeMillis() - lastShown >= INTERSTITIAL_INTERVAL;
        int visitCount = preferences.getInt(Constants.INTERSTITIAL_VISIT_COUNT, 0);
        boolean reachedThirdVisit = visitCount >= 3;
        return intervalPassed && reachedThirdVisit;
    }

    private void incrementDetailVisitCount(SharedPreferences preferences) {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String lastVisitDate = preferences.getString(Constants.INTERSTITIAL_LAST_VISIT_DATE, "");
        int visitCount = preferences.getInt(Constants.INTERSTITIAL_VISIT_COUNT, 0);
        if (!today.equals(lastVisitDate)) {
            visitCount = 1;
            preferences.edit()
                    .putString(Constants.INTERSTITIAL_LAST_VISIT_DATE, today)
                    .putInt(Constants.INTERSTITIAL_VISIT_COUNT, visitCount)
                    .apply();
        } else {
            visitCount++;
            preferences.edit()
                    .putInt(Constants.INTERSTITIAL_VISIT_COUNT, visitCount)
                    .apply();
        }
        if (BuildConfig.DEBUG) {
            Log.d("DetailActivity", "DetailActivity visit count today: " + visitCount);
        }
    }

    private void loadInterstitialAd() {
        interstitialAdLoader = new InterstitialAdLoader(this);
        interstitialAdLoader.loadAd(new AdRequest.Builder(BuildConfig.INTERSTITIAL_AD_UNIT_ID).build(),
                new InterstitialAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                    }
                });
    }

    private void loadNativeAd(@NonNull NativeAdView nativeAdView) {
        nativeAdView.setVisibility(View.GONE);
        com.djonique.birdays.ad.NativeAdCache.getInstance().loadAd(this, new NativeAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd nativeAd) {
                mNativeAd = nativeAd;
                NativeAdHelper.bindNativeAd(nativeAdView, nativeAd);
                nativeAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
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
            tvAge.setText(String.valueOf(displayedAge.equals("0") ? Utils.getCurrentAge(date) : Utils.getFutureAge(date)));
            tvDaysSinceBirthday.setText(Utils.daysSinceBirthday(date));
        }

        int zodiacId = Utils.getZodiacId(date);
        tvZodiacSign.setText(getString(zodiacId));
        ivZodiacSign.setImageResource(Utils.getZodiacImage(zodiacId));

        if (isEmpty(phoneNumber) && isEmpty(email))
            cardViewInfo.setVisibility(View.GONE);

        if (isEmpty(phoneNumber)) {
            rlPhoneNumber.setVisibility(View.GONE);
        } else {
            tvPhoneNumber.setText(String.valueOf(person.getPhoneNumber()));
        }

        if (isEmpty(email)) {
            rlEmail.setVisibility(View.GONE);
        } else {
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

        List<Person> famousPersons = dbHelper.query().getFamousBornThisDay(date);
        for (int i = 0; i < famousPersons.size(); i++) {
            adapter.addPerson(famousPersons.get(i));
        }
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getString(R.string.delete_record_confirm, person.getName()));
        builder.setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
            new AlarmHelper(getApplicationContext()).removeAlarms(timeStamp);
            dbHelper.removeRecord(timeStamp);
            Utils.notifyWidget(getApplicationContext());
            dialog.dismiss();
            finish();
            Utils.overrideCloseTransition(this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
        });
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel());
        builder.show();
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
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_DIAL,
                Uri.parse(Constants.TEL + phoneNumber)), null));
    }

    private void sendMessage() {
        Intent intent = new Intent(Intent.ACTION_SENDTO)
                .putExtra(Constants.ADDRESS, phoneNumber)
                .setData(Uri.parse(Constants.SMSTO + phoneNumber));
        startActivity(Intent.createChooser(intent, null));
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO)
                .putExtra(Intent.EXTRA_EMAIL, new String[]{email})
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.happy_birthday))
                .setData(Uri.parse(Constants.MAILTO + email));
        startActivity(Intent.createChooser(intent, null));
    }
}