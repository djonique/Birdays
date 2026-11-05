/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.DialogFragment;

import com.djonique.birdays.R;
import com.djonique.birdays.alarm.AlarmHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.ContactsHelper;
import com.djonique.birdays.utils.DatePickerManager;
import com.djonique.birdays.utils.PermissionManager;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class NewPersonDialogFragment extends DialogFragment implements
        android.app.DatePickerDialog.OnDateSetListener,
        com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener {

    private AddingPersonListener addingPersonListener;
    private EditText etName, etPhone, etEmail, etDate;
    private AppCompatCheckBox checkBox;
    private Calendar calendar;
    private String name;
    private long date;

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && getActivity() != null
                        && result.getData() != null) {
                    fillFromContact(result.getData());
                }
            });

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            addingPersonListener = (AddingPersonListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement AddingPersonListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.new_record);

        calendar = Calendar.getInstance();
        final Person person = new Person();

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View container = inflater.inflate(R.layout.fragment_dialog, null);

        AppCompatButton addFromContactsButton = container.findViewById(R.id.button_dialog_afc);
        if (addFromContactsButton != null) {
            addFromContactsButton.setOnClickListener(view -> {
                if (PermissionManager.readingContactsPermissionGranted(requireActivity())) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            ContactsContract.Contacts.CONTENT_URI);
                    try {
                        contactPickerLauncher.launch(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), R.string.open_contacts_error, Toast.LENGTH_LONG).show();
                    }
                } else {
                    PermissionManager.requestReadingContactsPermission(requireActivity());
                }
            });
        }

        final TextInputLayout tilName = container.findViewById(R.id.til_dialog_name);
        if (tilName != null) etName = tilName.getEditText();

        final TextInputLayout tilPhone = container.findViewById(R.id.til_dialog_phone);
        if (tilPhone != null) etPhone = tilPhone.getEditText();

        final TextInputLayout tilEmail = container.findViewById(R.id.til_dialog_email);
        if (tilEmail != null) etEmail = tilEmail.getEditText();

        final TextInputLayout tilDate = container.findViewById(R.id.til_dialog_date);
        etDate = container.findViewById(R.id.edittext_dialog_date);
        if (etDate != null) {
            etDate.setOnClickListener(v -> {
                if (etDate.length() == 0) {
                    etDate.setText("");
                }
                new DatePickerManager(requireActivity(), calendar)
                        .showDialog(NewPersonDialogFragment.this, NewPersonDialogFragment.this);
            });
        }

        checkBox = container.findViewById(R.id.checkbox_dialog);

        builder.setView(container);
        builder.setPositiveButton(R.string.ok_button, (dialog, which) -> {
            if (etName != null) {
                name = etName.getText().toString();
                person.setName(name);
            }

            if (!Utils.isEmptyDate(etDate)) {
                person.setDate(calendar.getTimeInMillis());
                new AlarmHelper(requireActivity()).setAlarms(person);
            }

            if (checkBox != null) person.setYearUnknown(checkBox.isChecked());

            if (etPhone != null && etPhone.length() != 0) {
                person.setPhoneNumber(etPhone.getText().toString());
            } else {
                person.setPhoneNumber("");
            }

            if (etEmail != null && etEmail.length() != 0) {
                person.setEmail(etEmail.getText().toString());
            } else {
                person.setEmail("");
            }

            if (addingPersonListener != null) {
                addingPersonListener.onPersonAdded(person);
            }
            dialog.dismiss();
        });

        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> {
            if (addingPersonListener != null) {
                addingPersonListener.onPersonAddedCancel();
            }
            dialog.cancel();
        });

        AlertDialog alertDialog = builder.create();
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
        alertDialog.setOnShowListener(dialog -> {
            final Button positiveButton =
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);

            if (etDate != null && etDate.length() == 0 && positiveButton != null) positiveButton.setEnabled(false);

            if (etName != null && etName.length() == 0) {
                if (positiveButton != null) positiveButton.setEnabled(false);
                if (tilName != null) tilName.setError(getString(R.string.error_hint));
            }

            if (etName != null) {
                etName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.length() == 0) {
                            if (tilName != null) tilName.setError(getString(R.string.error_hint));
                            if (positiveButton != null) positiveButton.setEnabled(false);
                        } else {
                            if (tilName != null) tilName.setErrorEnabled(false);
                            if (isDateCorrect() && positiveButton != null) {
                                positiveButton.setEnabled(true);
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }

            if (etDate != null) {
                etDate.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (isDateCorrect()) {
                            if (tilDate != null) tilDate.setErrorEnabled(false);
                            if (etName != null && etName.length() != 0 && positiveButton != null) {
                                positiveButton.setEnabled(true);
                            }
                        } else {
                            if (tilDate != null) tilDate.setError(getString(R.string.wrong_date));
                            if (positiveButton != null) positiveButton.setEnabled(false);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {}
                });
            }

            if (checkBox != null) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (checkBox.isChecked() && !Utils.isEmptyDate(etDate)) {
                        etDate.setText(Utils.getDateWithoutYear(date));
                    } else if (!checkBox.isChecked() && !Utils.isEmptyDate(etDate)) {
                        etDate.setText(Utils.getDate(date));
                    }

                    if (isDateCorrect()) {
                        if (tilDate != null) tilDate.setErrorEnabled(false);
                        if (etName != null && etName.length() != 0 && positiveButton != null) {
                            positiveButton.setEnabled(true);
                        }
                    } else {
                        if (tilDate != null) tilDate.setError(getString(R.string.wrong_date));
                        if (positiveButton != null) positiveButton.setEnabled(false);
                    }
                });
            }
        });
        return alertDialog;
    }

    private boolean isDateCorrect() {
        return !Utils.isEmptyDate(etDate) && Utils.isRightDate(calendar)
                || !Utils.isEmptyDate(etDate) && checkBox != null && checkBox.isChecked();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        setDate(year, month, dayOfMonth);
    }

    @Override
    public void onDateSet(com.wdullaer.materialdatetimepicker.date.DatePickerDialog view,
                          int year, int monthOfYear, int dayOfMonth) {
        setDate(year, monthOfYear, dayOfMonth);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        date = calendar.getTimeInMillis();
        if (checkBox != null && !checkBox.isChecked()) {
            etDate.setText(Utils.getDate(date));
        } else {
            etDate.setText(Utils.getDateWithoutYear(date));
        }
    }

    private void fillFromContact(Intent data) {
        Uri contactData = data.getData();
        ContentResolver contentResolver = getActivity().getContentResolver();
        ContactsHelper contactsHelper = new ContactsHelper(getActivity(), contentResolver);
        Cursor cursor = null;
        if (contactData != null) {
            cursor = contentResolver.query(contactData, null, null, null, null);
        }
        if (cursor != null && cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") String id = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts._ID));
                if (etName != null) etName.setText(contactsHelper.getContactName(contentResolver, id));
                if (etPhone != null) etPhone.setText(contactsHelper.getContactPhoneNumber(contentResolver, id));
                if (etEmail != null) etEmail.setText(contactsHelper.getContactEmail(contentResolver, id));
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public interface AddingPersonListener {
        void onPersonAdded(Person person);
        void onPersonAddedCancel();
    }
}