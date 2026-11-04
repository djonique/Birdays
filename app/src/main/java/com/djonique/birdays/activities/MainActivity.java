/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.djonique.birdays.R;
import com.djonique.birdays.ad.Ad;
import com.djonique.birdays.adapters.PagerAdapter;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.dialogs.NewPersonDialogFragment;
import com.djonique.birdays.fragments.AllFragment;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.BirdaysApplication;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.ContactsHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity implements
        NewPersonDialogFragment.AddingPersonListener,
        AllFragment.DeletingPersonListener,
        ContactsHelper.LoadingContactsListener {

    private static final String NEW_PERSON_DIALOG_TAG = "NEW_PERSON_DIALOG_TAG";

    public DbHelper dbHelper;

    private CoordinatorLayout container;
    private AppBarLayout appBarLayout;
    private Toolbar toolbar;
    private SearchView searchView;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;
    private AdView adView;

    private SharedPreferences preferences;
    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container_main);
        appBarLayout = findViewById(R.id.appbar_main);
        toolbar = findViewById(R.id.toolbar_main);
        searchView = findViewById(R.id.searchview_main);
        tabLayout = findViewById(R.id.tablayout_main);
        viewPager = findViewById(R.id.viewpager_main);
        fab = findViewById(R.id.fab_main);
        adView = findViewById(R.id.banner_main);

        FirebaseAnalytics.getInstance(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Utils.setupDayNightTheme(preferences);

        dbHelper = new DbHelper(this);

        pagerAdapter = new PagerAdapter(getSupportFragmentManager(), this);

        setSupportActionBar(toolbar);

        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);

        if (!preferences.getBoolean(Constants.CONTACTS_UPLOADED, false)) {
            new ContactsHelper(this, getContentResolver()).loadContacts(preferences);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                pagerAdapter.search(newText);
                return false;
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                MainActivity.this.onPageSelected(position);
            }
        });

        fab.setOnClickListener(v -> showDialog());

        if (preferences.getBoolean(getString(R.string.ad_banner_key), true)) {
            Ad.showBannerAd(container, adView, fab);
        }

        // Start page
        viewPager.setCurrentItem(Integer.parseInt(preferences.getString(Constants.START_PAGE, "1")));
    }

    @Override
    protected void onResume() {
        super.onResume();
        BirdaysApplication.activityResumed();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        pagerAdapter.addPersonsFromDb();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BirdaysApplication.activityPaused();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.activity_secondary_in, R.anim.activity_primary_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPersonAdded(Person person) {
        pagerAdapter.addPerson(person);
        Utils.notifyWidget(this);
        Snackbar.make(findViewById(R.id.container_main), R.string.record_added, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onPersonAddedCancel() {
    }

    @Override
    public void onPersonDeleted(long timeStamp) {
        pagerAdapter.deletePerson(timeStamp);
        Utils.notifyWidget(this);
    }

    private void onPageSelected(int position) {
        appBarLayout.setExpanded(true, true);
        if (position == PagerAdapter.FAMOUS_FRAGMENT_POSITION) {
            fab.hide();
            searchView.setVisibility(View.GONE);
        } else {
            fab.show();
            searchView.setVisibility(View.VISIBLE);
        }
    }

    private void showDialog() {
        NewPersonDialogFragment newPersonDialogFragment = new NewPersonDialogFragment();
        newPersonDialogFragment.show(getSupportFragmentManager(), NEW_PERSON_DIALOG_TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == Constants.READ_CONTACTS_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                    new ContactsHelper(this, getContentResolver()).loadContacts(preferences);
                }
            } else {
                Snackbar.make(container, R.string.permission_required,
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_allow_text, v -> openApplicationSettings())
                        .show();
            }
        }
    }

    /**
     * Opens application settings
     */
    private void openApplicationSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())));
    }

    @Override
    public void onContactsUploaded() {
        if (viewPager.getAdapter() != null) viewPager.getAdapter().notifyDataSetChanged();
    }
}