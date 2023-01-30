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
 *
 *
 */

package com.android.healthconnect.controller.dataentries

import android.healthconnect.HealthConnectManager
import android.healthconnect.ReadRecordsRequestUsingFilters
import android.healthconnect.ReadRecordsResponse
import android.healthconnect.TimeInstantRangeFilter
import android.healthconnect.datatypes.MenstruationFlowRecord
import android.healthconnect.datatypes.MenstruationPeriodRecord
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.dataentries.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.dataentries.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import java.time.Duration.ofDays
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

class LoadMenstruationDataUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val healthDataEntryFormatter: HealthDataEntryFormatter,
    private val menstruationPeriodFormatter: MenstruationPeriodFormatter,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<Instant, List<FormattedEntry>>(dispatcher) {

    companion object {
        private val SEARCH_RANGE = ofDays(30)
    }

    override suspend fun execute(selectedDate: Instant): List<FormattedEntry> {
        val data = buildList {
            addAll(getMenstruationPeriodRecords(selectedDate))
            addAll(getMenstruationFlowRecords(selectedDate))
        }
        return data
    }

    private suspend fun getMenstruationPeriodRecords(selectedDate: Instant): List<FormattedEntry> {
        val startDate = selectedDate.truncatedTo(ChronoUnit.DAYS)
        val end = startDate.plus(ofHours(23)).plus(ofMinutes(59))
        val start = end.minus(SEARCH_RANGE)

        // Special-casing MenstruationPeriod as it spans multiple days and we show it on all these
        // days in the UI (not just the first day).
        // Hardcode max period length to 30 days (completely arbitrary number).
        val timeRange = TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build()
        val filter =
            ReadRecordsRequestUsingFilters.Builder(MenstruationPeriodRecord::class.java)
                .setTimeRangeFilter(timeRange)
                .build()

        val records =
            suspendCancellableCoroutine<ReadRecordsResponse<MenstruationPeriodRecord>> {
                    continuation ->
                    healthConnectManager.readRecords(
                        filter, Runnable::run, continuation.asOutcomeReceiver())
                }
                .records
                .filter { menstruationPeriodRecord ->
                    menstruationPeriodRecord.startTime.isBefore(startDate) &&
                        menstruationPeriodRecord.endTime.isAfter(startDate)
                }

        return records.map { record -> menstruationPeriodFormatter.format(startDate, record) }
    }

    private suspend fun getMenstruationFlowRecords(selectedDate: Instant): List<FormattedEntry> {
        val start = selectedDate.truncatedTo(ChronoUnit.DAYS)
        val end = start.plus(ofHours(23)).plus(ofMinutes(59))
        val timeRange = TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build()
        val filter =
            ReadRecordsRequestUsingFilters.Builder(MenstruationFlowRecord::class.java)
                .setTimeRangeFilter(timeRange)
                .build()

        val records =
            suspendCancellableCoroutine<ReadRecordsResponse<MenstruationFlowRecord>> { continuation
                    ->
                    healthConnectManager.readRecords(
                        filter, Runnable::run, continuation.asOutcomeReceiver())
                }
                .records

        return records.map { healthDataEntryFormatter.format(it) }
    }
}
