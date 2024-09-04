/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.selectabledeletion.api

import android.health.connect.DeleteUsingFiltersRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.CyclingPedalingCadenceRecord
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeletionTypeHealthPermissionTypesFromApp
import com.android.healthconnect.controller.selectabledeletion.api.DeleteFitnessPermissionTypesFromAppUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@HiltAndroidTest
class DeletePermissionTypeUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeleteFitnessPermissionTypesFromAppUseCase
    var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = DeleteFitnessPermissionTypesFromAppUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_deletePermissionTypesFromApp_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any(DeleteUsingFiltersRequest::class.java), any(), any())

        val deletePermissionTypes =
            DeletionTypeHealthPermissionTypesFromApp(
                listOf(
                    FitnessPermissionType.STEPS,
                    FitnessPermissionType.HEART_RATE,
                    FitnessPermissionType.SLEEP,
                    FitnessPermissionType.EXERCISE,
                    FitnessPermissionType.MENSTRUATION,
                    MedicalPermissionType.IMMUNIZATION,
                ),
                packageName = "package.name",
                appName = "APP_NAME",
            )

        useCase.invoke(deletePermissionTypes)

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), any(), any())

        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins)
            .containsExactly(DataOrigin.Builder().setPackageName("package.name").build())
        assertThat(filtersCaptor.value.recordTypes)
            .containsExactly(
                StepsRecord::class.java,
                StepsCadenceRecord::class.java,
                HeartRateRecord::class.java,
                SleepSessionRecord::class.java,
                ExerciseSessionRecord::class.java,
                MenstruationFlowRecord::class.java,
                MenstruationPeriodRecord::class.java,
                CyclingPedalingCadenceRecord::class.java,
            )
    }

    private fun prepareAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { _: InvocationOnMock -> null }
        return answer
    }
}
