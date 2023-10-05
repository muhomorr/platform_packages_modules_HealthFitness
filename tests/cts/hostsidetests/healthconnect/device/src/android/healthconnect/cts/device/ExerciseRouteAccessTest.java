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

package android.healthconnect.cts.device;

import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;
import static android.healthconnect.cts.utils.TestUtils.READ_EXERCISE_ROUTE_PERMISSION;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.deleteTestData;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogToken;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogs;
import static android.healthconnect.cts.utils.TestUtils.getEmptyMetadata;
import static android.healthconnect.cts.utils.TestUtils.getExerciseRoute;
import static android.healthconnect.cts.utils.TestUtils.getLocation;
import static android.healthconnect.cts.utils.TestUtils.getMetadataForClientId;
import static android.healthconnect.cts.utils.TestUtils.getMetadataForId;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.android.compatibility.common.util.FeatureUtil.AUTOMOTIVE_FEATURE;
import static com.android.compatibility.common.util.FeatureUtil.hasSystemFeature;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.time.Duration.ofMinutes;

import android.app.UiAutomation;
import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.healthconnect.cts.lib.TestAppProxy;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExerciseRouteAccessTest {

    private static final TestAppProxy APP_A_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A");
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Instant START_TIME = NOW.minus(ofMinutes(30));
    private static final Instant END_TIME = NOW;
    private UiAutomation mAutomation;

    @Before
    public void setUp() {
        mAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        Assume.assumeFalse(hasSystemFeature(AUTOMOTIVE_FEATURE));

        mAutomation.grantRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteTestData();
        deleteAllStagedRemoteData();

        mAutomation.grantRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
    }

    @Test
    public void readRecords_usingFilters_cannotAccessOtherAppRoute() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(sessionWithRoute);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void readRecords_usingFilters_withReadExerciseRoutePermission_canAccessOtherAppRoute()
            throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(sessionWithRoute);
        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNotNull();
    }

    @Test
    public void readRecords_usingFilters_canAccessOwnRoute() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        insertRecords(List.of(sessionWithRoute), getApplicationContext());

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void readRecords_usingFilters_mixedOwnAndOtherAppSession() throws Exception {
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String otherAppSessionId =
                APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession).get(0);
        ExerciseSessionRecord ownSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String ownSessionId =
                insertRecords(List.of(ownSession), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        Map<String, ExerciseSessionRecord> idToRecordMap =
                records.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getMetadata().getId(),
                                        Function.identity()));
        assertThat(records).isNotNull();
        assertThat(records).hasSize(2);
        assertThat(idToRecordMap.get(otherAppSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(otherAppSessionId).getRoute()).isNull();
        assertThat(idToRecordMap.get(ownSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(ownSessionId).getRoute()).isEqualTo(ownSession.getRoute());
    }

    @Test
    public void readRecords_usingIds_cannotAccessOtherAppRoute() throws Exception {
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String sessionId = APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession).get(0);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void readRecords_usingIds_withReadExerciseRoutePermission_canAccessOtherAppRoute()
            throws Exception {
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String sessionId = APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession).get(0);
        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(otherAppSession.getRoute());
    }

    @Test
    public void readRecords_usingIds_canAccessOwnRoute() throws Exception {
        ExerciseSessionRecord record = getExerciseSessionWithRoute(getEmptyMetadata());
        String sessionId =
                insertRecords(List.of(record), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(record.getRoute());
    }

    @Test
    public void readRecords_usingIds_mixedOwnAndOtherAppSession() throws Exception {
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String otherAppSessionId =
                APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession).get(0);
        ExerciseSessionRecord ownSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String ownSessionId =
                insertRecords(List.of(ownSession), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(otherAppSessionId)
                                .addId(ownSessionId)
                                .build());

        Map<String, ExerciseSessionRecord> idToRecordMap =
                records.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getMetadata().getId(),
                                        Function.identity()));
        assertThat(records).isNotNull();
        assertThat(records).hasSize(2);
        assertThat(idToRecordMap.get(otherAppSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(otherAppSessionId).getRoute()).isNull();
        assertThat(idToRecordMap.get(ownSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(ownSessionId).getRoute()).isEqualTo(ownSession.getRoute());
    }

    @Test
    public void getChangelogs_cannotAccessOtherAppRoute() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();

        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(List.of(otherAppSession));
        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void getChangelogs_withReadExerciseRoutePermission_canAccessOtherAppRoute()
            throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession);
        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(otherAppSession.getRoute());
    }

    @Test
    public void getChangelogs_canAccessOwnRoute() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        ExerciseSessionRecord record = getExerciseSessionWithRoute(getEmptyMetadata());
        insertRecords(List.of(record), getApplicationContext());

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(record.getRoute());
    }

    @Test
    public void getChangelogs_mixedOwnAndOtherAppSession() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String otherAppSessionId =
                APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession).get(0);
        ExerciseSessionRecord ownSession = getExerciseSessionWithRoute(getEmptyMetadata());
        String ownSessionId =
                insertRecords(List.of(ownSession), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        Map<String, ExerciseSessionRecord> idToRecordMap =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .collect(
                                Collectors.toMap(
                                        record -> record.getMetadata().getId(),
                                        Function.identity()));
        assertThat(response.getUpsertedRecords()).isNotNull();
        assertThat(response.getUpsertedRecords()).hasSize(2);
        assertThat(idToRecordMap.get(otherAppSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(otherAppSessionId).getRoute()).isNull();
        assertThat(idToRecordMap.get(ownSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(ownSessionId).getRoute()).isEqualTo(ownSession.getRoute());
    }

    @Test
    public void testRouteInsert_cannotInsertRouteWithoutPerm() throws Exception {
        mAutomation.revokeRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> APP_A_WITH_READ_WRITE_PERMS.insertRecords(otherAppSession));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testRouteUpdate_updateRouteWithPerm_noRouteAfterUpdate() throws Exception {
        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isEmpty();
        ExerciseSessionRecord exerciseRecord = getExerciseSessionWithRoute(getEmptyMetadata());
        String exerciseRecordId = APP_A_WITH_READ_WRITE_PERMS.insertRecords(exerciseRecord).get(0);
        records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        ExerciseSessionRecord updatedSessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForId(exerciseRecordId));

        APP_A_WITH_READ_WRITE_PERMS.updateRecords(updatedSessionWithoutRoute);

        records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();
        assertThat(records.get(0).getRoute()).isNull();
        // Check that the route has been actually deleted, so no exceptions from incorrect record
        // state.
        records =
                APP_A_WITH_READ_WRITE_PERMS.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void testRouteUpdate_updateRouteWithoutPerm_hasRouteAfterUpdate() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        String otherAppSessionId =
                APP_A_WITH_READ_WRITE_PERMS.insertRecords(sessionWithRoute).get(0);
        mAutomation.revokeRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
        ExerciseSessionRecord updatedSessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForId(otherAppSessionId));

        APP_A_WITH_READ_WRITE_PERMS.updateRecords(updatedSessionWithoutRoute);

        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);
        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void testRouteUpsert_insertRecordNoRouteWithoutRoutePerm_hasRouteAfterInsert()
            throws Exception {
        ExerciseSessionRecord sessionWithRoute =
                getExerciseSessionWithRoute(getMetadataForClientId("client id"));
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(sessionWithRoute);
        mAutomation.revokeRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
        ExerciseSessionRecord updatedSessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForClientId("client id"));

        APP_A_WITH_READ_WRITE_PERMS.insertRecords(updatedSessionWithoutRoute);

        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);
        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void testRouteUpsert_insertRecordNoRouteWithRoutePerm_noRouteAfterInsert()
            throws Exception {
        ExerciseSessionRecord sessionWithRoute =
                getExerciseSessionWithRoute(getMetadataForClientId("client id"));
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(sessionWithRoute);
        ExerciseSessionRecord updatedSessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForClientId("client id"));

        APP_A_WITH_READ_WRITE_PERMS.insertRecords(updatedSessionWithoutRoute);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();
        assertThat(records.get(0).getRoute()).isNull();
    }

    private static ExerciseSessionRecord getExerciseSessionWithRoute(Metadata metadata) {
        return getExerciseSessionRecordBuilder(metadata)
                .setRoute(
                        getExerciseRoute(
                                getLocation(START_TIME, 52., 48.),
                                getLocation(START_TIME.plusSeconds(2), 51., 49.)))
                .build();
    }

    private static ExerciseSessionRecord getExerciseSessionWithoutRoute(Metadata metadata) {
        return getExerciseSessionRecordBuilder(metadata).build();
    }

    private static ExerciseSessionRecord.Builder getExerciseSessionRecordBuilder(
            Metadata metadata) {
        return new ExerciseSessionRecord.Builder(
                metadata, START_TIME, END_TIME, ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING);
    }
}
