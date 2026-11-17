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

import androidx.appcompat.app.AlertDialog;

import com.djonique.birdays.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProgressDialogHelper {

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

        progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    /**
     * Stops progress dialog. Safe to call from {@code onDestroy()} (or twice): a dialog whose
     * window is already detached throws {@link IllegalArgumentException} from {@code dismiss()},
     * which is expected there and carries no user-facing meaning.
     */
    public void dismissProgressDialog() {
        final AlertDialog dialog = progressDialog;
        if (dialog == null) return;
        try {
            dialog.dismiss();
        } catch (IllegalArgumentException ignored) {
            // The activity window is gone; there is nothing left to dismiss.
        } finally {
            progressDialog = null;
        }
    }
}
