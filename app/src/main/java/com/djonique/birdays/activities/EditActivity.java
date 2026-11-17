/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;

import com.djonique.birdays.R;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.DatePickerManager;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class EditActivity extends AppCompatActivity {

    private static final String STATE_DATE = "STATE_DATE";
    private static final String STATE_FORM_LOADED = "STATE_FORM_LOADED";
    private final Calendar calendar = Calendar.getInstance();
    private TextInputLayout tilEditName;
    private EditText etName;
    private TextInputLayout tilEditDate;
    private EditText etDate;
    private AppCompatCheckBox checkBox;
    private EditText etPhoneNumber;
    private EditText etEmail;
    private Person person;
    private long date;
    private boolean yearUnknown;
    private boolean hideOkButton = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Utils.setupSystemBarAppearance(this);
        View container = findViewById(R.id.container_edit);
        AppBarLayout appBarLayout = findViewById(R.id.appbar_edit);
        Utils.applySystemBarsInsets(container, appBarLayout, null, container);

        Toolbar toolbar = findViewById(R.id.toolbar_edit);
        toolbar.setTitle(R.string.editing_label);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        tilEditName = findViewById(R.id.til_edit_name);
        etName = findViewById(R.id.edittext_edit_name);
        tilEditDate = findViewById(R.id.til_edit_date);
        etDate = findViewById(R.id.edittext_edit_date);
        checkBox = findViewById(R.id.checkbox_edit);
        etPhoneNumber = findViewById(R.id.edittext_edit_phone);
        etEmail = findViewById(R.id.edittext_edit_email);

        // A recreated screen already has the user's edits restored by the framework. Only the
        // calendar (and the date derived from it) is not part of the view state.
        boolean formLoaded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_FORM_LOADED, false);
        if (formLoaded) {
            long savedDate = savedInstanceState.getLong(STATE_DATE, 0L);
            if (savedDate > 0L) {
                calendar.setTimeInMillis(savedDate);
                date = savedDate;
            }
        }

        Intent intent = getIntent();
        long timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);

        // The record is read on a worker thread. The form is already inflated, so the callback
        // only has to fill it in — after re-checking that this activity is still alive.
        BirdaysRepository.getInstance(this).getPerson(timeStamp,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Person result) {
                        if (isFinishing() || isDestroyed()) return;
                        if (result == null) {
                            Toast.makeText(EditActivity.this, R.string.record_deleted,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        person = result;
                        // Filling the form again after a rotation would discard what the user
                        // has typed (the async read can land after the framework restored the
                        // views). A rotation that happened before the first fill still needs it.
                        if (!formLoaded) {
                            yearUnknown = person.isYearUnknown();
                            calendar.setTimeInMillis(person.getDate());
                            setupUI();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(EditActivity.this, R.string.recover_records_error,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        etDate.setOnClickListener(v -> pickDate());

        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateDate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> checkBoxListener());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                Utils.overrideCloseTransition(EditActivity.this, R.anim.activity_primary_in, R.anim.activity_secondary_out);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FORM_LOADED, person != null);
        if (person != null) {
            outState.putLong(STATE_DATE, calendar.getTimeInMillis());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        if (hideOkButton) menu.findItem(R.id.menu_edit_ok).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (itemId == R.id.menu_edit_ok) {
            updatePerson();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupUI() {
        etName.setText(person.getName());
        etName.setSelection(etName.getText().length());

        date = person.getDate();
        if (yearUnknown) {
            etDate.setText(Utils.getDateWithoutYear(date));
        } else {
            etDate.setText(Utils.getDate(date));
        }

        checkBox.setChecked(yearUnknown);
        etPhoneNumber.setText(person.getPhoneNumber());
        etEmail.setText(person.getEmail());
    }

    private void updatePerson() {
        if (person == null) return;
        person.setName(updateText(etName));
        person.setDate(calendar.getTimeInMillis());
        person.setYearUnknown(checkBox.isChecked());
        person.setPhoneNumber(updateText(etPhoneNumber));
        person.setEmail(updateText(etEmail));

        // The repository also re-schedules the alarm for the new date, so RESULT_OK is reported
        // only after the write finished — the detail screen then re-reads the committed row.
        BirdaysRepository.getInstance(this).updatePerson(person,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(Person result) {
                        if (isFinishing() || isDestroyed()) return;
                        Utils.notifyWidget(EditActivity.this);
                        setResult(RESULT_OK, new Intent());
                        finish();
                        Utils.overrideCloseTransition(EditActivity.this,
                                R.anim.activity_primary_in, R.anim.activity_secondary_out);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(EditActivity.this, R.string.recover_records_error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String updateText(EditText editText) {
        String result = "";
        if (editText != null && editText.length() != 0) {
            result = editText.getText().toString();
        }
        return result;
    }

    private void pickDate() {
        new DatePickerManager(this, calendar).showDialog(this::setDate);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        date = calendar.getTimeInMillis();
        if (!checkBox.isChecked()) {
            etDate.setText(Utils.getDate(date));
        } else {
            etDate.setText(Utils.getDateWithoutYear(date));
        }
    }

    private void validateName() {
        if (etName.length() == 0) {
            tilEditName.setError(getString(R.string.error_hint));
            hideOkButton = true;
        } else {
            if (fieldsValid()) {
                hideOkButton = false;
            }
            tilEditName.setErrorEnabled(false);
        }
        invalidateOptionsMenu();
    }

    private void validateDate() {
        if (fieldsValid()) {
            tilEditDate.setErrorEnabled(false);
            if (etName.length() != 0) {
                hideOkButton = false;
            }
        } else {
            tilEditDate.setError(getString(R.string.wrong_date));
            hideOkButton = true;
        }
        invalidateOptionsMenu();
    }

    private void checkBoxListener() {
        if (checkBox.isChecked()) {
            etDate.setText(Utils.getDateWithoutYear(date));
        } else {
            etDate.setText(Utils.getDate(date));
        }
        if (fieldsValid()) {
            tilEditDate.setErrorEnabled(false);
            if (etName.length() != 0) {
                hideOkButton = false;
            }
        } else {
            tilEditDate.setError(getString(R.string.wrong_date));
            hideOkButton = true;
        }
        invalidateOptionsMenu();
    }

    private boolean fieldsValid() {
        return (!Utils.isEmptyDate(etDate) && Utils.isRightDate(calendar))
                || (!Utils.isEmptyDate(etDate) && checkBox.isChecked());
    }
}
