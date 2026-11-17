/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.djonique.birdays.models.Person;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The record count travels with the backup as {@code appProperties["recordCount"]} and is only a
 * hint for the restore dialog: a missing or unreadable value must fall back to the number of
 * records parsed from the downloaded file instead of failing the restore.
 */
public class BackupMetadataTest {

    // ------------------------------------------------------------- upload side

    @Test
    public void createBackup_appPropertiesContainCountOfSerializedList() {
        List<Person> persons = Arrays.asList(
                new Person("Alice", 1000L),
                new Person("Bob", -281750400000L),
                new Person("Carol", 2000L));

        ExportHelper.Backup backup = new ExportHelper.Backup(new File("backup_upload.xml"), persons);

        assertEquals(3, backup.getRecordCount());
        assertEquals("3", backup.getAppProperties().get(BackupMetadata.KEY_RECORD_COUNT));
        assertEquals("3", backup.getAppProperties().get("recordCount"));
    }

    @Test
    public void createBackup_emptyDatabase_uploadsZeroRecords() {
        ExportHelper.Backup backup =
                new ExportHelper.Backup(new File("backup_upload.xml"), Collections.emptyList());

        assertEquals("0", backup.getAppProperties().get(BackupMetadata.KEY_RECORD_COUNT));
    }

    // ------------------------------------------------------------ restore side

    @Test
    public void resolveRecordCount_storedCount_isUsedAsIs() {
        BackupMetadata metadata = new BackupMetadata("file-id", 1_700_000_000_000L, 45);

        assertTrue(metadata.hasRecordCount());
        assertEquals(Integer.valueOf(45), metadata.resolveRecordCount(7));
        assertEquals(Integer.valueOf(45), metadata.resolveRecordCount(null));
    }

    @Test
    public void resolveRecordCount_missingProperty_fallsBackToParsedCount() {
        BackupMetadata metadata = new BackupMetadata("file-id", 1_700_000_000_000L, null);

        assertFalse(metadata.hasRecordCount());
        assertEquals(Integer.valueOf(7), metadata.resolveRecordCount(7));
        // Nothing to show at all: the dialog uses the "unknown" label.
        assertNull(metadata.resolveRecordCount(null));
    }

    @Test
    public void parseRecordCount_validValue_isParsed() {
        assertEquals(Integer.valueOf(45), BackupMetadata.parseRecordCount(properties("45")));
        assertEquals(Integer.valueOf(45), BackupMetadata.parseRecordCount(properties(" 45 ")));
    }

    @Test
    public void parseRecordCount_blankGarbageOrNonPositive_isUnknown() {
        assertNull(BackupMetadata.parseRecordCount(null));
        assertNull(BackupMetadata.parseRecordCount(Collections.emptyMap()));
        assertNull(BackupMetadata.parseRecordCount(properties(null)));
        assertNull(BackupMetadata.parseRecordCount(properties("")));
        assertNull(BackupMetadata.parseRecordCount(properties("   ")));
        assertNull(BackupMetadata.parseRecordCount(properties("not a number")));
        assertNull(BackupMetadata.parseRecordCount(properties("0")));
        assertNull(BackupMetadata.parseRecordCount(properties("-5")));
    }

    @Test
    public void brokenRecordCount_doesNotBlockTheRestore() {
        // A file written by another client may carry a value the dialog cannot read; the restore
        // itself still runs, the count simply comes from the parsed records.
        Map<String, String> appProperties = properties("garbage");
        BackupMetadata metadata = new BackupMetadata("file-id", BackupMetadata.UNKNOWN_MODIFIED_TIME,
                BackupMetadata.parseRecordCount(appProperties));

        assertFalse(metadata.hasRecordCount());
        assertEquals(Integer.valueOf(12), metadata.resolveRecordCount(12));
    }

    private static Map<String, String> properties(String rawRecordCount) {
        Map<String, String> appProperties = new HashMap<>();
        appProperties.put(BackupMetadata.KEY_RECORD_COUNT, rawRecordCount);
        return appProperties;
    }
}
