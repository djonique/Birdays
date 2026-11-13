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
import android.os.Handler;
import android.os.Looper;
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
import androidx.viewpager2.widget.ViewPager2;

import com.djonique.birdays.R;
import com.djonique.birdays.adapters.PagerAdapter;
import com.djonique.birdays.fragments.AllFragment;
import com.djonique.birdays.utils.BirdaysApplication;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.ContactsHelper;
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.ReviewHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity implements
        AllFragment.DeletingPersonListener,
        ContactsHelper.LoadingContactsListener {

    /** One query per typing burst instead of one per keystroke. */
    private static final long SEARCH_DEBOUNCE_MS = 250;

    private CoordinatorLayout container;
    private AppBarLayout appBarLayout;
    private Toolbar toolbar;
    private SearchView searchView;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fab;

    private SharedPreferences preferences;
    private PagerAdapter pagerAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private boolean notificationsPermissionRequested;
    private boolean contactsPermissionRequestPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.setupSystemBarAppearance(this);

        container = findViewById(R.id.container_main);
        appBarLayout = findViewById(R.id.appbar_main);
        toolbar = findViewById(R.id.toolbar_main);
        searchView = findViewById(R.id.searchview_main);
        tabLayout = findViewById(R.id.tablayout_main);
        viewPager = findViewById(R.id.viewpager_main);
        fab = findViewById(R.id.fab_main);

        Utils.applySystemBarsInsets(container, appBarLayout, fab, findViewById(R.id.container_pager));
        ReviewHelper.trackLaunch(this);

        FirebaseAnalytics.getInstance(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean adEnabled = preferences.getBoolean(getString(R.string.show_ads_key), true);
        if (adEnabled) {
            com.djonique.birdays.ad.NativeAdCache.getInstance().preload(this);
        }

        Utils.setupDayNightTheme(preferences);

        pagerAdapter = new PagerAdapter(this);

        setSupportActionBar(toolbar);

        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))).attach();

        if (!preferences.getBoolean(Constants.CONTACTS_UPLOADED, false)) {
            if (!PermissionManager.readingContactsPermissionGranted(this)) {
                contactsPermissionRequestPending = true;
            }
            new ContactsHelper(this).loadContacts(this, preferences);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scheduleSearch(newText);
                return false;
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                MainActivity.this.onPageSelected(position);
            }
        });

        fab.setOnClickListener(v -> startNewPersonActivity());

        // Start page: only on a fresh launch. After a recreation (e.g. a theme change) the pager
        // restores the page it was showing, and overriding it here would leave the tab bar
        // pointing at a different page than the content.
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(
                    Integer.parseInt(preferences.getString(Constants.START_PAGE, "1")), false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BirdaysApplication.activityResumed();
        syncSelectedPage();
        // The pager may apply its restored page on the next layout pass.
        viewPager.post(this::syncSelectedPage);
        requestNotificationsPermissionIfNeeded();
    }

    /**
     * After the activity is recreated the pager restores its page itself, while the tab bar may
     * have restored an older selection. The pager is the source of truth, so the tab bar and the
     * page-dependent UI are aligned with it every time the screen comes back.
     */
    private void syncSelectedPage() {
        int position = viewPager.getCurrentItem();
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        if (tab != null && !tab.isSelected()) {
            tab.select();
        }
        updatePageUi(position);
    }

    private void requestNotificationsPermissionIfNeeded() {
        if (notificationsPermissionRequested
                || contactsPermissionRequestPending
                || !preferences.getBoolean(getString(R.string.notifications_key), true)
                || !PermissionManager.canRequestNotificationsPermission(this)) {
            return;
        }
        notificationsPermissionRequested = true;
        PermissionManager.requestNotificationsPermission(this);
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
            Utils.overrideOpenTransition(this, R.anim.activity_secondary_in, R.anim.activity_primary_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPersonDeleted(long timeStamp) {
        pagerAdapter.deletePerson(timeStamp);
        Utils.notifyWidget(this);
    }

    /**
     * Coalesces keystrokes into a single query. Clearing the field restores the full list right
     * away; otherwise the previous pending query is cancelled and a new one is posted after
     * {@link #SEARCH_DEBOUNCE_MS}. The fragments still drop stale results by sequence number, so a
     * query that is already running cannot overwrite a newer one.
     */
    private void scheduleSearch(String query) {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
        if (query == null || query.trim().isEmpty()) {
            pagerAdapter.search("");
            return;
        }
        pendingSearch = () -> pagerAdapter.search(query);
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void onPageSelected(int position) {        appBarLayout.setExpanded(true, true);
        updatePageUi(position);
    }

    private void updatePageUi(int position) {
        if (position == PagerAdapter.FAMOUS_FRAGMENT_POSITION) {
            fab.hide();
            searchView.setVisibility(View.GONE);
        } else {
            fab.show();
            searchView.setVisibility(View.VISIBLE);
        }
    }

    private void startNewPersonActivity() {
        startActivity(new Intent(this, NewPersonActivity.class));
        Utils.overrideOpenTransition(this, R.anim.activity_secondary_in, R.anim.activity_primary_out);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.READ_CONTACTS_PERMISSION_CODE) {
            contactsPermissionRequestPending = false;
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                    new ContactsHelper(this).loadContacts(this, preferences);
                }
            } else {
                Snackbar.make(container, R.string.permission_required,
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_allow_text, v -> openApplicationSettings())
                        .show();
            }
            // Ask for notifications only after the contacts prompt is dismissed,
            // so the user does not get two permission dialogs at the same time
            container.post(this::requestNotificationsPermissionIfNeeded);
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
        pagerAdapter.addPersonsFromDb();
    }

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}