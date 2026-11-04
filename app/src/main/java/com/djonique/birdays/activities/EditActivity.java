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
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.djonique.birdays.R;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.DatePickerManager;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.textfield.TextInputLayout;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;

public class EditActivity extends AppCompatActivity implements
        android.app.DatePickerDialog.OnDateSetListener,
        com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener {

    private TextInputLayout tilEditName;
    private EditText etName;
    private TextInputLayout tilEditDate;
    private EditText etDate;
    private AppCompatCheckBox checkBox;
    private EditText etPhoneNumber;
    private EditText etEmail;

    private DbHelper dbHelper;
    private Person person;
    private Calendar calendar;
    private long date;
    private boolean yearUnknown;
    private boolean hideOkButton = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        tilEditName = findViewById(R.id.til_edit_name);
        etName = findViewById(R.id.edittext_edit_name);
        tilEditDate = findViewById(R.id.til_edit_date);
        etDate = findViewById(R.id.edittext_edit_date);
        checkBox = findViewById(R.id.checkbox_edit);
        etPhoneNumber = findViewById(R.id.edittext_edit_phone);
        etEmail = findViewById(R.id.edittext_edit_email);

        Intent intent = getIntent();
        long timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);

        dbHelper = new DbHelper(this);
        person = dbHelper.query().getPerson(timeStamp);
        yearUnknown = person.isYearUnknown();

        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(person.getDate());

        setupUI();

        etDate.setOnClickListener(v -> pickDate());

        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateDate();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> checkBoxListener());
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
            onBackPressed();
            return true;
        } else if (itemId == R.id.menu_edit_ok) {
            updatePerson();
            setAlarms(person);
            Utils.notifyWidget(this);
            setResult(RESULT_OK, new Intent());
            finish();
            this.overridePendingTransition(R.anim.activity_primary_in, R.anim.activity_secondary_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_primary_in, R.anim.activity_secondary_out);
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
        person.setName(updateText(etName));
        person.setDate(calendar.getTimeInMillis());
        person.setYearUnknown(checkBox.isChecked());
        person.setPhoneNumber(updateText(etPhoneNumber));
        person.setEmail(updateText(etEmail));
        dbHelper.updateRecord(person);
    }

    private String updateText(EditText editText) {
        String result = "";
        if (editText != null && editText.length() != 0) {
            result = editText.getText().toString();
        }
        return result;
    }

    private void setAlarms(Person person) {
        AlarmHelper alarmHelper = new AlarmHelper(this);
        alarmHelper.removeAlarms(person.getTimeStamp());
        alarmHelper.setAlarms(person);
    }

    private void pickDate() {
        new DatePickerManager(this, calendar).showDialog(this, this);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        setDate(year, month, dayOfMonth);
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        setDate(year, monthOfYear, dayOfMonth);
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