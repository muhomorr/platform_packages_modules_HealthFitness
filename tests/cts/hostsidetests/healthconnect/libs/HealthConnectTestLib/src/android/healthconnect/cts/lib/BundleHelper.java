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

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.IntervalRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Power;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/** Converters from/to bundles for HC request, response, and record types. */
public final class BundleHelper {
    private static final String PREFIX = "android.healthconnect.cts.";
    public static final String QUERY_TYPE = PREFIX + "QUERY_TYPE";
    public static final String INSERT_RECORDS_QUERY = PREFIX + "INSERT_RECORDS_QUERY";
    public static final String READ_RECORDS_QUERY = PREFIX + "READ_RECORDS_QUERY";
    public static final String READ_RECORDS_USING_IDS_QUERY =
            PREFIX + "READ_RECORDS_USING_IDS_QUERY";
    public static final String READ_CHANGE_LOGS_QUERY = PREFIX + "READ_CHANGE_LOGS_QUERY";
    public static final String DELETE_RECORDS_QUERY = PREFIX + "DELETE_RECORDS_QUERY";
    public static final String UPDATE_RECORDS_QUERY = PREFIX + "UPDATE_RECORDS_QUERY";
    public static final String GET_CHANGE_LOG_TOKEN_QUERY = PREFIX + "GET_CHANGE_LOG_TOKEN_QUERY";
    public static final String INTENT_EXCEPTION = PREFIX + "INTENT_EXCEPTION";

    private static final String CHANGE_LOGS_RESPONSE = PREFIX + "CHANGE_LOGS_RESPONSE";
    private static final String CHANGE_LOG_TOKEN = PREFIX + "CHANGE_LOG_TOKEN";
    private static final String RECORD_CLASS_NAME = PREFIX + "RECORD_CLASS_NAME";
    private static final String START_TIME_MILLIS = PREFIX + "START_TIME_MILLIS";
    private static final String END_TIME_MILLIS = PREFIX + "END_TIME_MILLIS";
    private static final String EXERCISE_SESSION_TYPE = PREFIX + "EXERCISE_SESSION_TYPE";
    private static final String RECORD_LIST = PREFIX + "RECORD_LIST";
    private static final String PACKAGE_NAME = PREFIX + "PACKAGE_NAME";
    private static final String CLIENT_ID = PREFIX + "CLIENT_ID";
    private static final String RECORD_ID = PREFIX + "RECORD_ID";
    private static final String METADATA = PREFIX + "METADATA";
    private static final String VALUES = PREFIX + "VALUES";
    private static final String COUNT = PREFIX + "COUNT";
    private static final String SAMPLE_TIMES = PREFIX + "SAMPLE_TIMES";
    private static final String SAMPLE_VALUES = PREFIX + "SAMPLE_VALUES";
    private static final String EXERCISE_ROUTE_TIMESTAMPS = PREFIX + "EXERCISE_ROUTE_TIMESTAMPS";
    private static final String EXERCISE_ROUTE_LATITUDES = PREFIX + "EXERCISE_ROUTE_LATITUDES";
    private static final String EXERCISE_ROUTE_LONGITUDES = PREFIX + "EXERCISE_ROUTE_LONGITUDES";
    private static final String EXERCISE_ROUTE_ALTITUDES = PREFIX + "EXERCISE_ROUTE_ALTITUDES";
    private static final String EXERCISE_ROUTE_HACCS = PREFIX + "EXERCISE_ROUTE_HACCS";
    private static final String EXERCISE_ROUTE_VACCS = PREFIX + "EXERCISE_ROUTE_VACCS";
    private static final String EXERCISE_HAS_ROUTE = PREFIX + "EXERCISE_HAS_ROUTE";
    private static final String POWER_WATTS = PREFIX + "POWER_WATTS";
    private static final String TIME_INSTANT_RANGE_FILTER = PREFIX + "TIME_INSTANT_RANGE_FILTER";
    private static final String CHANGE_LOGS_REQUEST = PREFIX + "CHANGE_LOGS_REQUEST";
    private static final String CHANGE_LOG_TOKEN_REQUEST = PREFIX + "CHANGE_LOG_TOKEN_REQUEST";
    private static final String EXERCISE_SEGMENT_START_TIMES =
            PREFIX + "EXERCISE_SEGMENT_START_TIMES";
    private static final String EXERCISE_SEGMENT_END_TIMES = PREFIX + "EXERCISE_SEGMENT_END_TIMES";
    private static final String EXERCISE_SEGMENT_TYPES = PREFIX + "EXERCISE_SEGMENT_TYPES";

