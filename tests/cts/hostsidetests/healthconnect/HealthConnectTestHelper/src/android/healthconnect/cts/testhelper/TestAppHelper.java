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

package android.healthconnect.cts.testhelper;

import static android.healthconnect.cts.lib.MultiAppTestUtils.APP_PKG_NAME_USED_IN_DATA_ORIGIN;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CHANGE_LOGS_RESPONSE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CHANGE_LOG_TOKEN;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CLIENT_ID;
import static android.healthconnect.cts.lib.MultiAppTestUtils.DATA_ORIGIN_FILTER_PACKAGE_NAMES;
import static android.healthconnect.cts.lib.MultiAppTestUtils.DELETE_RECORDS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.END_TIME;
import static android.healthconnect.cts.lib.MultiAppTestUtils.EXERCISE_SESSION;
import static android.healthconnect.cts.lib.MultiAppTestUtils.GET_CHANGE_LOG_TOKEN_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.INSERT_RECORD_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.INTENT_EXCEPTION;
import static android.healthconnect.cts.lib.MultiAppTestUtils.PAUSE_END;
import static android.healthconnect.cts.lib.MultiAppTestUtils.PAUSE_START;
import static android.healthconnect.cts.lib.MultiAppTestUtils.QUERY_TYPE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_CHANGE_LOGS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORDS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORDS_SIZE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORD_CLASS_NAME;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_USING_DATA_ORIGIN_FILTERS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.RECORD_IDS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.RECORD_TYPE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.START_TIME;
import static android.healthconnect.cts.lib.MultiAppTestUtils.STEPS_COUNT;
import static android.healthconnect.cts.lib.MultiAppTestUtils.STEPS_RECORD;
import static android.healthconnect.cts.lib.MultiAppTestUtils.SUCCESS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.UPDATE_EXERCISE_ROUTE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.UPDATE_RECORDS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.UPSERT_EXERCISE_ROUTE;
import static android.healthconnect.cts.utils.TestUtils.buildExerciseSession;
import static android.healthconnect.cts.utils.TestUtils.buildStepsRecord;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogs;
import static android.healthconnect.cts.utils.TestUtils.getExerciseSessionRecord;
import static android.healthconnect.cts.utils.TestUtils.getTestRecords;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.insertRecordsAndGetIds;
import static android.healthconnect.cts.utils.TestUtils.readRecords;
import static android.healthconnect.cts.utils.TestUtils.updateRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import android.content.Context;
import android.content.Intent;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class TestAppHelper {

    static Intent handleRequest(Context context, Bundle bundle) {
        String queryType = bundle.getString(QUERY_TYPE);
        Intent returnIntent;
        try {
            switch (queryType) {
                case INSERT_RECORD_QUERY:
                    if (bundle.containsKey(APP_PKG_NAME_USED_IN_DATA_ORIGIN)) {
                        returnIntent =
                                insertRecordsWithDifferentPkgName(
                                        queryType,
                                        bundle.getString(APP_PKG_NAME_USED_IN_DATA_ORIGIN),
                                        context);
                        break;
                    }
                    if (bundle.containsKey(CLIENT_ID)) {
                        returnIntent =
                                insertRecordsWithGivenClientId(
                                        queryType, bundle.getDouble(CLIENT_ID), context);
                        break;
                    }
                    if (bundle.containsKey(RECORD_TYPE)) {
                        if (bundle.getString(RECORD_TYPE).equals(STEPS_RECORD)) {
                            returnIntent =
                                    insertStepsRecord(
                                            queryType,
                                            bundle.getString(START_TIME),
                                            bundle.getString(END_TIME),
                                            bundle.getInt(STEPS_COUNT),
                                            context);
                            break;
                        } else if (bundle.getString(RECORD_TYPE).equals(EXERCISE_SESSION)) {
                            returnIntent =
                                    insertExerciseSession(
                                            queryType,
                                            bundle.getString(START_TIME),
                                            bundle.getString(END_TIME),
                                            bundle.getString(PAUSE_START),
                                            bundle.getString(PAUSE_END),
                                            context);
                            break;
                        }
                    }
                    returnIntent = insertRecord(queryType, context);
                    break;
                case DELETE_RECORDS_QUERY:
                    returnIntent =
                            deleteRecords(
                                    queryType,
                                    (List<TestUtils.RecordTypeAndRecordIds>)
                                            bundle.getSerializable(RECORD_IDS),
                                    context);
                    break;
                case UPDATE_EXERCISE_ROUTE:
                    returnIntent = updateRoute(queryType, context);
                    break;
                case UPSERT_EXERCISE_ROUTE:
                    returnIntent = upsertRoute(queryType, context);
                    break;
                case UPDATE_RECORDS_QUERY:
                    returnIntent =
                            updateRecords(
                                    queryType,
                                    (List<TestUtils.RecordTypeAndRecordIds>)
                                            bundle.getSerializable(RECORD_IDS),
                                    context);
                    break;
                case READ_RECORDS_QUERY:
                    if (bundle.containsKey(READ_USING_DATA_ORIGIN_FILTERS)) {
                        List<String> dataOriginPackageNames =
                                bundle.containsKey(DATA_ORIGIN_FILTER_PACKAGE_NAMES)
                                        ?
                                        // if a set of data origin filters is specified, use that
                                        bundle.getStringArrayList(DATA_ORIGIN_FILTER_PACKAGE_NAMES)
                                        :
                                        // otherwise default to this app's package name
                                        List.of(context.getPackageName());
                        returnIntent =
                                readRecordsUsingDataOriginFilters(
                                        queryType,
                                        bundle.getStringArrayList(READ_RECORD_CLASS_NAME),
                                        dataOriginPackageNames,
                                        context);
                        break;
                    }
                    returnIntent =
                            readRecords(
                                    queryType,
                                    bundle.getStringArrayList(READ_RECORD_CLASS_NAME),
                                    context);
                    break;
                case READ_CHANGE_LOGS_QUERY:
                    returnIntent =
                            readChangeLogsUsingDataOriginFilters(
                                    queryType, bundle.getString(CHANGE_LOG_TOKEN), context);
                    break;
                case GET_CHANGE_LOG_TOKEN_QUERY:
                    if (bundle.containsKey(READ_RECORD_CLASS_NAME)) {
                        returnIntent =
                                getChangeLogToken(
                                        queryType,
                                        bundle.getString(APP_PKG_NAME_USED_IN_DATA_ORIGIN),
                                        bundle.getStringArrayList(READ_RECORD_CLASS_NAME),
                                        context);
                        break;
                    }
                    returnIntent =
                            getChangeLogToken(
                                    queryType,
                                    bundle.getString(APP_PKG_NAME_USED_IN_DATA_ORIGIN),
                                    context);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unknown query received from launcher app: " + queryType);
            }
        } catch (Exception e) {
            returnIntent = new Intent(queryType);
            returnIntent.putExtra(INTENT_EXCEPTION, e);
        }

        return returnIntent;
    }

    /**
     * Method to get test records, insert them, and put the list of recordId and recordClass in the
     * intent
     *
     * @param queryType - specifies the action, here it should be INSERT_RECORDS_QUERY
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent insertRecord(String queryType, Context context) {
        List<Record> records = getTestRecords(context.getPackageName());
        final Intent intent = new Intent(queryType);
        try {
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                    insertRecordsAndGetIds(records, context);
            intent.putExtra(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(SUCCESS, false);
            intent.putExtra(INTENT_EXCEPTION, e);
        }

        return intent;
    }

    /**
     * Method to delete the records and put the Exception in the intent if deleting records throws
     * an exception
     *
     * @param queryType - specifies the action, here it should be DELETE_RECORDS_QUERY
     * @param listOfRecordIdsAndClassName - list of recordId and recordClass of records to be
     *     deleted
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     * @throws ClassNotFoundException if a record category class is not found for any class name
     *     present in the list @listOfRecordIdsAndClassName
     */
    private static Intent deleteRecords(
            String queryType,
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClassName,
            Context context)
            throws ClassNotFoundException {
        final Intent intent = new Intent(queryType);

        List<RecordIdFilter> recordIdFilters = new ArrayList<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds :
                listOfRecordIdsAndClassName) {
            for (String recordId : recordTypeAndRecordIds.getRecordIds()) {
                recordIdFilters.add(
                        RecordIdFilter.fromId(
                                (Class<? extends Record>)
                                        Class.forName(recordTypeAndRecordIds.getRecordType()),
                                recordId));
            }
        }
        try {
            verifyDeleteRecords(recordIdFilters, context);
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
        }
        return intent;
    }

    /**
     * Method to update the records and put the exception in the intent if updating the records
     * throws an exception
     *
     * @param queryType - specifies the action, here it should be UPDATE_RECORDS_QUERY
     * @param listOfRecordIdsAndClassName - list of recordId and recordClass of records to be
     *     updated
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent updateRecords(
            String queryType,
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClassName,
            Context context) {
        final Intent intent = new Intent(queryType);

        try {
            for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds :
                    listOfRecordIdsAndClassName) {
                List<? extends Record> recordsToBeUpdated =
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(
                                                (Class<? extends Record>)
                                                        Class.forName(
                                                                recordTypeAndRecordIds
                                                                        .getRecordType()))
                                        .build(),
                                context);
                TestUtils.updateRecords((List<Record>) recordsToBeUpdated, context);
            }
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
            intent.putExtra(SUCCESS, false);
        }

        return intent;
    }

    /**
     * Method to update the session record to the session without route and put the exception in the
     * intent if updating the record throws an exception
     *
     * @param queryType - specifies the action, here it should be UPDATE_RECORDS_QUERY
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent updateRoute(String queryType, Context context) {
        final Intent intent = new Intent(queryType);
        try {
            ExerciseSessionRecord existingSession =
                    TestUtils.readRecords(
                                    new ReadRecordsRequestUsingFilters.Builder<>(
                                                    ExerciseSessionRecord.class)
                                            .build(),
                                    context)
                            .get(0);
            TestUtils.updateRecords(
                    List.of(
                            getExerciseSessionRecord(
                                    context.getPackageName(),
                                    Double.parseDouble(
                                            existingSession.getMetadata().getClientRecordId()),
                                    false)),
                    context);
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
            intent.putExtra(SUCCESS, false);
        }

        return intent;
    }

    /**
     * Method to upsert the session record to the session without route and put the exception in the
     * intent if updating the record throws an exception
     *
     * @param queryType - specifies the action, here it should be UPDATE_RECORDS_QUERY
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent upsertRoute(String queryType, Context context) {
        final Intent intent = new Intent(queryType);
        try {
            ExerciseSessionRecord existingSession =
                    TestUtils.readRecords(
                                    new ReadRecordsRequestUsingFilters.Builder<>(
                                                    ExerciseSessionRecord.class)
                                            .build(),
                                    context)
                            .get(0);
            insertRecords(
                    List.of(
                            getExerciseSessionRecord(
                                    context.getPackageName(),
                                    Double.parseDouble(
                                            existingSession.getMetadata().getClientRecordId()),
                                    false)),
                    context);
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
            intent.putExtra(SUCCESS, false);
        }

        return intent;
    }

    /**
     * Method to insert records with different package name in dataOrigin of the record and add the
     * details in the intent
     *
     * @param queryType - specifies the action, here it should be INSERT_RECORDS_QUERY
     * @param pkgNameUsedInDataOrigin - package name to be added in the dataOrigin of the records
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     * @throws InterruptedException
     */
    private static Intent insertRecordsWithDifferentPkgName(
            String queryType, String pkgNameUsedInDataOrigin, Context context)
            throws InterruptedException {
        final Intent intent = new Intent(queryType);

        List<Record> recordsToBeInserted = getTestRecords(pkgNameUsedInDataOrigin);
        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                insertRecordsAndGetIds(recordsToBeInserted, context);

        intent.putExtra(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);
        return intent;
    }

    /**
     * Method to read records and put the number of records read in the intent or put the exception
     * in the intent in case reading records throws exception
     *
     * @param queryType - specifies the action, here it should be READ_RECORDS_QUERY
     * @param recordClassesToRead - List of Record Class names for the records to be read
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent readRecords(
            String queryType, ArrayList<String> recordClassesToRead, Context context) {
        final Intent intent = new Intent(queryType);
        int recordsSize = 0;
        try {
            for (String recordClass : recordClassesToRead) {
                List<? extends Record> recordsRead =
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(
                                                (Class<? extends Record>)
                                                        Class.forName(recordClass))
                                        .build(),
                                context);

                recordsSize += recordsRead.size();
            }
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
            intent.putExtra(SUCCESS, false);
        }

        intent.putExtra(READ_RECORDS_SIZE, recordsSize);

        return intent;
    }

    /**
     * Method to insert records with given clientId in their dataOrigin and put SUCCESS as true if
     * insertion is successfule or SUCCESS as false if insertion throws an exception
     *
     * @param queryType - specifies the action, here it should be INSERT_RECORDS_QUERY
     * @param clientId - clientId to be specified in the dataOrigin of the records to be inserted
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent insertRecordsWithGivenClientId(
            String queryType, double clientId, Context context) {
        final Intent intent = new Intent(queryType);

        List<Record> records = getTestRecords(context.getPackageName(), clientId);

        try {
            insertRecords(records, context);
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(SUCCESS, false);
        }

        return intent;
    }

    /**
     * Method to read records using data origin filters and add number of records read to the intent
     *
     * @param queryType - specifies the action, here it should be READ_RECORDS_QUERY
     * @param recordClassesToRead - List of Record Class names for the records to be read
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent readRecordsUsingDataOriginFilters(
            String queryType,
            ArrayList<String> recordClassesToRead,
            List<String> dataOriginPackageNames,
            Context context) {
        final Intent intent = new Intent(queryType);

        int recordsSize = 0;
        try {
            for (String recordClass : recordClassesToRead) {
                ReadRecordsRequestUsingFilters.Builder requestBuilder =
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                (Class<? extends Record>) Class.forName(recordClass));
                dataOriginPackageNames.forEach(
                        packageName ->
                                requestBuilder.addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName)
                                                .build()));
                List<? extends Record> recordsRead =
                        TestUtils.readRecords(requestBuilder.build(), context);
                recordsSize += recordsRead.size();
            }
        } catch (Exception e) {
            intent.putExtra(READ_RECORDS_SIZE, 0);
            intent.putExtra(INTENT_EXCEPTION, e);
        }

        intent.putExtra(READ_RECORDS_SIZE, recordsSize);

        return intent;
    }

    /**
     * Method to read changeLogs using dataOriginFilters and add the changeLogToken
     *
     * @param queryType - specifies the action, here it should be
     *     READ_CHANGE_LOGS_USING_DATA_ORIGIN_FILTERS_QUERY
     * @param context - application context
     * @param changeLogToken - Token corresponding to which changeLogs have to be read
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent readChangeLogsUsingDataOriginFilters(
            String queryType, String changeLogToken, Context context) {
        final Intent intent = new Intent(queryType);

        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        try {
            ChangeLogsResponse response = getChangeLogs(changeLogsRequest, context);
            intent.putExtra(CHANGE_LOGS_RESPONSE, response);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
        }

        return intent;
    }

    /**
     * Method to get changeLogToken for an app
     *
     * @param queryType - specifies the action, here it should be GET_CHANGE_LOG_TOKEN_QUERY
     * @param pkgName - pkgName of the app whose changeLogs we have to read using the returned token
     * @param context - application context
     * @return - Intent to send back to the main app which is running the tests
     */
    private static Intent getChangeLogToken(String queryType, String pkgName, Context context)
            throws Exception {
        final Intent intent = new Intent(queryType);

        ChangeLogTokenResponse tokenResponse =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder().setPackageName(pkgName).build())
                                .build(),
                        context);

        intent.putExtra(CHANGE_LOG_TOKEN, tokenResponse.getToken());
        return intent;
    }

    /**
     * Method to get changeLogToken for an app
     *
     * @param queryType - specifies the action, here it should be GET_CHANGE_LOG_TOKEN_QUERY
     * @param pkgName - pkgName of the app whose changeLogs we have to read using the returned token
     * @param recordClassesToRead - Record Classes whose changeLogs to be read using the returned
     *     token
     * @param context - application context
     * @return - Intent to send back to the main app which is running the tests
     */
    private static Intent getChangeLogToken(
            String queryType,
            String pkgName,
            ArrayList<String> recordClassesToRead,
            Context context)
            throws Exception {
        final Intent intent = new Intent(queryType);

        ChangeLogTokenRequest.Builder changeLogTokenRequestBuilder =
                new ChangeLogTokenRequest.Builder()
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName(pkgName).build());
        for (String recordClass : recordClassesToRead) {
            changeLogTokenRequestBuilder.addRecordType(
                    (Class<? extends Record>) Class.forName(recordClass));
        }
        ChangeLogTokenResponse tokenResponse =
                TestUtils.getChangeLogToken(changeLogTokenRequestBuilder.build(), context);

        intent.putExtra(CHANGE_LOG_TOKEN, tokenResponse.getToken());
        return intent;
    }

    /**
     * Method to build steps record and insert them
     *
     * @param queryType - specifies the action, here it should be INSERT_RECORDS_QUERY
     * @param startTime - start time for the steps record to build
     * @param endTime - end time for the steps record to build
     * @param stepsCount - number of steps to be added in the steps record
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent insertStepsRecord(
            String queryType, String startTime, String endTime, int stepsCount, Context context) {
        final Intent intent = new Intent(queryType);
        try {
            List<Record> recordToInsert =
                    Arrays.asList(
                            buildStepsRecord(
                                    startTime, endTime, stepsCount, context.getPackageName()));
            List<Record> insertedRecords = insertRecords(recordToInsert, context);
            List<TestUtils.RecordTypeAndRecordIds> recordTypeAndRecordIdsList =
                    new ArrayList<TestUtils.RecordTypeAndRecordIds>();
            recordTypeAndRecordIdsList.add(
                    new TestUtils.RecordTypeAndRecordIds(
                            StepsRecord.class.getName(),
                            List.of(insertedRecords.get(0).getMetadata().getId())));
            intent.putExtra(SUCCESS, true);
            intent.putExtra(RECORD_IDS, (Serializable) recordTypeAndRecordIdsList);
        } catch (Exception e) {
            intent.putExtra(SUCCESS, false);
        }
        return intent;
    }

    /**
     * Method to build Exercise Session records and insert them
     *
     * @param queryType - specifies the action, here it should be INSERT_RECORDS_QUERY
     * @param sessionStartTime - start time of the exercise session to build
     * @param sessionEndTime - end time of the exercise session t build
     * @param pauseStart - start time of the pause segment in the exercise session
     * @param pauseEnd - end time of the pause segment in the exercise session
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private static Intent insertExerciseSession(
            String queryType,
            String sessionStartTime,
            String sessionEndTime,
            String pauseStart,
            String pauseEnd,
            Context context) {
        final Intent intent = new Intent(queryType);
        try {
            List<Record> recordToInsert;
            if (pauseStart == null) {
                recordToInsert =
                        Arrays.asList(
                                buildExerciseSession(sessionStartTime, sessionEndTime, context));
            } else {
                recordToInsert =
                        Arrays.asList(
                                buildExerciseSession(
                                        sessionStartTime,
                                        sessionEndTime,
                                        pauseStart,
                                        pauseEnd,
                                        context));
            }
            insertRecords(recordToInsert, context);
            intent.putExtra(SUCCESS, true);
        } catch (Exception e) {
            intent.putExtra(SUCCESS, false);
        }
        return intent;
    }

    private TestAppHelper() {}
}
