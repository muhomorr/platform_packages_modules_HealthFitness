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

import static android.healthconnect.cts.lib.MultiAppTestUtils.APP_PKG_NAME_USED_IN_DATA_ORIGIN;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CHANGE_LOG_TOKEN;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CLIENT_ID;
import static android.healthconnect.cts.lib.MultiAppTestUtils.DATA_ORIGIN_FILTER_PACKAGE_NAMES;
import static android.healthconnect.cts.lib.MultiAppTestUtils.DELETE_RECORDS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.END_TIME;
import static android.healthconnect.cts.lib.MultiAppTestUtils.EXERCISE_SESSION;
import static android.healthconnect.cts.lib.MultiAppTestUtils.GET_CHANGE_LOG_TOKEN_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.INSERT_RECORD_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.INTENT_EXCEPTION;
import static android.healthconnect.cts.lib.MultiAppTestUtils.INTENT_EXTRA_CALLING_PKG;
import static android.healthconnect.cts.lib.MultiAppTestUtils.PAUSE_END;
import static android.healthconnect.cts.lib.MultiAppTestUtils.PAUSE_START;
import static android.healthconnect.cts.lib.MultiAppTestUtils.QUERY_TYPE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_CHANGE_LOGS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORDS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORD_CLASS_NAME;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_USING_DATA_ORIGIN_FILTERS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.RECORD_IDS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.RECORD_TYPE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.START_TIME;
import static android.healthconnect.cts.lib.MultiAppTestUtils.STEPS_COUNT;
import static android.healthconnect.cts.lib.MultiAppTestUtils.STEPS_RECORD;
import static android.healthconnect.cts.lib.MultiAppTestUtils.UPDATE_EXERCISE_ROUTE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.UPDATE_RECORDS_QUERY;
import static android.healthconnect.cts.lib.MultiAppTestUtils.UPSERT_EXERCISE_ROUTE;

import static androidx.test.InstrumentationRegistry.getContext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.health.connect.datatypes.Record;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Bundle;

