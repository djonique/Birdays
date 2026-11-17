/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM tests for {@link NamedDaemonThreadFactory}. Threads are inspected but never started:
 * {@code Process.setThreadPriority} inside {@code run()} is an Android call.
 */
public class NamedDaemonThreadFactoryTest {

    @Test
    public void newThread_createdThread_isDaemon() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("test-pool");

        assertTrue(factory.newThread(() -> {
        }).isDaemon());
    }

    @Test
    public void newThread_firstThread_nameContainsPrefixAndOne() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("birdays-db");

        assertEquals("birdays-db-1", factory.newThread(() -> {
        }).getName());
    }

    @Test
    public void newThread_multipleCalls_incrementsNameSuffix() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("birdays-db");

        Thread first = factory.newThread(() -> {
        });
        Thread second = factory.newThread(() -> {
        });
        Thread third = factory.newThread(() -> {
        });

        assertEquals("birdays-db-1", first.getName());
        assertEquals("birdays-db-2", second.getName());
        assertEquals("birdays-db-3", third.getName());
    }

    @Test
    public void newThread_multipleCalls_returnsDistinctThreads() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("birdays-db");

        assertNotSame(factory.newThread(() -> {
        }), factory.newThread(() -> {
        }));
    }
}
