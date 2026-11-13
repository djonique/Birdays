/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.os.Process;

import androidx.annotation.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates named daemon threads that run at background priority.
 *
 * <p>Daemon threads never keep the process alive, so a repository shut down while work is
 * still queued cannot leak a non-daemon thread. The priority is lowered inside {@code run()}
 * because thread priority is a per-thread property of the native thread that executes the
 * task, not of the {@link Thread} object that was created on the caller's thread.</p>
 */
public final class NamedDaemonThreadFactory implements ThreadFactory {

    private final String namePrefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedDaemonThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            runnable.run();
        }, namePrefix + "-" + counter.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
