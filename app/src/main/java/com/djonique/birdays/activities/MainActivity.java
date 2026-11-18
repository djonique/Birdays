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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.djonique.birdays.R;
import com.djonique.birdays.adapters.PagerAdapter;
import com.djonique.birdays.fragments.AllFragment;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.BirdaysApplication;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.DeletedRecord;
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.ProgressDialogHelper;
import com.djonique.birdays.utils.ReviewHelper;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity implements
        AllFragment.DeletingPersonListener {

    /**
     * One query per typing burst instead of one per keystroke.
     */
    private static final long SEARCH_DEBOUNCE_MS = 250;
    private static final String STATE_NOTIFICATIONS_PERMISSION_REQUESTED = "STATE_NOTIFICATIONS_PERMISSION_REQUESTED";
    private static final String STATE_CONTACTS_PERMISSION_REQUEST_PENDING = "STATE_CONTACTS_PERMISSION_REQUEST_PENDING";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private CoordinatorLayout container;
    private AppBarLayout appBarLayout;
    private SearchView searchView;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fab;
    private SharedPreferences preferences;
    private PagerAdapter pagerAdapter;
    private ProgressDialogHelper progressDialog;
    private Runnable pendingSearch;
    private boolean notificationsPermissionRequested;
    private boolean contactsPermissionRequestPending;
    /**
     * The process-wide alarm rebuild runs once per screen instance, not on every resume.
     */
    private boolean alarmsRescheduled;
    /**
     * Set when the detail screen deleted a record and the list is about to re-enter.
     */
    private Person pendingDeletedPerson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.setupSystemBarAppearance(this);

        container = findViewById(R.id.container_main);
        appBarLayout = findViewById(R.id.appbar_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        searchView = findViewById(R.id.searchview_main);
        tabLayout = findViewById(R.id.tablayout_main);
        viewPager = findViewById(R.id.viewpager_main);
        fab = findViewById(R.id.fab_main);

        Utils.applySystemBarsInsets(container, appBarLayout, fab, findViewById(R.id.container_pager));
        ReviewHelper.trackLaunch(this);

        FirebaseAnalytics.getInstance(this);

        preferences = BirdaysPreferencesManager.getDefaultSharedPreferences(this);
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

        if (savedInstanceState != null) {
            notificationsPermissionRequested = savedInstanceState.getBoolean(
                    STATE_NOTIFICATIONS_PERMISSION_REQUESTED, false);
            contactsPermissionRequestPending = savedInstanceState.getBoolean(
                    STATE_CONTACTS_PERMISSION_REQUEST_PENDING, false);
        }

        requestPermissionsSequentially();

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
        showDeletionSnackbarIfNeeded();
        rescheduleAlarmsOnce();
    }

    /**
     * Safety net for alarms that were lost without a broadcast reaching {@link
     * com.djonique.birdays.alarm.AlarmSetter} (for example when the process was killed mid-boot).
     * Runs on the repository pool so the database is never opened on the main thread.
     */
    private void rescheduleAlarmsOnce() {
        if (alarmsRescheduled) return;
        alarmsRescheduled = true;
        BirdaysRepository.getInstance(this).rescheduleAllAlarms(null);
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

    private void requestPermissionsSequentially() {
        if (notificationsPermissionRequested || contactsPermissionRequestPending) {
            return;
        }

        if (preferences.getBoolean(getString(R.string.notifications_key), true)
                && PermissionManager.canRequestNotificationsPermission(this)) {
            notificationsPermissionRequested = true;
            PermissionManager.requestNotificationsPermission(this);
            return;
        }

        checkAndRequestContactsPermission();
    }

    private void checkAndRequestContactsPermission() {
        if (!preferences.getBoolean(Constants.CONTACTS_UPLOADED, false)) {
            if (PermissionManager.readingContactsPermissionGranted(this)) {
                importContacts();
            } else if (!contactsPermissionRequestPending) {
                contactsPermissionRequestPending = true;
                PermissionManager.requestReadingContactsPermission(this);
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        pendingDeletedPerson = DeletedRecord.take();
        // When a record was just deleted, only that row has to go. A full reload would rebuild
        // both lists and replace the row-removal animation with a list-wide one, so the row is
        // removed in onResume, once the window is visible again.
        if (pendingDeletedPerson == null) {
            pagerAdapter.addPersonsFromDb();
        }
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
     * The detail screen deletes a record, closes itself and hands the deleted person over. The
     * snackbar is shown here, on the list, and the row is removed through the same adapter call
     * as the long-press deletion in the list, so it animates out instead of the list reloading.
     * "Undo" re-inserts the record and the row animates back in.
     */
    private void showDeletionSnackbarIfNeeded() {
        Person deleted = pendingDeletedPerson;
        if (deleted == null) return;
        pendingDeletedPerson = null;

        final boolean[] undone = {false};

        Snackbar.make(container, R.string.record_deleted, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> {
                    undone[0] = true;
                    restoreDeletedPerson(deleted);
                })
                .show();

        // Posted so the removal animation starts on a visible window when the screen comes back.
        container.post(() -> {
            if (!undone[0]) {
                pagerAdapter.deletePerson(deleted.getTimeStamp());
            }
        });
    }

    private void restoreDeletedPerson(Person person) {
        BirdaysRepository.getInstance(this).addPerson(person,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Person result) {
                        if (isFinishing() || isDestroyed()) return;
                        Utils.notifyWidget(MainActivity.this);
                        pagerAdapter.restorePerson(result);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(MainActivity.this, R.string.recover_records_error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Coalesces keystrokes into a single query. Clearing the field restores the full list right
     * away; otherwise the previous pending query is canceled and a new one is posted after
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

    private void onPageSelected(int position) {
        appBarLayout.setExpanded(true, true);
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_NOTIFICATIONS_PERMISSION_REQUESTED, notificationsPermissionRequested);
        outState.putBoolean(STATE_CONTACTS_PERMISSION_REQUEST_PENDING, contactsPermissionRequestPending);
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
        if (requestCode == Constants.NOTIFICATIONS_PERMISSION_CODE) {
            notificationsPermissionRequested = false;
            container.post(this::checkAndRequestContactsPermission);
        } else if (requestCode == Constants.READ_CONTACTS_PERMISSION_CODE) {
            contactsPermissionRequestPending = false;
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!preferences.getBoolean(Constants.WRONG_CONTACTS_FORMAT, false)) {
                    importContacts();
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

    /**
     * Imports contacts on the repository pool. The progress dialog is dismissed on both outcomes,
     * and the UI is touched only while the activity is still alive.
     */
    private void importContacts() {
        progressDialog = new ProgressDialogHelper(this);
        progressDialog.startProgressDialog(getString(R.string.loading_contacts));

        BirdaysRepository.getInstance(this).importContacts(
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Integer inserted) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        preferences.edit().putBoolean(Constants.CONTACTS_UPLOADED, true).apply();
                        pagerAdapter.addPersonsFromDb();
                        Utils.notifyWidget(MainActivity.this);
                        Toast.makeText(MainActivity.this, R.string.contacts_uploaded,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        dismissProgressDialog();
                        preferences.edit().putBoolean(Constants.WRONG_CONTACTS_FORMAT, true).apply();
                        Toast.makeText(MainActivity.this, R.string.loading_contacts_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismissProgressDialog();
            progressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacksAndMessages(null);
        // The dialog is not cancelable, so it must be dismissed before the window goes away;
        // otherwise it would be left attached to a destroyed activity.
        dismissProgressDialog();
        super.onDestroy();
    }
}
