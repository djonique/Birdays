/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.djonique.birdays.R;

public class ProgressDialogHelper {

    private static final String ILLEGAL_ARGUMENT_EXCEPTION = "IllegalArgumentException";

    private final Context context;
    private AlertDialog progressDialog;

    public ProgressDialogHelper(Context context) {
        this.context = context;
    }

    /**
     * Starts progress dialog
     */
    @SuppressLint("InflateParams")
    public void startProgressDialog(String message) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        TextView messageView = view.findViewById(R.id.textview_progress_message);
        messageView.setText(message);

        progressDialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    /**
     * Stops progress dialog
     */
    public void dismissProgressDialog() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                Toast.makeText(context, ILLEGAL_ARGUMENT_EXCEPTION, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
