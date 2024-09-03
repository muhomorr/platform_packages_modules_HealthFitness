/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.storage.utils;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;

import static com.android.server.healthconnect.storage.utils.InternalDataTypeDescriptors.getAllInternalDataTypeDescriptors;
import static com.android.server.healthconnect.storage.utils.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

@EnableFlags({Flags.FLAG_HEALTH_CONNECT_MAPPINGS})
public class InternalHealthConnectMappingsTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void recordTypeIds_uniques() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();

        List<Integer> recordTypeIds =
                descriptors.stream()
                        .map(InternalDataTypeDescriptor::getRecordTypeIdentifier)
                        .toList();

        assertThat(recordTypeIds).containsNoDuplicates();
        assertThat(recordTypeIds).hasSize(descriptors.size());
        assertThat(recordTypeIds).doesNotContain(RECORD_TYPE_UNKNOWN);
    }

    @Test
    public void getRecordTypeIdForUuid() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings = new InternalHealthConnectMappings();
        for (var descriptor : descriptors) {
            String className = descriptor.getRecordHelper().getClass().getSimpleName();
            int recordTypeId = descriptor.getRecordTypeIdentifier();

            assertWithMessage(className)
                    .that(mappings.getRecordTypeIdForUuid(recordTypeId))
                    .isEqualTo(RecordTypeForUuidMappings.getRecordTypeIdForUuid(recordTypeId));
            assertWithMessage(className)
                    .that(mappings.getRecordTypeIdForUuid(recordTypeId))
                    .isGreaterThan(RECORD_TYPE_ID_FOR_UUID_UNKNOWN);
        }

        List<Integer> allRecordIdsForUuid =
                descriptors.stream()
                        .map(InternalDataTypeDescriptor::getRecordTypeIdForUuid)
                        .toList();
        assertThat(allRecordIdsForUuid).containsNoDuplicates();
        assertThat(allRecordIdsForUuid).hasSize(descriptors.size());
        assertThat(allRecordIdsForUuid).doesNotContain(RECORD_TYPE_ID_FOR_UUID_UNKNOWN);
    }

    @Test
    public void getRecordHelpers() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings = new InternalHealthConnectMappings(descriptors);

        Collection<RecordHelper<?>> recordHelpers = mappings.getRecordHelpers();

        assertThat(recordHelpers).containsNoDuplicates();
        assertThat(recordHelpers).hasSize(descriptors.size());
        for (var descriptor : descriptors) {
            assertThat(recordHelpers).contains(descriptor.getRecordHelper());
        }

        assertThat(recordHelpers.stream().map(Object::getClass).toList())
                .containsExactlyElementsIn(
                        RecordHelperProvider.getRecordHelpers().values().stream()
                                .map(x -> x.getClass())
                                .toList());
    }

    @Test
    public void getRecordHelper() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings = new InternalHealthConnectMappings(descriptors);

        for (var descriptor : descriptors) {
            int recordTypeId = descriptor.getRecordTypeIdentifier();
            assertThat(mappings.getRecordHelper(recordTypeId))
                    .isSameInstanceAs(descriptor.getRecordHelper());
            assertThat(mappings.getRecordHelper(recordTypeId))
                    .isInstanceOf(RecordHelperProvider.getRecordHelper(recordTypeId).getClass());
        }

        assertThat(
                        descriptors.stream()
                                .map(InternalDataTypeDescriptor::getRecordTypeIdentifier)
                                .map(mappings::getRecordHelper)
                                .map(Object::getClass)
                                .toList())
                .containsNoDuplicates();
    }

    @Test
    public void getLoggingEnumForRecordTypeId() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings = new InternalHealthConnectMappings();

        for (var descriptor : descriptors) {
            int loggingEnum =
                    mappings.getLoggingEnumForRecordTypeId(descriptor.getRecordTypeIdentifier());

            assertThat(loggingEnum).isEqualTo(descriptor.getLoggingEnum());
            assertThat(loggingEnum)
                    .isEqualTo(
                            HealthConnectServiceLogger.Builder.getDataTypeEnumFromRecordType(
                                    descriptor.getRecordTypeIdentifier()));
            assertThat(loggingEnum)
                    .isNotEqualTo(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN);
        }

        assertThat(
                        descriptors.stream()
                                .map(InternalDataTypeDescriptor::getRecordTypeIdentifier)
                                .map(mappings::getLoggingEnumForRecordTypeId)
                                .toList())
                .containsNoDuplicates();
    }
}
