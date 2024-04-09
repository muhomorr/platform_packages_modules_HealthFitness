/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.healthconnect.cts;

import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readAllRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static java.util.Comparator.comparing;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.TestUtils;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class SharedMemoryTest {

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    TestUtils::isHardwareSupported, "Tests should run on supported hardware only.");

    @Before
    public void before() {
        deleteAllStagedRemoteData();
    }

    @After
    public void after() {
        deleteAllStagedRemoteData();
    }

    @Test
    public void insertRecordsAndReadRecords_viaSharedMemory_recordsEqual() throws Exception {
        DataOrigin dataOrigin =
                new DataOrigin.Builder()
                        .setPackageName(getApplicationContext().getPackageName())
                        .build();

        Metadata metadata = new Metadata.Builder().setDataOrigin(dataOrigin).build();
        int recordCount = 5000;
        List<HeightRecord> records = new ArrayList<>(recordCount);
        Instant now = Instant.now();

        for (int i = 0; i < recordCount; i++) {
            records.add(
                    new HeightRecord.Builder(
                                    metadata,
                                    now.minusMillis(i),
                                    Length.fromMeters(3.0 * i / recordCount))
                            .build());
        }

        insertRecords(records);

        List<HeightRecord> readRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                                .setPageSize(records.size())
                                .build());

        assertWithMessage("Record list sizes do not match")
                .that(readRecords.size())
                .isEqualTo(recordCount);

        readRecords.sort(comparing(InstantRecord::getTime).reversed());

        for (int i = 0; i < recordCount; i++) {
            assertThat(readRecords.get(i).getHeight()).isEqualTo(records.get(i).getHeight());
        }
    }

    @Test
    public void getChangeLogs_viaSharedMemory_recordsMatch() throws Exception {
        DataOrigin dataOrigin =
                new DataOrigin.Builder()
                        .setPackageName(getApplicationContext().getPackageName())
                        .build();
        Metadata metadata = new Metadata.Builder().setDataOrigin(dataOrigin).build();

        // One less than a multiple of the default page size to allow at least one page in the
        // response to have both upserted records and deleted logs.
        int recordsToDeleteCount = 6999;

        Instant now = Instant.now();

        String changeLogToken =
                TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build()).getToken();

        List<HeightRecord> heightRecords = new ArrayList<>(recordsToDeleteCount);
        for (int i = 0; i < recordsToDeleteCount; i++) {
            heightRecords.add(
                    new HeightRecord.Builder(
                                    metadata,
                                    now.minusMillis(i),
                                    Length.fromMeters(3.0 * i / recordsToDeleteCount))
                            .build());
        }
        insertRecords(heightRecords);
        heightRecords = readAllRecords(HeightRecord.class);
        List<RecordIdFilter> recordIdFiltersToDelete =
                heightRecords.stream()
                        .map(
                                record ->
                                        RecordIdFilter.fromId(
                                                record.getClass(), record.getMetadata().getId()))
                        .collect(Collectors.toList());
        verifyDeleteRecords(recordIdFiltersToDelete);

        // One less than a multiple of the default page size to allow at least one page in the
        // response to have both upserted records and deleted logs.
        int recordsToInsertCount = 4999;

        now = Instant.now();
        List<WeightRecord> weightRecords = new ArrayList<>();
        for (int i = 0; i < recordsToInsertCount; i++) {
            weightRecords.add(
                    new WeightRecord.Builder(
                                    metadata,
                                    now.minusMillis(i),
                                    Mass.fromGrams(1000.0 * 70.0 + i * 10))
                            .build());
        }
        insertRecords(weightRecords);
        weightRecords = readAllRecords(WeightRecord.class);

        Set<String> deletedLogsIds = new HashSet<>();
        Set<String> upsertedRecordsIds = new HashSet<>();

        ChangeLogsResponse changeLogsResponse =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(changeLogToken).build());
        while (true) {
            upsertedRecordsIds.addAll(
                    changeLogsResponse.getUpsertedRecords().stream()
                            .map(record -> record.getMetadata().getId())
                            .collect(Collectors.toList()));
            deletedLogsIds.addAll(
                    changeLogsResponse.getDeletedLogs().stream()
                            .map(DeletedLog::getDeletedRecordId)
                            .collect(Collectors.toList()));
            if (!changeLogsResponse.hasMorePages()) {
                break;
            }
            changeLogToken = changeLogsResponse.getNextChangesToken();
            changeLogsResponse =
                    TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(changeLogToken).build());
        }

        assertWithMessage("Upserted records count does not match")
                .that(upsertedRecordsIds.size())
                .isEqualTo(recordsToInsertCount);
        assertWithMessage("Deleted logs count does not match")
                .that(deletedLogsIds.size())
                .isEqualTo(recordsToDeleteCount);

        for (int i = 0; i < recordsToInsertCount; i++) {
            String recordId = weightRecords.get(i).getMetadata().getId();
            assertWithMessage("Missing upserted record at index %s with id %s", i, recordId)
                    .that(upsertedRecordsIds)
                    .contains(recordId);
        }

        for (int i = 0; i < recordsToDeleteCount; i++) {
            String recordId = recordIdFiltersToDelete.get(i).getId();
            assertWithMessage("Missing deleted log at index %s with id %s", i, recordId)
                    .that(deletedLogsIds)
                    .contains(recordId);
        }
    }
}
