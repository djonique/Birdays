/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.djonique.birdays.models.Person;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Read-only instrumented checks of the real repository/database on the device. No records are
 * written, so the user's data is not modified.
 */
@RunWith(AndroidJUnit4.class)
public class BirdaysRepositoryInstrumentedTest {

    private BirdaysRepository repository;

    @Before
    public void setUp() {
        repository = BirdaysRepository.getInstance(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void getPersons_existingDatabase_deliversNonNullList() throws InterruptedException {
        CallbackResult<List<Person>> result = new CallbackResult<>();
        repository.getPersons(result);

        List<Person> persons = result.awaitSuccess();
        assertNotNull(persons);
        for (Person person : persons) {
            assertNotNull(person);
        }
    }

    @Test
    public void getPerson_unknownTimeStamp_deliversNull() throws InterruptedException {
        CallbackResult<Person> result = new CallbackResult<>();
        repository.getPerson(Long.MAX_VALUE, result);

        assertNull(result.awaitSuccess());
    }

    @Test
    public void searchPersons_nameThatCannotExist_deliversEmptyList() throws InterruptedException {
        CallbackResult<List<Person>> result = new CallbackResult<>();
        repository.searchPersons("zzz_no_such_person_in_birdays_zzz", false, result);

        assertTrue(result.awaitSuccess().isEmpty());
    }

    /** Collects the main-thread callback and blocks the instrumentation thread until it arrives. */
    private static final class CallbackResult<T> implements BirdaysRepository.Callback<T> {

        private final CountDownLatch latch = new CountDownLatch(1);
        private T value;
        private Throwable error;

        @Override
        public void onSuccess(T result) {
            value = result;
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            latch.countDown();
        }

        T awaitSuccess() throws InterruptedException {
            assertTrue("Repository callback was not invoked within 10 seconds",
                    latch.await(10, TimeUnit.SECONDS));
            assertNull("Repository reported an error", error);
            return value;
        }
    }
}
