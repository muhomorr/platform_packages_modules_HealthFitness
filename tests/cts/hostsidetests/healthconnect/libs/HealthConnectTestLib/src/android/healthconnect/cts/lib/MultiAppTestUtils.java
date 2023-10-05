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

package android.healthconnect.cts.lib;

import android.health.connect.datatypes.DataOrigin;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Bundle;

import com.android.cts.install.lib.TestApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiAppTestUtils {
    public static final String QUERY_TYPE = "android.healthconnect.cts.queryType";
    public static final String INTENT_EXTRA_CALLING_PKG = "android.healthconnect.cts.calling_pkg";
    public static final String APP_PKG_NAME_USED_IN_DATA_ORIGIN =
            "android.healthconnect.cts.pkg.usedInDataOrigin";
    public static final String INSERT_RECORD_QUERY = "android.healthconnect.cts.insertRecord";
    public static final String READ_RECORDS_QUERY = "android.healthconnect.cts.readRecords";
    public static final String READ_RECORDS_SIZE = "android.healthconnect.cts.readRecordsNumber";
    public static final String READ_USING_DATA_ORIGIN_FILTERS =
            "android.healthconnect.cts.readUsingDataOriginFilters";

    public static final String DATA_ORIGIN_FILTER_PACKAGE_NAMES =
            "android.healthconnect.cts.dataOriginFilterPackageNames";
    public static final String READ_RECORD_CLASS_NAME =
            "android.healthconnect.cts.readRecordsClass";
    public static final String READ_CHANGE_LOGS_QUERY = "android.healthconnect.cts.readChangeLogs";
    public static final String CHANGE_LOGS_RESPONSE =
            "android.healthconnect.cts.changeLogsResponse";
    public static final String CHANGE_LOG_TOKEN = "android.healthconnect.cts.changeLogToken";
    public static final String SUCCESS = "android.healthconnect.cts.success";
    public static final String CLIENT_ID = "android.healthconnect.cts.clientId";
    public static final String RECORD_IDS = "android.healthconnect.cts.records";
    public static final String DELETE_RECORDS_QUERY = "android.healthconnect.cts.deleteRecords";
    public static final String UPDATE_RECORDS_QUERY = "android.healthconnect.cts.updateRecords";
    public static final String UPDATE_EXERCISE_ROUTE = "android.healthconnect.cts.updateRoute";

    public static final String UPSERT_EXERCISE_ROUTE = "android.healthconnect.cts.upsertRoute";
    public static final String GET_CHANGE_LOG_TOKEN_QUERY =
            "android.healthconnect.cts.getChangeLogToken";
    public static final String RECORD_TYPE = "android.healthconnect.cts.recordType";
    public static final String STEPS_RECORD = "android.healthconnect.cts.stepsRecord";
    public static final String EXERCISE_SESSION = "android.healthconnect.cts.exerciseSession";
    public static final String START_TIME = "android.healthconnect.cts.startTime";
    public static final String END_TIME = "android.healthconnect.cts.endTime";
    public static final String STEPS_COUNT = "android.healthconnect.cts.stepsCount";
    public static final String PAUSE_START = "android.healthconnect.cts.pauseStart";
    public static final String PAUSE_END = "android.healthconnect.cts.pauseEnd";
    public static final String INTENT_EXCEPTION = "android.healthconnect.cts.exception";

    public static Bundle insertRecordAs(TestApp testApp) throws Exception {
        return TestAppProxy.forApp(testApp).insertRecord();
    }

    public static Bundle deleteRecordsAs(
            TestApp testApp, List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass)
            throws Exception {
        return TestAppProxy.forApp(testApp).deleteRecords(listOfRecordIdsAndClass);
    }

    public static Bundle updateRecordsAs(
            TestApp testAppToUpdateData,
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass)
            throws Exception {
        return TestAppProxy.forApp(testAppToUpdateData).updateRecords(listOfRecordIdsAndClass);
    }

    public static Bundle updateRouteAs(TestApp testAppToUpdateData) throws Exception {
        return TestAppProxy.forApp(testAppToUpdateData).updateRoute();
    }

    public static Bundle insertSessionNoRouteAs(TestApp testAppToUpdateData) throws Exception {
        return TestAppProxy.forApp(testAppToUpdateData).insertSessionNoRoute();
    }

    public static Bundle insertRecordWithAnotherAppPackageName(
            TestApp testAppToInsertData, TestApp testAppPkgNameUsed) throws Exception {
        return TestAppProxy.forApp(testAppToInsertData)
                .insertRecordWithAnotherAppPackageName(testAppPkgNameUsed);
    }

    public static Bundle readRecordsAs(TestApp testApp, ArrayList<String> recordClassesToRead)
            throws Exception {
        return TestAppProxy.forApp(testApp).readRecords(recordClassesToRead);
    }

    public static Bundle readRecordsAs(
            TestApp testApp,
            ArrayList<String> recordClassesToRead,
            Optional<List<String>> dataOriginFilterPackageNames)
            throws Exception {
        return TestAppProxy.forApp(testApp)
                .readRecords(recordClassesToRead, dataOriginFilterPackageNames);
    }

    public static Bundle insertRecordWithGivenClientId(TestApp testApp, double clientId)
            throws Exception {
        return TestAppProxy.forApp(testApp).insertRecordWithGivenClientId(clientId);
    }

    public static Bundle readRecordsUsingDataOriginFiltersAs(
            TestApp testApp, ArrayList<String> recordClassesToRead) throws Exception {
        return TestAppProxy.forApp(testApp).readRecordsUsingDataOriginFilters(recordClassesToRead);
    }

    public static Bundle readChangeLogsUsingDataOriginFiltersAs(
            TestApp testApp, String changeLogToken) throws Exception {
        return TestAppProxy.forApp(testApp).readChangeLogsUsingDataOriginFilters(changeLogToken);
    }

    public static Bundle getChangeLogTokenAs(
            TestApp testApp, String pkgName, ArrayList<String> recordClassesToRead)
            throws Exception {
        return TestAppProxy.forApp(testApp).getChangeLogToken(pkgName, recordClassesToRead);
    }

    public static Bundle insertStepsRecordAs(
            TestApp testApp, String startTime, String endTime, int stepsCount) throws Exception {
        return TestAppProxy.forApp(testApp).insertStepsRecord(startTime, endTime, stepsCount);
    }

    public static Bundle insertExerciseSessionAs(
            TestApp testApp,
            String sessionStartTime,
            String sessionEndTime,
            String pauseStart,
            String pauseEnd)
            throws Exception {
        return TestAppProxy.forApp(testApp)
                .insertExerciseSession(sessionStartTime, sessionEndTime, pauseStart, pauseEnd);
    }

    public static List<DataOrigin> getDataOriginPriorityOrder(TestApp testAppA, TestApp testAppB) {
        return List.of(
                new DataOrigin.Builder().setPackageName(testAppA.getPackageName()).build(),
                new DataOrigin.Builder().setPackageName(testAppB.getPackageName()).build());
    }
}
