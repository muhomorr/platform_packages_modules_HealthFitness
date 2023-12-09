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

package com.android.server.healthconnect.injector;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.InstrumentationRegistry;

import com.android.server.healthconnect.HealthConnectDeviceConfigManager;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HealthConnectInjectorTest {

    private Context mContext;
    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private TransactionManager mTransactionManager;
    @Mock private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = InstrumentationRegistry.getContext();
        HealthConnectDeviceConfigManager.initializeInstance(mContext);
    }

    @Test
    public void setFakePackageInfoUtils_injectorReturnsFakePackageInfoUtils() {
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPackageInfoUtils(mPackageInfoUtils)
                        .build();

        assertThat(healthConnectInjector.getPackageInfoUtils()).isEqualTo(mPackageInfoUtils);
    }

    @Test
    public void testProductionInjector_injectorReturnsOriginalPackageInfoUtils() {
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext).build();

        assertThat(healthConnectInjector.getPackageInfoUtils())
                .isEqualTo(PackageInfoUtils.getInstance());
    }

    @Test
    public void setFakeHealthDataCategoryPriorityHelper_injectorReturnsFakeTransactionManager() {
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setHealthDataCategoryPriorityHelper(mHealthDataCategoryPriorityHelper)
                        .build();

        assertThat(healthConnectInjector.getHealthDataCategoryPriorityHelper())
                .isEqualTo(mHealthDataCategoryPriorityHelper);
    }

    @Test
    public void testProductionInjector_injectorReturnsOriginalHealthDataCategoryPriorityHelper() {
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext).build();

        assertThat(healthConnectInjector.getHealthDataCategoryPriorityHelper())
                .isEqualTo(HealthDataCategoryPriorityHelper.getInstance());
    }
}