    /** Converts an insert records request to a bundle. */
    public static Bundle fromInsertRecordsRequest(List<Record> records) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORDS_QUERY);
        bundle.putParcelableArrayList(RECORD_LIST, new ArrayList<>(fromRecordList(records)));
        return bundle;
    }

    /** Converts a bundle to an insert records request. */
    public static List<? extends Record> toInsertRecordsRequest(Bundle bundle) {
        return toRecordList(bundle.getParcelableArrayList(RECORD_LIST, Bundle.class));
    }

    /** Converts an update records request to a bundle. */
    public static Bundle fromUpdateRecordsRequest(List<Record> records) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPDATE_RECORDS_QUERY);
        bundle.putParcelableArrayList(RECORD_LIST, new ArrayList<>(fromRecordList(records)));
        return bundle;
    }

    /** Converts a bundle to an update records request. */
    public static List<? extends Record> toUpdateRecordsRequest(Bundle bundle) {
        return toRecordList(bundle.getParcelableArrayList(RECORD_LIST, Bundle.class));
    }

    /** Converts an insert records response to a bundle. */
    public static Bundle fromInsertRecordsResponse(List<String> recordIds) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(RECORD_ID, new ArrayList<>(recordIds));
        return bundle;
    }

    /** Converts a bundle to an insert records response. */
    public static List<String> toInsertRecordsResponse(Bundle bundle) {
        return bundle.getStringArrayList(RECORD_ID);
    }

    /** Converts a ReadRecordsRequestUsingFilters to a bundle. */
    public static <T extends Record> Bundle fromReadRecordsRequestUsingFilters(
            ReadRecordsRequestUsingFilters<T> request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_QUERY);
        bundle.putString(RECORD_CLASS_NAME, request.getRecordType().getName());
        bundle.putStringArrayList(
                PACKAGE_NAME,
                new ArrayList<>(
                        request.getDataOrigins().stream()
                                .map(DataOrigin::getPackageName)
                                .toList()));

        if (request.getTimeRangeFilter() instanceof TimeInstantRangeFilter filter) {
            bundle.putBoolean(TIME_INSTANT_RANGE_FILTER, true);

            Long startTime = transformOrNull(filter.getStartTime(), Instant::toEpochMilli);
            Long endTime = transformOrNull(filter.getEndTime(), Instant::toEpochMilli);

            bundle.putSerializable(START_TIME_MILLIS, startTime);
            bundle.putSerializable(END_TIME_MILLIS, endTime);
        } else if (request.getTimeRangeFilter() != null) {
            throw new IllegalArgumentException("Unsupported time range filter");
        }

        return bundle;
    }

    /** Converts a bundle to a ReadRecordsRequestUsingFilters. */
    public static ReadRecordsRequestUsingFilters<? extends Record> toReadRecordsRequestUsingFilters(
            Bundle bundle) {
        String recordClassName = bundle.getString(RECORD_CLASS_NAME);

        Class<? extends Record> recordClass = recordClassForName(recordClassName);

        ReadRecordsRequestUsingFilters.Builder<? extends Record> request =
                new ReadRecordsRequestUsingFilters.Builder<>(recordClass);

        if (bundle.getBoolean(TIME_INSTANT_RANGE_FILTER)) {
            Long startTimeMillis = bundle.getSerializable(START_TIME_MILLIS, Long.class);
            Long endTimeMillis = bundle.getSerializable(END_TIME_MILLIS, Long.class);

            Instant startTime = transformOrNull(startTimeMillis, Instant::ofEpochMilli);
            Instant endTime = transformOrNull(endTimeMillis, Instant::ofEpochMilli);

            TimeInstantRangeFilter timeInstantRangeFilter =
                    new TimeInstantRangeFilter.Builder()
                            .setStartTime(startTime)
                            .setEndTime(endTime)
                            .build();

            request.setTimeRangeFilter(timeInstantRangeFilter);
        }
        List<String> packageNames = bundle.getStringArrayList(PACKAGE_NAME);

        if (packageNames != null) {
            for (String packageName : packageNames) {
                request.addDataOrigins(
                        new DataOrigin.Builder().setPackageName(packageName).build());
            }
        }

        return request.build();
    }

    /** Converts a ReadRecordsRequestUsingFilters to a bundle. */
    public static <T extends Record> Bundle fromReadRecordsRequestUsingIds(
            ReadRecordsRequestUsingIds<T> request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_USING_IDS_QUERY);
        bundle.putString(RECORD_CLASS_NAME, request.getRecordType().getName());

        var recordIdFilters = request.getRecordIdFilters();
        List<String> recordIds = recordIdFilters.stream().map(RecordIdFilter::getId).toList();
        bundle.putStringArrayList(RECORD_ID, new ArrayList<>(recordIds));

        return bundle;
    }

    /** Converts a bundle to a ReadRecordsRequestUsingFilters. */
    public static ReadRecordsRequestUsingIds<? extends Record> toReadRecordsRequestUsingIds(
            Bundle bundle) {
        String recordClassName = bundle.getString(RECORD_CLASS_NAME);
        var request = new ReadRecordsRequestUsingIds.Builder<>(recordClassForName(recordClassName));
        var recordIds = bundle.getStringArrayList(RECORD_ID);
        for (String id : recordIds) {
            request.addId(id);
        }

        return request.build();
    }

    /** Converts a read records response to a bundle. */
    public static Bundle fromReadRecordsResponse(List<? extends Record> records) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(RECORD_LIST, new ArrayList<>(fromRecordList(records)));
        return bundle;
    }

    /** Converts a bundle to a read records response. */
    public static <T extends Record> List<T> toReadRecordsResponse(Bundle bundle) {
        return (List<T>) toRecordList(bundle.getParcelableArrayList(RECORD_LIST, Bundle.class));
    }

    /** Converts a delete records request to a bundle. */
    public static Bundle fromDeleteRecordsByIdsRequest(List<RecordIdFilter> recordIdFilters) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_RECORDS_QUERY);

        List<String> recordClassNames =
                recordIdFilters.stream()
                        .map(RecordIdFilter::getRecordType)
                        .map(Class::getName)
                        .toList();
        List<String> recordIds = recordIdFilters.stream().map(RecordIdFilter::getId).toList();

        bundle.putStringArrayList(RECORD_CLASS_NAME, new ArrayList<>(recordClassNames));
        bundle.putStringArrayList(RECORD_ID, new ArrayList<>(recordIds));

        return bundle;
    }

    /** Converts a bundle to a delete records request. */
    public static List<RecordIdFilter> toDeleteRecordsByIdsRequest(Bundle bundle) {
        List<String> recordClassNames = bundle.getStringArrayList(RECORD_CLASS_NAME);
        List<String> recordIds = bundle.getStringArrayList(RECORD_ID);

        return IntStream.range(0, recordClassNames.size())
                .mapToObj(
                        i -> {
                            String recordClassName = recordClassNames.get(i);
                            Class<? extends Record> recordClass =
                                    recordClassForName(recordClassName);
                            String recordId = recordIds.get(i);
                            return RecordIdFilter.fromId(recordClass, recordId);
                        })
                .toList();
    }

    /** Converts a ChangeLogTokenRequest to a bundle. */
    public static Bundle fromChangeLogTokenRequest(ChangeLogTokenRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, GET_CHANGE_LOG_TOKEN_QUERY);
        bundle.putParcelable(CHANGE_LOG_TOKEN_REQUEST, request);
        return bundle;
    }

    /** Converts a bundle to a ChangeLogTokenRequest. */
    public static ChangeLogTokenRequest toChangeLogTokenRequest(Bundle bundle) {
        return bundle.getParcelable(CHANGE_LOG_TOKEN_REQUEST, ChangeLogTokenRequest.class);
    }

    /** Converts a changelog token response to a bundle. */
    public static Bundle fromChangeLogTokenResponse(String token) {
        Bundle bundle = new Bundle();
        bundle.putString(CHANGE_LOG_TOKEN, token);
        return bundle;
    }

    /** Converts a bundle to a change log token response. */
    public static String toChangeLogTokenResponse(Bundle bundle) {
        return bundle.getString(CHANGE_LOG_TOKEN);
    }

    /** Converts a ChangeLogsRequest to a bundle. */
    public static Bundle fromChangeLogsRequest(ChangeLogsRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_CHANGE_LOGS_QUERY);
        bundle.putParcelable(CHANGE_LOGS_REQUEST, request);
        return bundle;
    }

    /** Converts a bundle to a ChangeLogsRequest. */
    public static ChangeLogsRequest toChangeLogsRequest(Bundle bundle) {
        return bundle.getParcelable(CHANGE_LOGS_REQUEST, ChangeLogsRequest.class);
    }

    /** Converts a ChangeLogsResponse to a bundle. */
    public static Bundle fromChangeLogsResponse(ChangeLogsResponse response) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CHANGE_LOGS_RESPONSE, response);
        return bundle;
    }

    /** Converts a bundle to a ChangeLogsResponse. */
    public static ChangeLogsResponse toChangeLogsResponse(Bundle bundle) {
        return bundle.getParcelable(CHANGE_LOGS_RESPONSE, ChangeLogsResponse.class);
    }

    private static List<Bundle> fromRecordList(List<? extends Record> records) {
        return records.stream().map(BundleHelper::fromRecord).toList();
    }

    private static List<? extends Record> toRecordList(List<Bundle> bundles) {
        return bundles.stream().map(BundleHelper::toRecord).toList();
    }

    private static Bundle fromRecord(Record record) {
        Bundle bundle = new Bundle();
        bundle.putString(RECORD_CLASS_NAME, record.getClass().getName());
        bundle.putBundle(METADATA, fromMetadata(record.getMetadata()));

        if (record instanceof IntervalRecord intervalRecord) {
            bundle.putLong(START_TIME_MILLIS, intervalRecord.getStartTime().toEpochMilli());
            bundle.putLong(END_TIME_MILLIS, intervalRecord.getEndTime().toEpochMilli());
        } else if (record instanceof InstantRecord instantRecord) {
            bundle.putLong(START_TIME_MILLIS, instantRecord.getTime().toEpochMilli());
        } else {
            throw new IllegalArgumentException("Unsupported record type: ");
        }

        Bundle values;

        if (record instanceof BasalMetabolicRateRecord basalMetabolicRateRecord) {
            values = getBasalMetabolicRateRecordValues(basalMetabolicRateRecord);
        } else if (record instanceof ExerciseSessionRecord exerciseSessionRecord) {
            values = getExerciseSessionRecordValues(exerciseSessionRecord);
        } else if (record instanceof StepsRecord stepsRecord) {
            values = getStepsRecordValues(stepsRecord);
        } else if (record instanceof HeartRateRecord heartRateRecord) {
            values = getHeartRateRecordValues(heartRateRecord);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported record type: " + record.getClass().getName());
        }

        bundle.putBundle(VALUES, values);

        Record decodedRecord = toRecord(bundle);
        if (!record.equals(decodedRecord)) {
            throw new IllegalArgumentException(
                    "Some fields are incorrectly encoded in " + record.getClass().getSimpleName());
        }

        return bundle;
    }

    private static Record toRecord(Bundle bundle) {
        Metadata metadata = toMetadata(bundle.getBundle(METADATA));

        String recordClassName = bundle.getString(RECORD_CLASS_NAME);

        Instant startTime = Instant.ofEpochMilli(bundle.getLong(START_TIME_MILLIS));
        Instant endTime = Instant.ofEpochMilli(bundle.getLong(END_TIME_MILLIS));

        Bundle values = bundle.getBundle(VALUES);

        if (Objects.equals(recordClassName, BasalMetabolicRateRecord.class.getName())) {
            return createBasalMetabolicRateRecord(metadata, startTime, values);
        } else if (Objects.equals(recordClassName, ExerciseSessionRecord.class.getName())) {
            return createExerciseSessionRecord(metadata, startTime, endTime, values);
        } else if (Objects.equals(recordClassName, HeartRateRecord.class.getName())) {
            return createHeartRateRecord(metadata, startTime, endTime, values);
        } else if (Objects.equals(recordClassName, StepsRecord.class.getName())) {
            return createStepsRecord(metadata, startTime, endTime, values);
        }

        throw new IllegalArgumentException("Unsupported record type: " + recordClassName);
    }

    private static Bundle getBasalMetabolicRateRecordValues(BasalMetabolicRateRecord record) {
        Bundle values = new Bundle();
        values.putDouble(POWER_WATTS, record.getBasalMetabolicRate().getInWatts());
        return values;
    }

    private static BasalMetabolicRateRecord createBasalMetabolicRateRecord(
            Metadata metadata, Instant time, Bundle values) {
        double powerWatts = values.getDouble(POWER_WATTS);

        return new BasalMetabolicRateRecord.Builder(metadata, time, Power.fromWatts(powerWatts))
                .build();
    }

    private static Bundle getExerciseSessionRecordValues(ExerciseSessionRecord record) {
        Bundle values = new Bundle();

        values.putInt(EXERCISE_SESSION_TYPE, record.getExerciseType());

        ExerciseRoute route = record.getRoute();

        if (route != null) {
            long[] timestamps =
                    route.getRouteLocations().stream()
                            .map(ExerciseRoute.Location::getTime)
                            .mapToLong(Instant::toEpochMilli)
                            .toArray();
            double[] latitudes =
                    route.getRouteLocations().stream()
                            .mapToDouble(ExerciseRoute.Location::getLatitude)
                            .toArray();
            double[] longitudes =
                    route.getRouteLocations().stream()
                            .mapToDouble(ExerciseRoute.Location::getLongitude)
                            .toArray();
            List<Double> altitudes =
                    route.getRouteLocations().stream()
                            .map(ExerciseRoute.Location::getAltitude)
                            .map(alt -> transformOrNull(alt, Length::getInMeters))
                            .toList();
            List<Double> hAccs =
                    route.getRouteLocations().stream()
                            .map(ExerciseRoute.Location::getHorizontalAccuracy)
                            .map(hAcc -> transformOrNull(hAcc, Length::getInMeters))
                            .toList();
            List<Double> vAccs =
                    route.getRouteLocations().stream()
                            .map(ExerciseRoute.Location::getVerticalAccuracy)
                            .map(vAcc -> transformOrNull(vAcc, Length::getInMeters))
                            .toList();

            values.putLongArray(EXERCISE_ROUTE_TIMESTAMPS, timestamps);
            values.putDoubleArray(EXERCISE_ROUTE_LATITUDES, latitudes);
            values.putDoubleArray(EXERCISE_ROUTE_LONGITUDES, longitudes);
            values.putSerializable(EXERCISE_ROUTE_ALTITUDES, new ArrayList<>(altitudes));
            values.putSerializable(EXERCISE_ROUTE_HACCS, new ArrayList<>(hAccs));
            values.putSerializable(EXERCISE_ROUTE_VACCS, new ArrayList<>(vAccs));
        }

        values.putBoolean(EXERCISE_HAS_ROUTE, record.hasRoute());

        long[] segmentStartTimes =
                record.getSegments().stream()
                        .map(ExerciseSegment::getStartTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        long[] segmentEndTimes =
                record.getSegments().stream()
                        .map(ExerciseSegment::getEndTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        int[] segmentTypes =
                record.getSegments().stream().mapToInt(ExerciseSegment::getSegmentType).toArray();

        values.putLongArray(EXERCISE_SEGMENT_START_TIMES, segmentStartTimes);
        values.putLongArray(EXERCISE_SEGMENT_END_TIMES, segmentEndTimes);
        values.putIntArray(EXERCISE_SEGMENT_TYPES, segmentTypes);

        return values;
    }

    private static ExerciseSessionRecord createExerciseSessionRecord(
            Metadata metadata, Instant startTime, Instant endTime, Bundle values) {
        int exerciseType = values.getInt(EXERCISE_SESSION_TYPE);

        ExerciseSessionRecord.Builder record =
                new ExerciseSessionRecord.Builder(metadata, startTime, endTime, exerciseType);

        long[] routeTimestamps = values.getLongArray(EXERCISE_ROUTE_TIMESTAMPS);

        int locationCount = routeTimestamps == null ? 0 : routeTimestamps.length;

        if (locationCount > 0) {
            double[] latitudes = values.getDoubleArray(EXERCISE_ROUTE_LATITUDES);
            double[] longitudes = values.getDoubleArray(EXERCISE_ROUTE_LONGITUDES);
            List<Double> altitudes =
                    values.getSerializable(EXERCISE_ROUTE_ALTITUDES, ArrayList.class);
            List<Double> hAccs = values.getSerializable(EXERCISE_ROUTE_HACCS, ArrayList.class);
            List<Double> vAccs = values.getSerializable(EXERCISE_ROUTE_VACCS, ArrayList.class);
            List<ExerciseRoute.Location> locations =
                    IntStream.range(0, locationCount)
                            .mapToObj(
                                    i -> {
                                        Instant time = Instant.ofEpochMilli(routeTimestamps[i]);
                                        double latitude = latitudes[i];
                                        double longitude = longitudes[i];
                                        Double altitude = altitudes.get(i);
                                        Double hAcc = hAccs.get(i);
                                        Double vAcc = vAccs.get(i);

                                        var location =
                                                new ExerciseRoute.Location.Builder(
                                                        time, latitude, longitude);

                                        if (altitude != null) {
                                            location.setAltitude(Length.fromMeters(altitude));
                                        }

                                        if (hAcc != null) {
                                            location.setHorizontalAccuracy(Length.fromMeters(hAcc));
                                        }

                                        if (vAcc != null) {
                                            location.setVerticalAccuracy(Length.fromMeters(vAcc));
                                        }

                                        return location.build();
                                    })
                            .toList();

            record.setRoute(new ExerciseRoute(locations));
        }

        boolean hasRoute = values.getBoolean(EXERCISE_HAS_ROUTE);

        if (hasRoute && locationCount == 0) {
            // Handle the `route == null && hasRoute == true` case which is a valid state.
            setHasRoute(record, hasRoute);
        }

        long[] segmentStartTimes = values.getLongArray(EXERCISE_SEGMENT_START_TIMES);
        long[] segmentEndTimes = values.getLongArray(EXERCISE_SEGMENT_END_TIMES);
        int[] segmentTypes = values.getIntArray(EXERCISE_SEGMENT_TYPES);

        List<ExerciseSegment> segments =
                IntStream.range(0, segmentStartTimes.length)
                        .mapToObj(
                                i -> {
                                    Instant segmentStartTime =
                                            Instant.ofEpochMilli(segmentStartTimes[i]);
                                    Instant segmentEndTime =
                                            Instant.ofEpochMilli(segmentEndTimes[i]);
                                    return new ExerciseSegment.Builder(
                                                    segmentStartTime,
                                                    segmentEndTime,
                                                    segmentTypes[i])
                                            .build();
                                })
                        .toList();

        record.setSegments(segments);

        return record.build();
    }

    private static Bundle getHeartRateRecordValues(HeartRateRecord record) {
        Bundle values = new Bundle();
        long[] times =
                record.getSamples().stream()
                        .map(HeartRateRecord.HeartRateSample::getTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        long[] bpms =
                record.getSamples().stream()
                        .mapToLong(HeartRateRecord.HeartRateSample::getBeatsPerMinute)
                        .toArray();

        values.putLongArray(SAMPLE_TIMES, times);
        values.putLongArray(SAMPLE_VALUES, bpms);
        return values;
    }

    private static HeartRateRecord createHeartRateRecord(
            Metadata metadata, Instant startTime, Instant endTime, Bundle values) {

        long[] times = values.getLongArray(SAMPLE_TIMES);
        long[] bpms = values.getLongArray(SAMPLE_VALUES);

        List<HeartRateRecord.HeartRateSample> samples =
                IntStream.range(0, times.length)
                        .mapToObj(
                                i ->
                                        new HeartRateRecord.HeartRateSample(
                                                bpms[i], Instant.ofEpochMilli(times[i])))
                        .toList();

        return new HeartRateRecord.Builder(metadata, startTime, endTime, samples).build();
    }

    private static Bundle getStepsRecordValues(StepsRecord record) {
        Bundle values = new Bundle();
        values.putLong(COUNT, record.getCount());
        return values;
    }

    private static StepsRecord createStepsRecord(
            Metadata metadata, Instant startTime, Instant endTime, Bundle values) {
        long count = values.getLong(COUNT);

        return new StepsRecord.Builder(metadata, startTime, endTime, count).build();
    }

    private static Bundle fromMetadata(Metadata metadata) {
        Bundle bundle = new Bundle();
        bundle.putString(RECORD_ID, metadata.getId());
        bundle.putString(PACKAGE_NAME, metadata.getDataOrigin().getPackageName());
        bundle.putString(CLIENT_ID, metadata.getClientRecordId());
        return bundle;
    }

    private static Metadata toMetadata(Bundle bundle) {
        Metadata.Builder metadata = new Metadata.Builder();

        ifNotNull(bundle.getString(RECORD_ID), metadata::setId);
        ifNotNull(
                bundle.getString(PACKAGE_NAME),
                packageName ->
                        metadata.setDataOrigin(
                                new DataOrigin.Builder().setPackageName(packageName).build()));
        metadata.setClientRecordId(bundle.getString(CLIENT_ID));

        return metadata.build();
    }

    private static <T> void ifNotNull(T obj, Consumer<T> consumer) {
        if (obj == null) {
            return;
        }
        consumer.accept(obj);
    }

    private static <T, R> R transformOrNull(T obj, Function<T, R> transform) {
        if (obj == null) {
            return null;
        }
        return transform.apply(obj);
    }

    private static Class<? extends Record> recordClassForName(String className) {
        try {
            return (Class<? extends Record>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Calls {@code ExerciseSessionRecord.Builder.setHasRoute} using reflection as the method is
     * hidden.
     */
    private static void setHasRoute(ExerciseSessionRecord.Builder record, boolean hasRoute) {
        // Getting a hidden method by its signature using getMethod() throws an exception in test
        // apps, but iterating throw all the methods and getting the needed one works.
        for (var method : record.getClass().getMethods()) {
            if (method.getName().equals("setHasRoute")) {
                try {
                    method.invoke(record, hasRoute);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    private BundleHelper() {}
}