import com.android.cts.install.lib.TestApp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Performs API calls to HC on behalf of test apps. */
public class TestAppProxy {
    private static final String TEST_APP_RECEIVER_CLASS_NAME =
            "android.healthconnect.cts.testhelper.TestAppReceiver";
    private static final long POLLING_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20);

    private final String mPackageName;
    private final boolean mInBackground;

    private TestAppProxy(String packageName, boolean inBackground) {
        mPackageName = packageName;
        mInBackground = inBackground;
    }

    /** Create a new {@link TestAppProxy} for given test app. */
    public static TestAppProxy forApp(TestApp testApp) {
        return forPackageName(testApp.getPackageName());
    }

    /** Create a new {@link TestAppProxy} for given package name. */
    public static TestAppProxy forPackageName(String packageName) {
        return new TestAppProxy(packageName, false);
    }

    /**
     * Create a new {@link TestAppProxy} for given package name which performs calls in the
     * background.
     */
    public static TestAppProxy forPackageNameInBackground(String packageName) {
        return new TestAppProxy(packageName, true);
    }

    /** Insert a few hardcoded records on behalf of the app. */
    public Bundle insertRecord() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);

        return getFromTestApp(bundle);
    }

    /** Delete records on behalf of the app. */
    public Bundle deleteRecords(List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_RECORDS_QUERY);
        bundle.putSerializable(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);

        return getFromTestApp(bundle);
    }

    /** Update records on behalf of the app. */
    public Bundle updateRecords(List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPDATE_RECORDS_QUERY);
        bundle.putSerializable(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);

        return getFromTestApp(bundle);
    }

    /** Update exercise route on behalf of the app. */
    public Bundle updateRoute() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPDATE_EXERCISE_ROUTE);
        return getFromTestApp(bundle);
    }

    /** Insert an exercise session with a route on behalf of the app. */
    public Bundle insertSessionNoRoute() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPSERT_EXERCISE_ROUTE);
        return getFromTestApp(bundle);
    }

    /** Insert a record with data origin set to given other app on behalf of the app. */
    public Bundle insertRecordWithAnotherAppPackageName(TestApp testAppPkgNameUsed)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putString(APP_PKG_NAME_USED_IN_DATA_ORIGIN, testAppPkgNameUsed.getPackageName());

        return getFromTestApp(bundle);
    }

    /** Read records on behalf of the app. */
    public Bundle readRecords(ArrayList<String> recordClassesToRead) throws Exception {
        return readRecords(
                recordClassesToRead, /* dataOriginFilterPackageNames= */ Optional.empty());
    }

    /** Read records on behalf of the app. */
    public Bundle readRecords(Class<? extends Record> recordClassToRead) throws Exception {
        return readRecords(
                new ArrayList<>(List.of(recordClassToRead.getName())),
                /* dataOriginFilterPackageNames= */ Optional.empty());
    }

    /** Read records on behalf of the app. */
    public Bundle readRecords(
            ArrayList<String> recordClassesToRead,
            Optional<List<String>> dataOriginFilterPackageNames)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_QUERY);
        bundle.putStringArrayList(READ_RECORD_CLASS_NAME, recordClassesToRead);
        if (!dataOriginFilterPackageNames.isEmpty()) {
            ArrayList<String> dataOrigins = new ArrayList<>();
            dataOrigins.addAll(dataOriginFilterPackageNames.get());
            bundle.putBoolean(READ_USING_DATA_ORIGIN_FILTERS, true);
            bundle.putStringArrayList(DATA_ORIGIN_FILTER_PACKAGE_NAMES, dataOrigins);
        }
        return getFromTestApp(bundle);
    }

    /** Insert a record with a given client id on behalf of the app. */
    public Bundle insertRecordWithGivenClientId(double clientId) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putDouble(CLIENT_ID, clientId);

        return getFromTestApp(bundle);
    }

    /** Read records using data origin filters on behalf of the app. */
    public Bundle readRecordsUsingDataOriginFilters(ArrayList<String> recordClassesToRead)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_QUERY);
        bundle.putStringArrayList(READ_RECORD_CLASS_NAME, recordClassesToRead);
        bundle.putBoolean(READ_USING_DATA_ORIGIN_FILTERS, true);

        return getFromTestApp(bundle);
    }

    /** Read changelogs using data origin filters on behalf of the app. */
    public Bundle readChangeLogsUsingDataOriginFilters(String changeLogToken) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_CHANGE_LOGS_QUERY);
        bundle.putString(CHANGE_LOG_TOKEN, changeLogToken);
        bundle.putBoolean(READ_USING_DATA_ORIGIN_FILTERS, true);

        return getFromTestApp(bundle);
    }

    /** Get a changelogs token on behalf of the app. */
    public Bundle getChangeLogToken(String pkgName, ArrayList<String> recordClassesToRead)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, GET_CHANGE_LOG_TOKEN_QUERY);
        bundle.putString(APP_PKG_NAME_USED_IN_DATA_ORIGIN, pkgName);

        if (recordClassesToRead != null) {
            bundle.putStringArrayList(READ_RECORD_CLASS_NAME, recordClassesToRead);
        }
        return getFromTestApp(bundle);
    }

    /** Insert a steps record on behalf of the app. */
    public Bundle insertStepsRecord(String startTime, String endTime, int stepsCount)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putString(RECORD_TYPE, STEPS_RECORD);
        bundle.putString(START_TIME, startTime);
        bundle.putString(END_TIME, endTime);
        bundle.putInt(STEPS_COUNT, stepsCount);

        return getFromTestApp(bundle);
    }

    /** Insert an exercise session on behalf of the app. */
    public Bundle insertExerciseSession(
            String sessionStartTime, String sessionEndTime, String pauseStart, String pauseEnd)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putString(RECORD_TYPE, EXERCISE_SESSION);
        bundle.putString(START_TIME, sessionStartTime);
        bundle.putString(END_TIME, sessionEndTime);
        bundle.putString(PAUSE_START, pauseStart);
        bundle.putString(PAUSE_END, pauseEnd);

        return getFromTestApp(bundle);
    }

    private Bundle getFromTestApp(Bundle bundleToCreateIntent) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Bundle> response = new AtomicReference<>();
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
        BroadcastReceiver broadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.hasExtra(INTENT_EXCEPTION)) {
                            exceptionAtomicReference.set(
                                    (Exception) (intent.getSerializableExtra(INTENT_EXCEPTION)));
                        } else {
                            response.set(intent.getExtras());
                        }
                        latch.countDown();
                    }
                };

        launchTestApp(bundleToCreateIntent, broadcastReceiver, latch);
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
        return response.get();
    }

    private void launchTestApp(
            Bundle bundleToCreateIntent, BroadcastReceiver broadcastReceiver, CountDownLatch latch)
            throws Exception {

        // Register broadcast receiver
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(bundleToCreateIntent.getString(QUERY_TYPE));
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        // Launch the test app.
        Intent intent;

        if (mInBackground) {
            intent = new Intent().setClassName(mPackageName, TEST_APP_RECEIVER_CLASS_NAME);
        } else {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage(mPackageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }

        intent.putExtra(INTENT_EXTRA_CALLING_PKG, getContext().getPackageName());
        intent.putExtras(bundleToCreateIntent);

        Thread.sleep(500);

        if (mInBackground) {
            getContext().sendBroadcast(intent);
        } else {
            getContext().startActivity(intent);
        }

        if (!latch.await(POLLING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final String errorMessage =
                    "Timed out while waiting to receive "
                            + bundleToCreateIntent.getString(QUERY_TYPE)
                            + " intent from "
                            + mPackageName;
            throw new TimeoutException(errorMessage);
        }
        getContext().unregisterReceiver(broadcastReceiver);
    }
}
