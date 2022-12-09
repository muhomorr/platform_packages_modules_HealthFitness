/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.storage.request;

import android.annotation.NonNull;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.AggregateResult;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.aidl.AggregateDataRequestParcel;
import android.healthconnect.aidl.AggregateDataResponseParcel;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.util.ArrayMap;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;

import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Refines aggregate request from what the client sent to a format that makes the most sense for the
 * TransactionManager.
 *
 * @hide
 */
public final class AggregateTransactionRequest {
    private final String mPackageName;
    private final List<AggregateTableRequest> mAggregateTableRequests;
    private final Period mPeriod;
    private final Duration mDuration;
    private final TimeRangeFilter mTimeRangeFilter;

    /**
     * TODO(b/249581069): Add support for aggregates that require information from multiple tables
     */
    public AggregateTransactionRequest(
            @NonNull String packageName, @NonNull AggregateDataRequestParcel request) {
        mPackageName = packageName;
        mAggregateTableRequests = new ArrayList<>(request.getAggregateIds().length);
        mPeriod = request.getPeriod();
        mDuration = request.getDuration();
        mTimeRangeFilter = request.getTimeRangeFilter();

        final AggregationTypeIdMapper aggregationTypeIdMapper =
                AggregationTypeIdMapper.getInstance();
        for (int id : request.getAggregateIds()) {
            AggregationType<?> aggregationType = aggregationTypeIdMapper.getAggregationTypeFor(id);
            List<Integer> recordTypeIds = aggregationType.getApplicableRecordTypeIds();
            if (recordTypeIds.size() == 1) {
                RecordHelper<?> recordHelper =
                        RecordHelperProvider.getInstance().getRecordHelper(recordTypeIds.get(0));
                AggregateTableRequest aggregateTableRequest =
                        recordHelper.getAggregateTableRequest(
                                aggregationType,
                                request.getPackageFilters(),
                                request.getStartTime(),
                                request.getEndTime());

                if (mDuration != null) {
                    aggregateTableRequest.setGroupBy(
                            recordHelper.getDurationGroupByColumnName(),
                            mDuration,
                            mTimeRangeFilter);
                } else if (mPeriod != null) {
                    aggregateTableRequest.setGroupBy(
                            recordHelper.getPeriodGroupByColumnName(), mPeriod, mTimeRangeFilter);
                }

                mAggregateTableRequests.add(aggregateTableRequest);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * @return Compute and return aggregations
     */
    public AggregateDataResponseParcel getAggregateDataResponseParcel() {
        Map<AggregationType<?>, List<AggregateResult<?>>> results = new ArrayMap<>();
        int size = 0;
        for (AggregateTableRequest aggregateTableRequest : mAggregateTableRequests) {
            // Compute aggregations
            TransactionManager.getInitialisedInstance()
                    .populateWithAggregation(aggregateTableRequest);
            results.put(
                    aggregateTableRequest.getAggregationType(),
                    aggregateTableRequest.getAggregateResults());
            size = aggregateTableRequest.getAggregateResults().size();
        }

        // Convert DB friendly results to aggregateRecordsResponses
        List<AggregateRecordsResponse<?>> aggregateRecordsResponses = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<Integer, AggregateResult<?>> aggregateResultMap = new ArrayMap<>();
            for (AggregationType<?> aggregationType : results.keySet()) {
                aggregateResultMap.put(
                        (AggregationTypeIdMapper.getInstance().getIdFor(aggregationType)),
                        Objects.requireNonNull(results.get(aggregationType)).get(i));
            }
            aggregateRecordsResponses.add(new AggregateRecordsResponse<>(aggregateResultMap));
        }

        // Create and return parcel
        AggregateDataResponseParcel aggregateDataResponseParcel =
                new AggregateDataResponseParcel(aggregateRecordsResponses);
        if (mPeriod != null) {
            aggregateDataResponseParcel.setPeriod(mPeriod, mTimeRangeFilter);
        } else if (mDuration != null) {
            aggregateDataResponseParcel.setDuration(mDuration, mTimeRangeFilter);
        }

        return aggregateDataResponseParcel;
    }
}
