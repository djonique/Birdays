/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.repository;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.djonique.birdays.models.Person;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validation rules of the Drive restore. Dates are local-time millisecond values, so a birthday
 * in 1961 is a negative number; that is valid data and must not reject the backup.
 */
public class BirdaysRepositoryBackupTest {

    @Test
    public void validateBackup_birthdayBefore1970_isAccepted() {
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("Ирина Анатольевна", -281750400000L));
        persons.add(new Person("Мама", -5270400000L));
        persons.add(new Person("Сегодня", 0L));

        // Must not throw: only an unreadable (empty) backup is an error.
        BirdaysRepository.validateBackup(persons);
    }

    @Test
    public void validateBackup_emptyBackup_isRejected() {
        assertThrows(IllegalStateException.class,
                () -> BirdaysRepository.validateBackup(Collections.emptyList()));
    }

    @Test
    public void validateBackup_truncatedBackupWithoutPersons_isRejected() {
        List<Person> persons = Collections.emptyList();
        try {
            BirdaysRepository.validateBackup(persons);
            fail("An empty backup must not be restored");
        } catch (IllegalStateException expected) {
            // The current data has to survive a truncated download.
        }
    }
}
