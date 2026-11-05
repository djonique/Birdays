/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.BuildConfig;
import com.djonique.birdays.R;
import com.djonique.birdays.adapters.FamousFragmentAdapter;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kobakei.ratethisapp.RateThisApp;

import java.util.Calendar;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private static final int INSTALL_DAYS = 7;
    private static final int LAUNCH_TIMES = 5;
    private static final int EDIT_ACTIVITY = 4;

    private CoordinatorLayout container;
    private Toolbar toolbar;
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
    private FloatingActionButton fabDetail;
    private ImageButton btnPhone;
    private ImageButton btnChat;
    private ImageButton btnEmail;

    private InterstitialAd mInterstitialAd;
    private DbHelper dbHelper;
    private Person person;
    private long timeStamp, date;
    private String phoneNumber, email, displayedAge;
    private boolean yearUnknown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        container = findViewById(R.id.container_detail);
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
        fabDetail = findViewById(R.id.fab_detail);
        btnPhone = findViewById(R.id.imagebutton_detail_phone);
        btnChat = findViewById(R.id.imagebutton_detail_chat);
        btnEmail = findViewById(R.id.imagebutton_detail_email);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean adEnabled = preferences.getBoolean(getString(R.string.ad_interstitial_key), true);
        displayedAge = preferences.getString(Constants.DISPLAYED_AGE_KEY, "0");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && adEnabled) {
            InterstitialAd.load(this, BuildConfig.INTERSTITIAL_AD_UNIT_ID, new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                        }
                    });
        }

        dbHelper = new DbHelper(this);

        Utils.setupDayNightTheme(preferences);

        Intent intent = getIntent();
        timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);
        person = dbHelper.query().getPerson(timeStamp);
        date = person.getDate();
        yearUnknown = person.isYearUnknown();
        phoneNumber = person.getPhoneNumber();
        email = person.getEmail();

        toolbar.setTitle(person.getName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        setupUI();

        loadBornThisDay();

        recyclerView.setFocusable(false);

        rateThisAppInit(this);

        if (fabDetail != null) fabDetail.setOnClickListener(v -> starEditActivity());
        if (btnPhone != null) btnPhone.setOnClickListener(v -> makeCall());
        if (btnChat != null) btnChat.setOnClickListener(v -> sendMessage());
        if (btnEmail != null) btnEmail.setOnClickListener(v -> sendEmail());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.menu_detail_delete) {
            deletePersonDialog(person);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        }
        finish();
        overridePendingTransition(R.anim.activity_primary_in, R.anim.activity_secondary_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == EDIT_ACTIVITY && resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.record_edited, Toast.LENGTH_SHORT).show();
            Intent refresh = new Intent(this, DetailActivity.class);
            refresh.putExtra(Constants.TIME_STAMP, timeStamp);
            startActivity(refresh);
            this.finish();
        }
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
        return text == null || text.equals("");
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
        startActivityForResult(intent, EDIT_ACTIVITY);
        overridePendingTransition(R.anim.activity_secondary_in, R.anim.activity_primary_out);
    }

    private void setSeasonImage() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        int month = calendar.get(Calendar.MONTH);
        if (month >= 0 && month < 2 || month == 11) {
            ivSeasonPicture.setImageResource(R.drawable.img_winter);
        } else if (month >= 2 && month < 5) {
            ivSeasonPicture.setImageResource(R.drawable.img_spring);
        } else if (month >= 5 && month < 8) {
            ivSeasonPicture.setImageResource(R.drawable.img_summer);
        } else ivSeasonPicture.setImageResource(R.drawable.img_autumn);
    }

    private void deletePersonDialog(final Person person) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_record_text) + person.getName() + "?");
        builder.setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
            new AlarmHelper(getApplicationContext()).removeAlarms(timeStamp);
            dbHelper.removeRecord(timeStamp);
            Utils.notifyWidget(getApplicationContext());
            dialog.dismiss();
            finish();
            overridePendingTransition(R.anim.activity_primary_in, R.anim.activity_secondary_out);
        });
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void rateThisAppInit(Context context) {
        RateThisApp.onCreate(context);
        RateThisApp.Config config = new RateThisApp.Config(INSTALL_DAYS, LAUNCH_TIMES);
        RateThisApp.init(config);
        RateThisApp.showRateDialogIfNeeded(context);
    }

    private void makeCall() {
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_DIAL,
                Uri.parse(Constants.TEL + phoneNumber)), null));
    }

    private void sendMessage() {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setType(Constants.TYPE_SMS)
                .putExtra(Constants.ADDRESS, phoneNumber)
                .setData(Uri.parse(Constants.SMSTO + phoneNumber));
        startActivity(Intent.createChooser(intent, null));
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO)
                .setType(Constants.TYPE_EMAIL)
                .putExtra(Intent.EXTRA_EMAIL, new String[]{email})
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.happy_birthday))
                .setData(Uri.parse(Constants.MAILTO + email));
        startActivity(Intent.createChooser(intent, null));
    }
}