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
package com.android.healthconnect.controller.tests.data.appdata

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.os.OutcomeReceiver
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.appdata.AppDataUseCase
import com.android.healthconnect.controller.data.appdata.AppDataViewModel
import com.android.healthconnect.controller.data.appdata.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppDataViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = TestCoroutineDispatcher()

    @Inject lateinit var appInfoReader: AppInfoReader

    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    private lateinit var viewModel: AppDataViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel = AppDataViewModel(appInfoReader, AppDataUseCase(manager, Dispatchers.Main))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun loadAppData_noData_returnsEmptyList() = runTest {
        doAnswer(prepareAnswer(mapOf())).`when`(manager).queryAllRecordTypesInfo(any(), any())

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(HealthDataCategory.ACTIVITY, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.BODY_MEASUREMENTS, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.VITALS, listOf()))
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
    }

    @Test
    fun loadAppData_hasData_returnsDataWrittenByGivenApp() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2)))),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.HEART_RATE,
                        HealthDataCategory.VITALS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME)))))
        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY, listOf(FitnessPermissionType.STEPS)),
                PermissionTypesPerCategory(HealthDataCategory.BODY_MEASUREMENTS, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS, listOf(FitnessPermissionType.HEART_RATE)))
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
    }

    // TODO(b/355793284): Update once API is ready and add more tests.
    @Test
    fun loadAppData_fitnessAndMedicalData_returnsDataWrittenByGivenApp() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2)))),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.HEART_RATE,
                        HealthDataCategory.VITALS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME)))))
        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY, listOf(FitnessPermissionType.STEPS)),
                PermissionTypesPerCategory(HealthDataCategory.BODY_MEASUREMENTS, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS, listOf(FitnessPermissionType.HEART_RATE)),
                PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.IMMUNIZATION)))
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
    }

    private fun prepareAnswer(
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(recordTypeInfoMap)
            recordTypeInfoMap
        }
        return answer
    }
}
