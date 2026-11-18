/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class NewPersonActivity extends AppCompatActivity {

    private static final String STATE_DATE = "STATE_DATE";
    private final Calendar calendar = Calendar.getInstance();
    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    fillFromContact(result.getData());
                }
            });
    private final ActivityResultLauncher<String> readContactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openContactsPicker();
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                }
            });
    private TextInputLayout tilName;
    private TextInputLayout tilDate;
    private EditText etName;
    private EditText etDate;
    private EditText etPhone;
    private EditText etEmail;
    private AppCompatCheckBox checkBox;
    private boolean hideOkButton = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_person);
        Utils.setupSystemBarAppearance(this);

        View container = findViewById(R.id.container_new_person);
        AppBarLayout appBarLayout = findViewById(R.id.appbar_new_person);
        Utils.applySystemBarsInsets(container, appBarLayout, null, container);

        Toolbar toolbar = findViewById(R.id.toolbar_new_person);
        toolbar.setTitle(R.string.new_record);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        tilName = findViewById(R.id.til_new_person_name);
        etName = findViewById(R.id.edittext_new_person_name);
        tilDate = findViewById(R.id.til_new_person_date);
        etDate = findViewById(R.id.edittext_new_person_date);
        checkBox = findViewById(R.id.checkbox_new_person);
        etPhone = findViewById(R.id.edittext_new_person_phone);
        etEmail = findViewById(R.id.edittext_new_person_email);

        if (savedInstanceState != null) {
            long savedDate = savedInstanceState.getLong(STATE_DATE, 0L);
            if (savedDate > 0L) {
                calendar.setTimeInMillis(savedDate);
            }
        }

        findViewById(R.id.button_add_from_contacts).setOnClickListener(v -> addFromContacts());

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
                Utils.overrideCloseTransition(NewPersonActivity.this,
                        R.anim.activity_primary_in, R.anim.activity_secondary_out);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_DATE, calendar.getTimeInMillis());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        MenuItem okItem = menu.findItem(R.id.menu_edit_ok);
        if (okItem != null) okItem.setVisible(!hideOkButton);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (itemId == R.id.menu_edit_ok) {
            addPerson();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addPerson() {
        Person person = new Person();
        person.setName(etName.getText().toString().trim());
        person.setDate(calendar.getTimeInMillis());
        person.setYearUnknown(checkBox.isChecked());
        person.setPhoneNumber(getText(etPhone));
        person.setEmail(getText(etEmail));

        BirdaysRepository.getInstance(this).addPerson(person, new BirdaysRepository.Callback<>() {
            @Override
            public void onSuccess(Person result) {
                if (isFinishing() || isDestroyed()) return;
                Utils.notifyWidget(NewPersonActivity.this);
                Intent intent = new Intent(NewPersonActivity.this, DetailActivity.class);
                intent.putExtra(Constants.TIME_STAMP, result.getTimeStamp());
                startActivity(intent);
                Utils.overrideOpenTransition(NewPersonActivity.this,
                        R.anim.activity_secondary_in, R.anim.activity_primary_out);
                finish();
            }

            @Override
            public void onError(Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(NewPersonActivity.this, R.string.recover_records_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(EditText editText) {
        return editText != null && editText.length() != 0 ? editText.getText().toString() : "";
    }

    private void addFromContacts() {
        if (PermissionManager.readingContactsPermissionGranted(this)) {
            openContactsPicker();
        } else {
            readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void openContactsPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        try {
            contactPickerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found_error, Toast.LENGTH_LONG).show();
        }
    }

    private void fillFromContact(Intent data) {
        Uri contactData = data.getData();
        if (contactData == null) return;

        BirdaysRepository.getInstance(this).getContactDetails(contactData,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(BirdaysRepository.ContactDetails details) {
                        if (isFinishing() || isDestroyed()) return;
                        etName.setText(details.getName());
                        etPhone.setText(details.getPhoneNumber());
                        etEmail.setText(details.getEmail());
                    }

                    @Override
                    public void onError(Throwable t) {
                        // The picked contact can be gone by the time the query runs; keeping the
                        // form as it is lets the user enter the data manually.
                    }
                });
    }

    private void pickDate() {
        new DatePickerManager(this, calendar).showDialog(this::setDate);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (checkBox.isChecked()) {
            etDate.setText(Utils.getDateWithoutYear(calendar.getTimeInMillis()));
        } else {
            etDate.setText(Utils.getDate(calendar.getTimeInMillis()));
        }
    }

    private void validateName() {
        if (etName.length() == 0) {
            tilName.setError(getString(R.string.error_hint));
        } else {
            tilName.setErrorEnabled(false);
        }
        updateOkButton();
    }

    private void validateDate() {
        if (Utils.isEmptyDate(etDate) || isDateValid()) {
            tilDate.setErrorEnabled(false);
        } else {
            tilDate.setError(getString(R.string.wrong_date));
        }
        updateOkButton();
    }

    private void checkBoxListener() {
        if (!Utils.isEmptyDate(etDate)) {
            if (checkBox.isChecked()) {
                etDate.setText(Utils.getDateWithoutYear(calendar.getTimeInMillis()));
            } else {
                etDate.setText(Utils.getDate(calendar.getTimeInMillis()));
            }
        }
        validateDate();
    }

    private void updateOkButton() {
        boolean valid = etName.length() != 0 && !Utils.isEmptyDate(etDate) && isDateValid();
        hideOkButton = !valid;
        invalidateOptionsMenu();
    }

    private boolean isDateValid() {
        return Utils.isRightDate(calendar) || checkBox.isChecked();
    }
}
