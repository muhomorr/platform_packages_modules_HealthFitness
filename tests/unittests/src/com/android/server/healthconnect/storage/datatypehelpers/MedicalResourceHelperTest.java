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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_UNKNOWN;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_UNKNOWN;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.cts.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.utils.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.cts.utils.PhrDataFactory.R4_VERSION_STRING;
import static android.healthconnect.cts.utils.PhrDataFactory.addCompletedStatus;
import static android.healthconnect.cts.utils.PhrDataFactory.createImmunizationMedicalResources;
import static android.healthconnect.cts.utils.PhrDataFactory.getFhirResource;
import static android.healthconnect.cts.utils.PhrDataFactory.getFhirResourceAllergy;
import static android.healthconnect.cts.utils.PhrDataFactory.getFhirResourceBuilder;
import static android.healthconnect.cts.utils.PhrDataFactory.getFhirResourceDifferentImmunization;
import static android.healthconnect.cts.utils.PhrDataFactory.getUpdatedAllergyFhirResource;
import static android.healthconnect.cts.utils.PhrDataFactory.getUpdatedImmunizationFhirResource;

import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.DATA_SOURCE_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_DATA_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_RESOURCE_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_RESOURCE_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_VERSION_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.MEDICAL_RESOURCE_TABLE_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.getCreateTableRequest;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.getPrimaryColumn;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getParentColumnReference;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getTableName;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_UNIQUE_NON_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.generateMedicalResourceUUID;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getHexString;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.HealthConnectManager;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesRequest;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.utils.PhrDataFactory;
import android.os.Environment;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.HealthConnectUserContext;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import com.google.common.collect.ImmutableList;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MedicalResourceHelperTest {

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 2)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(HealthConnectManager.class)
                    .mockStatic(Environment.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    @Rule(order = 3)
    public final HealthConnectDatabaseTestRule mHealthConnectDatabaseTestRule =
            new HealthConnectDatabaseTestRule();

    private MedicalResourceHelper mMedicalResourceHelper;
    private MedicalDataSourceHelper mMedicalDataSourceHelper;
    private TransactionManager mTransactionManager;
    private HealthConnectUserContext mContext;
    private static final long DATA_SOURCE_ROW_ID = 1234;
    private static final String INVALID_PAGE_TOKEN = "aw==";

    @Before
    public void setup() {
        mTransactionManager = mHealthConnectDatabaseTestRule.getTransactionManager();
        mMedicalDataSourceHelper =
                new MedicalDataSourceHelper(mTransactionManager, AppInfoHelper.getInstance());
        mMedicalResourceHelper =
                new MedicalResourceHelper(mTransactionManager, mMedicalDataSourceHelper);
        mContext = mHealthConnectDatabaseTestRule.getUserContext();
        TransactionTestUtils mTransactionTestUtils =
                new TransactionTestUtils(mContext, mTransactionManager);
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
    }

    @Test
    public void getCreateTableRequest_correctResult() {
        List<Pair<String, String>> columnInfoMedicalResource =
                List.of(
                        Pair.create(getPrimaryColumn(), PRIMARY_AUTOINCREMENT),
                        Pair.create(FHIR_RESOURCE_TYPE_COLUMN_NAME, INTEGER_NOT_NULL),
                        Pair.create(FHIR_RESOURCE_ID_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(FHIR_DATA_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(FHIR_VERSION_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(DATA_SOURCE_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                        Pair.create(UUID_COLUMN_NAME, BLOB_UNIQUE_NON_NULL),
                        Pair.create(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER));
        List<Pair<String, String>> columnInfoMedicalResourceIndices =
                List.of(
                        Pair.create(getParentColumnReference(), INTEGER_NOT_NULL),
                        Pair.create(getMedicalResourceTypeColumnName(), INTEGER_NOT_NULL));
        CreateTableRequest childTableRequest =
                new CreateTableRequest(getTableName(), columnInfoMedicalResourceIndices)
                        .addForeignKey(
                                MEDICAL_RESOURCE_TABLE_NAME,
                                Collections.singletonList(getParentColumnReference()),
                                Collections.singletonList(getPrimaryColumn()));
        CreateTableRequest expected =
                new CreateTableRequest(MEDICAL_RESOURCE_TABLE_NAME, columnInfoMedicalResource)
                        .addForeignKey(
                                MedicalDataSourceHelper.getMainTableName(),
                                Collections.singletonList(DATA_SOURCE_ID_COLUMN_NAME),
                                Collections.singletonList(PRIMARY_COLUMN_NAME))
                        .setChildTableRequests(List.of(childTableRequest));

        CreateTableRequest result = getCreateTableRequest();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void getUpsertTableRequest_correctResult() {
        FhirResource fhirResource = getFhirResource();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        DATA_SOURCE_ID);
        UUID uuid =
                generateMedicalResourceUUID(
                        fhirResource.getId(), fhirResource.getType(), DATA_SOURCE_ID);

        UpsertTableRequest upsertRequest =
                MedicalResourceHelper.getUpsertTableRequest(
                        uuid, DATA_SOURCE_ROW_ID, upsertMedicalResourceInternalRequest);
        ContentValues contentValues = upsertRequest.getContentValues();
        UpsertTableRequest childUpsertRequestExpected =
                MedicalResourceIndicesHelper.getChildTableUpsertRequests(
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION);
        UpsertTableRequest childUpsertRequestResult = upsertRequest.getChildTableRequests().get(0);

        assertThat(upsertRequest.getTable()).isEqualTo(MEDICAL_RESOURCE_TABLE_NAME);
        assertThat(upsertRequest.getUniqueColumnsCount()).isEqualTo(1);
        assertThat(contentValues.size()).isEqualTo(6);
        assertThat(contentValues.get(FHIR_RESOURCE_TYPE_COLUMN_NAME))
                .isEqualTo(fhirResource.getType());
        assertThat(contentValues.get(DATA_SOURCE_ID_COLUMN_NAME)).isEqualTo(DATA_SOURCE_ROW_ID);
        assertThat(contentValues.get(FHIR_VERSION_COLUMN_NAME)).isEqualTo(R4_VERSION_STRING);
        assertThat(contentValues.get(FHIR_DATA_COLUMN_NAME)).isEqualTo(fhirResource.getData());
        assertThat(contentValues.get(UUID_COLUMN_NAME))
                .isEqualTo(StorageUtils.convertUUIDToBytes(uuid));
        assertThat(childUpsertRequestResult.getTable())
                .isEqualTo(childUpsertRequestExpected.getTable());
        assertThat(childUpsertRequestResult.getContentValues())
                .isEqualTo(childUpsertRequestExpected.getContentValues());
        assertThat(childUpsertRequestResult.getUniqueColumnsCount())
                .isEqualTo(childUpsertRequestExpected.getUniqueColumnsCount());
    }

    @Test
    public void getReadTableRequest_usingMedicalResourceId_correctQuery() {
        MedicalResourceId medicalResourceId1 =
                new MedicalResourceId(
                        DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, "resourceId1");
        MedicalResourceId medicalResourceId2 =
                new MedicalResourceId(
                        DIFFERENT_DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_UNKNOWN, "resourceId2");
        List<MedicalResourceId> medicalResourceIds =
                List.of(medicalResourceId1, medicalResourceId2);
        String hex1 = makeMedicalResourceHexString(medicalResourceId1);
        String hex2 = makeMedicalResourceHexString(medicalResourceId2);
        List<String> hexValues = List.of(hex1, hex2);

        ReadTableRequest readRequest =
                MedicalResourceHelper.getReadTableRequestByIdsJoinWithIndicesAndDataSourceTables(
                        medicalResourceIds);

        assertThat(readRequest.getTableName()).isEqualTo(MEDICAL_RESOURCE_TABLE_NAME);
        assertThat(readRequest.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM medical_resource_table WHERE uuid IN ("
                                + String.join(", ", hexValues)
                                + ") ) AS inner_query_result  INNER JOIN"
                                + " medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id  INNER JOIN"
                                + " medical_data_source_table ON inner_query_result.data_source_id"
                                + " = medical_data_source_table.row_id");
    }

    @Test
    public void getReadTableRequest_usingRequest_correctQuery() {
        ReadMedicalResourcesRequest request =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION).build();
        ReadTableRequest readRequest =
                MedicalResourceHelper.getReadTableRequestUsingRequestFilterOnMedicalResourceTypes(
                        request);

        // TODO(b/352546342): Explore improving the query building logic, so the query below
        // is simpler to read, for context: http://shortn/_2YCniY49K6
        assertThat(readRequest.getTableName()).isEqualTo(MEDICAL_RESOURCE_TABLE_NAME);
        assertThat(readRequest.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM medical_resource_table ORDER BY"
                                + " medical_resource_row_id LIMIT "
                                + (DEFAULT_PAGE_SIZE + 1)
                                + " ) AS"
                                + " inner_query_result  INNER JOIN ( SELECT * FROM"
                                + " medical_resource_indices_table WHERE medical_resource_type IN "
                                + "("
                                + MEDICAL_RESOURCE_TYPE_IMMUNIZATION
                                + ")"
                                + ") medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id  INNER JOIN"
                                + " medical_data_source_table ON inner_query_result.data_source_id"
                                + " = medical_data_source_table.row_id");
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readMedicalResourcesByIds_WithoutPermissionChecks_returnsEmpty() {
        List<MedicalResourceId> medicalResourceIds =
                List.of(
                        new MedicalResourceId(
                                DATA_SOURCE_ID,
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION));

        List<MedicalResource> resources =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        medicalResourceIds);

        assertThat(resources).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readMedicalResourcesByRequest_dbEmpty_returnsEmpty() {
        ReadMedicalResourcesRequest readImmunizationRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION).build();

        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readImmunizationRequest);

        assertThat(result.getMedicalResources()).isEmpty();
        assertThat(result.getPageToken()).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void upsertAndReadMedicalResourcesByRequest_MedicalResourceTypeDoesNotExist_success() {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        UpsertMedicalResourceInternalRequest upsertImmunizationResourceRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        dataSource.getId());

        List<MedicalResource> upsertedResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(upsertImmunizationResourceRequest));
        ReadMedicalResourcesRequest readUnknownRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_UNKNOWN).build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readUnknownRequest);

        assertThat(upsertedResources)
                .containsExactly(
                        new MedicalResource.Builder(
                                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                        dataSource.getId(),
                                        FHIR_VERSION_R4,
                                        fhirResource)
                                .build());
        assertThat(result.getMedicalResources()).isEmpty();
        assertThat(result.getPageToken()).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void upsertMedicalResourcesSameDataSource_readMedicalResourcesByRequest_success() {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource immunization =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(immunization);
        FhirResource differentFhirResource = getFhirResourceDifferentImmunization();
        MedicalResource differentImmunization =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                differentFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(differentImmunization);
        FhirResource allergyFhirResource = getFhirResourceAllergy();
        MedicalResource allergy =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                allergyFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest3 =
                makeUpsertRequest(allergy);
        List<MedicalResource> upsertedResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2,
                                upsertMedicalResourceInternalRequest3));

        ReadMedicalResourcesRequest readUnknownRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_UNKNOWN).build();
        ReadMedicalResourcesRequest readImmunizationRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION).build();
        ReadMedicalResourcesInternalResponse unknownResourcesResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readUnknownRequest);
        ReadMedicalResourcesInternalResponse immunizationResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readImmunizationRequest);

        assertThat(upsertedResources).containsExactly(immunization, differentImmunization, allergy);
        assertThat(unknownResourcesResult.getMedicalResources()).containsExactly(allergy);
        assertThat(unknownResourcesResult.getPageToken()).isEqualTo(null);
        assertThat(immunizationResult.getMedicalResources())
                .containsExactly(immunization, differentImmunization);
        assertThat(immunizationResult.getPageToken()).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void upsertMedicalResourcesDifferentDataSources_readMedicalResourcesByRequest_success() {
        MedicalDataSource dataSource1 = insertMedicalDataSource("id1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                insertMedicalDataSource("id2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource immunization =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource1.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(immunization);
        FhirResource differentFhirResource = getFhirResourceDifferentImmunization();
        MedicalResource differentImmunization =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource2.getId(),
                                FHIR_VERSION_R4,
                                differentFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(differentImmunization);
        FhirResource allergyFhirResource = getFhirResourceAllergy();
        MedicalResource allergy =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource1.getId(),
                                FHIR_VERSION_R4,
                                allergyFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest3 =
                makeUpsertRequest(allergy);
        List<MedicalResource> upsertedResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2,
                                upsertMedicalResourceInternalRequest3));

        ReadMedicalResourcesRequest readUnknownRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_UNKNOWN).build();
        ReadMedicalResourcesRequest readImmunizationRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION).build();
        ReadMedicalResourcesInternalResponse unknownResourcesResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readUnknownRequest);
        ReadMedicalResourcesInternalResponse immunizationResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readImmunizationRequest);

        assertThat(upsertedResources).containsExactly(immunization, differentImmunization, allergy);
        assertThat(unknownResourcesResult.getMedicalResources()).containsExactly(allergy);
        assertThat(unknownResourcesResult.getPageToken()).isEqualTo(null);
        assertThat(immunizationResult.getMedicalResources())
                .containsExactly(immunization, differentImmunization);
        assertThat(immunizationResult.getPageToken()).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void insertMedicalResources_dataSourceNotInserted_exceptionThrown() {
        FhirResource fhirResource = getFhirResource();
        String datasourceId = "acc6c726-b7ea-42f1-a063-e34f5b4e6247";
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        datasourceId);

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mMedicalResourceHelper.upsertMedicalResources(
                                        List.of(upsertMedicalResourceInternalRequest)));
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Invalid data source id: "
                                + upsertMedicalResourceInternalRequest.getDataSourceId());
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readSubsetOfResources_multipleResourcesUpserted_success() {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource resource1 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(resource1);
        MedicalResourceId medicalResourceId1 =
                new MedicalResourceId(
                        dataSource.getId(), fhirResource.getType(), fhirResource.getId());
        FhirResource allergyFhirResource = getFhirResourceAllergy();
        MedicalResource resource2 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                allergyFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(resource2);

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2));
        List<MedicalResource> readResult =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(medicalResourceId1));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(indicesResult)
                .containsExactly(MEDICAL_RESOURCE_TYPE_IMMUNIZATION, MEDICAL_RESOURCE_TYPE_UNKNOWN);
        assertThat(readResult).containsExactly(resource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void insertMedicalResources_returnsMedicalResources() {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource expectedResource =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(expectedResource);

        List<MedicalResource> result =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(upsertMedicalResourceInternalRequest));

        assertThat(result).containsExactly(expectedResource);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void insertSingleMedicalResource_readSingleResource() throws JSONException {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource expectedResource =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(expectedResource);
        List<MedicalResourceId> ids = List.of(getResourceId(expectedResource));

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(upsertMedicalResourceInternalRequest));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(ids);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).isEqualTo(upsertedMedicalResources);
        assertThat(result).containsExactly(expectedResource);
        assertThat(indicesResult).containsExactly(MEDICAL_RESOURCE_TYPE_IMMUNIZATION);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_noReadOrWritePermissions_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                                List.of(),
                                /* grantedReadMedicalResourceTypes= */ Set.of(),
                                DATA_SOURCE_PACKAGE_NAME,
                                /* hasWritePermission= */ false,
                                /* isCalledFromBgWithoutBgRead= */ false));
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inBgWithoutBgPerm_hasWritePerm_success() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(immunizationDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inBgWithoutBgPerm_hasWritePerm_hasReadPermForResourceTypes_success() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(immunizationDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inBgWithoutBgPerm_noWritePerm_immunizationReadPermOnly_success() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(immunizationDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void
            readById_inBgWithoutBgPerm_noWritePerm_bothUnknownAndImmunizationReadPerm_success() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_UNKNOWN, MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(immunizationDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inForegroundOrinBgWithBgPerm_noWritePerm_success() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(immunizationDatasource1, immunizationDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inForeground_hasWritePerm_noReadResourceTypesPerm_canOnlyReadSelfData() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(immunizationDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inForeground_noWritePerm_readImmunizationPerm_canOnlyReadImmunization() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(immunizationDatasource1, immunizationDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inForeground_noWritePerm_readAllergyPerm_canOnlyReadAllergy() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_UNKNOWN),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(allergyDatasource1, allergyDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readById_inForeground_noWritePerm_readImmunizationAndAllergyPerm_canReadBoth() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_UNKNOWN, MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        immunizationDatasource1,
                        immunizationDatasource2,
                        allergyDatasource1,
                        allergyDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void
            readById_inForeground_hasWritePermAndReadImmunization_readsSelfDataAndImmunizations() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource immunizationDatasource1 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource immunizationDatasource2 =
                upsertResource(PhrDataFactory::createImmunizationMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        immunizationDatasource1.getId(),
                        immunizationDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_IMMUNIZATION),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        immunizationDatasource1, immunizationDatasource2, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readByRequest_isNotEnforceSelfRead_immunizationFilter_canReadAllImmunizations() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        List<MedicalResource> immunizationsDataSource1 =
                upsertResources(
                        PhrDataFactory::createImmunizationMedicalResources,
                        /* numOfResources= */ 4,
                        dataSource1);
        List<MedicalResource> immunizationsDataSource2 =
                upsertResources(
                        PhrDataFactory::createImmunizationMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource2);
        List<MedicalResource> allergyDatasource1 =
                upsertResources(
                        PhrDataFactory::createAllergyMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource1);
        List<MedicalResource> resources =
                joinLists(immunizationsDataSource1, immunizationsDataSource2, allergyDatasource1);

        ReadMedicalResourcesRequest readRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        readRequest, DATA_SOURCE_PACKAGE_NAME, /* enforceSelfRead= */ false);
        String pageToken = result.getPageToken();
        assertThat(result.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(0), resources.get(1)));
        assertThat(pageToken)
                .isEqualTo(PhrPageTokenWrapper.of(readRequest, /* lastRowId= */ 2).encode());

        ReadMedicalResourcesRequest readRequest1 =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .setPageToken(pageToken)
                        .build();
        ReadMedicalResourcesInternalResponse result1 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        readRequest1, DATA_SOURCE_PACKAGE_NAME, /* enforceSelfRead= */ false);
        String pageToken1 = result1.getPageToken();
        assertThat(result1.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(2), resources.get(3)));
        assertThat(pageToken1)
                .isEqualTo(PhrPageTokenWrapper.of(readRequest1, /* lastRowId= */ 4).encode());

        ReadMedicalResourcesRequest readRequest2 =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .setPageToken(pageToken1)
                        .build();
        ReadMedicalResourcesInternalResponse result2 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        readRequest2, DATA_SOURCE_PACKAGE_NAME, /* enforceSelfRead= */ false);
        String pageToken2 = result2.getPageToken();
        assertThat(result2.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(4), resources.get(5)));
        assertThat(pageToken2).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE})
    public void readByRequest_enforceSelfRead_immunizationFilter_canReadOnlySelfImmunization() {
        String dataSource1 = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        String dataSource2 =
                insertMedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        List<MedicalResource> immunizationsDataSource1 =
                upsertResources(
                        PhrDataFactory::createImmunizationMedicalResources,
                        /* numOfResources= */ 4,
                        dataSource1);
        List<MedicalResource> immunizationsDataSource2 =
                upsertResources(
                        PhrDataFactory::createImmunizationMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource2);
        List<MedicalResource> allergyDatasource1 =
                upsertResources(
                        PhrDataFactory::createAllergyMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource1);
        List<MedicalResource> resources =
                joinLists(immunizationsDataSource1, immunizationsDataSource2, allergyDatasource1);

        ReadMedicalResourcesRequest readRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        readRequest, DATA_SOURCE_PACKAGE_NAME, /* enforceSelfRead= */ true);
        String pageToken = result.getPageToken();
        assertThat(result.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(0), resources.get(1)));
        assertThat(pageToken)
                .isEqualTo(PhrPageTokenWrapper.of(readRequest, /* lastRowId= */ 2).encode());

        ReadMedicalResourcesRequest readRequest1 =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .setPageToken(pageToken)
                        .build();
        ReadMedicalResourcesInternalResponse result1 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        readRequest1, DATA_SOURCE_PACKAGE_NAME, /* enforceSelfRead= */ true);
        String pageToken1 = result1.getPageToken();
        assertThat(result1.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(2), resources.get(3)));
        assertThat(pageToken1).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void insertMultipleMedicalResourcesWithSameDataSource_readMultipleResources()
            throws JSONException {
        // TODO(b/351992434): Create test utilities to make these large repeated code blocks
        // clearer.
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource1 = getFhirResource();
        MedicalResource resource1 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource1)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(resource1);
        MedicalResourceId medicalResourceId1 = getResourceId(resource1);
        FhirResource fhirResource2 = getFhirResourceAllergy();
        MedicalResource resource2 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource2)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(resource2);
        MedicalResourceId medicalResourceId2 = getResourceId(resource2);
        List<MedicalResourceId> ids = List.of(medicalResourceId1, medicalResourceId2);

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(ids);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(resource1, resource2);
        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(indicesResult)
                .containsExactly(MEDICAL_RESOURCE_TYPE_IMMUNIZATION, MEDICAL_RESOURCE_TYPE_UNKNOWN);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_DEVELOPMENT_DATABASE})
    public void insertMultipleMedicalResourcesWithDifferentDataSources_readMultipleResources() {
        MedicalDataSource dataSource1 = insertMedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 = insertMedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource resource1 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource1.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(resource1);
        MedicalResourceId medicalResourceId1 = getResourceId(resource1);
        MedicalResource resource2 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource2.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(resource2);
        MedicalResourceId medicalResourceId2 = getResourceId(resource2);
        List<MedicalResourceId> ids = List.of(medicalResourceId1, medicalResourceId2);

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(ids);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(resource1, resource2);
        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(indicesResult)
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION, MEDICAL_RESOURCE_TYPE_IMMUNIZATION);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void updateSingleMedicalResource_success() throws JSONException {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource originalFhirResource = getFhirResource();
        FhirResource fhirResourceUpdated =
                getFhirResourceBuilder()
                        .setData(addCompletedStatus(getFhirResource().getData()))
                        .build();
        UpsertMedicalResourceInternalRequest originalUpsertRequest =
                makeUpsertRequest(
                        originalFhirResource,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        dataSource.getId());
        MedicalResource expectedUpdatedMedicalResource =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResourceUpdated)
                        .build();
        UpsertMedicalResourceInternalRequest updateRequest =
                makeUpsertRequest(expectedUpdatedMedicalResource);
        List<MedicalResourceId> ids = List.of(getResourceId(expectedUpdatedMedicalResource));

        mMedicalResourceHelper.upsertMedicalResources(List.of(originalUpsertRequest));
        List<MedicalResource> updatedMedicalResource =
                mMedicalResourceHelper.upsertMedicalResources(List.of(updateRequest));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(ids);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(expectedUpdatedMedicalResource);
        assertThat(result).isEqualTo(updatedMedicalResource);
        assertThat(indicesResult).containsExactly(MEDICAL_RESOURCE_TYPE_IMMUNIZATION);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResources_updateSingleMedicalResource_success()
            throws JSONException {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource originalResource =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(originalResource);
        FhirResource updatedFhirResource = getUpdatedImmunizationFhirResource();
        MedicalResource updatedResource1 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                updatedFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequestUpdated1 =
                makeUpsertRequest(updatedResource1);
        MedicalResourceId medicalResourceId1 = getResourceId(updatedResource1);
        FhirResource allergyFhirResource = getFhirResourceAllergy();
        MedicalResource resource2 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                allergyFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(resource2);
        MedicalResourceId medicalResourceId2 = getResourceId(resource2);
        List<MedicalResourceId> medicalIdFilters = List.of(medicalResourceId1, medicalResourceId2);

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2));
        List<MedicalResource> updatedMedicalResource =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(upsertMedicalResourceInternalRequestUpdated1));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        medicalIdFilters);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(updatedResource1, resource2);
        assertThat(upsertedMedicalResources).containsExactly(originalResource, resource2);
        assertThat(updatedMedicalResource).containsExactly(updatedResource1);
        assertThat(indicesResult)
                .containsExactly(MEDICAL_RESOURCE_TYPE_IMMUNIZATION, MEDICAL_RESOURCE_TYPE_UNKNOWN);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResources_updateMultipleMedicalResources_success()
            throws JSONException {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        MedicalResource resource1 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest1 =
                makeUpsertRequest(resource1);
        FhirResource updatedFhirResource = getUpdatedImmunizationFhirResource();
        MedicalResource updatedResource1 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                updatedFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequestUpdated1 =
                makeUpsertRequest(updatedResource1);
        FhirResource allergyFhirResource = getFhirResourceAllergy();
        MedicalResource resource2 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                allergyFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest2 =
                makeUpsertRequest(resource2);
        FhirResource updatedAllergyFhirResource = getUpdatedAllergyFhirResource();
        MedicalResource updatedResource2 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                dataSource.getId(),
                                FHIR_VERSION_R4,
                                updatedAllergyFhirResource)
                        .build();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequestUpdated2 =
                makeUpsertRequest(updatedResource2);

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequest1,
                                upsertMedicalResourceInternalRequest2));
        List<MedicalResource> updatedMedicalResource =
                mMedicalResourceHelper.upsertMedicalResources(
                        List.of(
                                upsertMedicalResourceInternalRequestUpdated1,
                                upsertMedicalResourceInternalRequestUpdated2));
        List<MedicalResourceId> medicalIdFilters =
                List.of(getResourceId(resource1), getResourceId(resource2));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        medicalIdFilters);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(updatedMedicalResource)
                .containsExactly(updatedResource1, updatedResource2)
                .inOrder();
        assertThat(result).containsExactly(updatedResource1, updatedResource2);
        assertThat(indicesResult)
                .containsExactly(MEDICAL_RESOURCE_TYPE_IMMUNIZATION, MEDICAL_RESOURCE_TYPE_UNKNOWN);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResources_readByRequest_success() {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        List<MedicalResource> resources =
                createImmunizationMedicalResources(/* numOfResources= */ 6, dataSource.getId());
        List<UpsertMedicalResourceInternalRequest> upsertRequests =
                createUpsertMedicalResourceRequests(resources, dataSource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(upsertRequests);
        assertThat(upsertedMedicalResources).containsExactlyElementsIn(resources);

        ReadMedicalResourcesRequest readRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readRequest);
        String pageToken = result.getPageToken();
        assertThat(result.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(0), resources.get(1)));
        assertThat(pageToken)
                .isEqualTo(PhrPageTokenWrapper.of(readRequest, /* lastRowId= */ 2).encode());

        ReadMedicalResourcesRequest readRequest1 =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .setPageToken(pageToken)
                        .build();
        ReadMedicalResourcesInternalResponse result1 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readRequest1);
        String pageToken1 = result1.getPageToken();
        assertThat(result1.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(2), resources.get(3)));
        assertThat(pageToken1)
                .isEqualTo(PhrPageTokenWrapper.of(readRequest1, /* lastRowId= */ 4).encode());

        ReadMedicalResourcesRequest readRequest2 =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(2)
                        .setPageToken(pageToken1)
                        .build();
        ReadMedicalResourcesInternalResponse result2 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readRequest2);
        String pageToken2 = result2.getPageToken();
        assertThat(result2.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(4), resources.get(5)));
        assertThat(pageToken2).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResourcesReadByRequest_pageSizeLargerThanResources_success() {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        List<MedicalResource> resources =
                createImmunizationMedicalResources(/* numOfResources= */ 6, dataSource.getId());
        List<UpsertMedicalResourceInternalRequest> requests =
                createUpsertMedicalResourceRequests(resources, dataSource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(requests);
        ReadMedicalResourcesRequest readRequest =
                new ReadMedicalResourcesRequest.Builder(MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                        .setPageSize(10)
                        .build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        readRequest);

        assertThat(upsertedMedicalResources).containsExactlyElementsIn(resources);
        assertThat(result.getMedicalResources()).containsExactlyElementsIn(resources);
        assertThat(result.getPageToken()).isEqualTo(null);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readMedicalResourcedByRequest_invalidPageToken_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                                new ReadMedicalResourcesRequest.Builder(
                                                MEDICAL_RESOURCE_TYPE_IMMUNIZATION)
                                        .setPageToken(INVALID_PAGE_TOKEN)
                                        .build()));
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_noId_fails() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mMedicalResourceHelper.deleteMedicalResourcesByIds(List.of()));
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneIdNotPresent_succeeds() throws Exception {
        mMedicalResourceHelper.deleteMedicalResourcesByIds(
                List.of(
                        new MedicalResourceId(
                                DATA_SOURCE_ID,
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION)));
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneIdPresent_succeedsDeleting() throws Exception {
        MedicalDataSource dataSource = insertMedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        UpsertMedicalResourceInternalRequest upsertRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        dataSource.getId());
        mMedicalResourceHelper.upsertMedicalResources(List.of(upsertRequest));

        MedicalResourceId id =
                new MedicalResourceId(
                        dataSource.getId(), fhirResource.getType(), fhirResource.getId());
        mMedicalResourceHelper.deleteMedicalResourcesByIds(List.of(id));

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(id));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).isEmpty();
        assertThat(indicesResult).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneOfTwoSpecified_onlySpecifiedDeleted()
            throws Exception {
        MedicalDataSource dataSource = insertMedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource1 = getFhirResource();
        UpsertMedicalResourceInternalRequest medicalResource1 =
                makeUpsertRequest(
                        fhirResource1,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        dataSource.getId());
        FhirResource fhirResource2 = getFhirResourceAllergy();
        UpsertMedicalResourceInternalRequest medicalResource2 =
                makeUpsertRequest(
                        fhirResource2,
                        MEDICAL_RESOURCE_TYPE_UNKNOWN,
                        FHIR_VERSION_R4,
                        dataSource.getId());

        mMedicalResourceHelper.upsertMedicalResources(List.of(medicalResource1, medicalResource2));
        MedicalResourceId id1 =
                new MedicalResourceId(
                        dataSource.getId(), fhirResource1.getType(), fhirResource1.getId());
        MedicalResourceId id2 =
                new MedicalResourceId(
                        dataSource.getId(), fhirResource2.getType(), fhirResource2.getId());

        mMedicalResourceHelper.deleteMedicalResourcesByIds(List.of(id1));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(id1, id2));
        assertThat(result)
                .containsExactly(
                        new MedicalResource.Builder(
                                        MEDICAL_RESOURCE_TYPE_UNKNOWN,
                                        dataSource.getId(),
                                        FHIR_VERSION_R4,
                                        fhirResource2)
                                .build());
        assertThat(indicesResult).containsExactly(MEDICAL_RESOURCE_TYPE_UNKNOWN);
    }

    @Test
    public void getDeleteRequest_noIds_exceptions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceHelper.getDeleteRequest(Collections.emptyList()));
    }

    @Test
    public void getDeleteRequest_oneId_success() {
        MedicalResourceId medicalResourceId =
                new MedicalResourceId(DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, "anId");
        DeleteTableRequest request =
                MedicalResourceHelper.getDeleteRequest(ImmutableList.of(medicalResourceId));
        String hex = makeMedicalResourceHexString(medicalResourceId);

        assertThat(request.getDeleteCommand())
                .isEqualTo("DELETE FROM medical_resource_table WHERE uuid IN (" + hex + ")");
    }

    @Test
    public void getDeleteRequest_multipleId_success() {
        MedicalResourceId medicalResourceId1 =
                new MedicalResourceId(
                        DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, "Immunization1");
        MedicalResourceId medicalResourceId2 =
                new MedicalResourceId(
                        DIFFERENT_DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_UNKNOWN, "Allergy1");
        MedicalResourceId medicalResourceId3 =
                new MedicalResourceId(
                        DIFFERENT_DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_UNKNOWN, "Allergy2");
        String uuidHex1 = makeMedicalResourceHexString(medicalResourceId1);
        String uuidHex2 = makeMedicalResourceHexString(medicalResourceId2);
        String uuidHex3 = makeMedicalResourceHexString(medicalResourceId3);

        DeleteTableRequest request =
                MedicalResourceHelper.getDeleteRequest(
                        ImmutableList.of(
                                medicalResourceId1, medicalResourceId2, medicalResourceId3));

        assertThat(request.getIds()).isEqualTo(ImmutableList.of(uuidHex1, uuidHex2, uuidHex3));
        assertThat(request.getIdColumnName()).isEqualTo("uuid");
        assertThat(request.getDeleteCommand())
                .isEqualTo(
                        "DELETE FROM medical_resource_table WHERE uuid IN ("
                                + uuidHex1
                                + ", "
                                + uuidHex2
                                + ", "
                                + uuidHex3
                                + ")");
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByDataSources_noIds_succeeds() {
        mMedicalResourceHelper.deleteMedicalResourcesByDataSources(List.of());
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByDataSources_singleDataSource_succeeds() {
        // Create two datasources, with one resource each.
        MedicalDataSource dataSource1 = insertMedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 = insertMedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource1 = getFhirResource();
        FhirResource fhirResource2 = getFhirResourceAllergy();
        UpsertMedicalResourceInternalRequest dataSource1Resource1 =
                makeUpsertRequest(
                        fhirResource1,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        dataSource1.getId());
        UpsertMedicalResourceInternalRequest dataSource1Resource2 =
                makeUpsertRequest(
                        fhirResource2,
                        MEDICAL_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_VERSION_R4,
                        dataSource1.getId());
        UpsertMedicalResourceInternalRequest datasource2resource =
                makeUpsertRequest(
                        fhirResource2,
                        MEDICAL_RESOURCE_TYPE_UNKNOWN,
                        FHIR_VERSION_R4,
                        dataSource2.getId());
        mMedicalResourceHelper.upsertMedicalResources(
                List.of(dataSource1Resource1, dataSource1Resource2, datasource2resource));

        // Delete all of the data for just the first datasource
        mMedicalResourceHelper.deleteMedicalResourcesByDataSources(List.of(dataSource1.getId()));

        // Test that the data for data source 1 is gone, but 2 is still present
        MedicalResourceId datasource1Resource1Id =
                new MedicalResourceId(
                        dataSource1.getId(), fhirResource1.getType(), fhirResource1.getId());
        MedicalResourceId datasource1resource2Id =
                new MedicalResourceId(
                        dataSource1.getId(), fhirResource2.getType(), fhirResource2.getId());
        MedicalResourceId datasource2resourceId =
                new MedicalResourceId(
                        dataSource2.getId(), fhirResource2.getType(), fhirResource2.getId());
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(datasource1Resource1Id, datasource1resource2Id)))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(datasource2resourceId)))
                .hasSize(1);
    }

    /**
     * Returns a UUID for the given triple {@code resourceId}, {@code resourceType} and {@code
     * dataSourceId}.
     */
    private static String makeMedicalResourceHexString(MedicalResourceId medicalResourceId) {
        return getHexString(
                generateMedicalResourceUUID(
                        medicalResourceId.getFhirResourceId(),
                        medicalResourceId.getFhirResourceType(),
                        medicalResourceId.getDataSourceId()));
    }

    /**
     * Insert and return a {@link MedicalDataSource} where the display name, and URI will contain
     * the given name.
     */
    private MedicalDataSource insertMedicalDataSource(String name, String packageName) {
        String uri = String.format("%s/%s", DATA_SOURCE_FHIR_BASE_URI, name);
        String displayName = String.format("%s %s", DATA_SOURCE_DISPLAY_NAME, name);

        CreateMedicalDataSourceRequest createMedicalDataSourceRequest =
                new CreateMedicalDataSourceRequest.Builder(uri, displayName).build();
        return mMedicalDataSourceHelper.createMedicalDataSource(
                mContext, createMedicalDataSourceRequest, packageName);
    }

    private static MedicalResourceId getResourceId(MedicalResource resource1) {
        return new MedicalResourceId(
                resource1.getDataSourceId(),
                resource1.getFhirResource().getType(),
                resource1.getFhirResource().getId());
    }

    private MedicalResource upsertResource(MedicalResourceCreator creator, String dataSourceId) {
        MedicalResource medicalResource = creator.create(dataSourceId);
        return mMedicalResourceHelper
                .upsertMedicalResources(List.of(makeUpsertRequest(medicalResource)))
                .get(0);
    }

    private List<MedicalResource> upsertResources(
            MedicalResourcesCreator creator, int numOfResources, String dataSourceId) {
        List<MedicalResource> medicalResources = creator.create(numOfResources, dataSourceId);
        return mMedicalResourceHelper.upsertMedicalResources(
                medicalResources.stream()
                        .map(MedicalResourceHelperTest::makeUpsertRequest)
                        .toList());
    }

    private interface MedicalResourceCreator {
        MedicalResource create(String dataSourceId);
    }

    private interface MedicalResourcesCreator {
        List<MedicalResource> create(int num, String dataSourceId);
    }

    /** Returns a request to upsert the given {@link MedicalResource}. */
    private static UpsertMedicalResourceInternalRequest makeUpsertRequest(
            MedicalResource resource) {
        return makeUpsertRequest(
                resource.getFhirResource(),
                resource.getType(),
                resource.getFhirVersion(),
                resource.getDataSourceId());
    }

    /**
     * Returns a request to upsert the given {@link FhirResource}, along with required source
     * information.
     */
    private static UpsertMedicalResourceInternalRequest makeUpsertRequest(
            FhirResource resource,
            int medicalResourceType,
            FhirVersion fhirVersion,
            String datasourceId) {
        return new UpsertMedicalResourceInternalRequest()
                .setMedicalResourceType(medicalResourceType)
                .setFhirResourceId(resource.getId())
                .setFhirResourceType(resource.getType())
                .setFhirVersion(fhirVersion)
                .setData(resource.getData())
                .setDataSourceId(datasourceId);
    }

    private List<Integer> readEntriesInMedicalResourceIndicesTable() {
        List<Integer> medicalResourceTypes = new ArrayList<>();
        ReadTableRequest readTableRequest = new ReadTableRequest(getTableName());
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            if (cursor.moveToFirst()) {
                do {
                    medicalResourceTypes.add(
                            getCursorInt(cursor, getMedicalResourceTypeColumnName()));
                } while (cursor.moveToNext());
            }
            return medicalResourceTypes;
        }
    }

    /**
     * Creates a list of {@link UpsertMedicalResourceInternalRequest}s for the given list of {@link
     * MedicalResource}s and {@code dataSourceId}.
     */
    private static List<UpsertMedicalResourceInternalRequest> createUpsertMedicalResourceRequests(
            List<MedicalResource> medicalResources, String dataSourceId) {
        List<UpsertMedicalResourceInternalRequest> requests = new ArrayList<>();
        for (MedicalResource medicalResource : medicalResources) {
            FhirResource fhirResource = medicalResource.getFhirResource();
            UpsertMedicalResourceInternalRequest request =
                    new UpsertMedicalResourceInternalRequest()
                            .setMedicalResourceType(medicalResource.getType())
                            .setFhirResourceId(fhirResource.getId())
                            .setFhirResourceType(fhirResource.getType())
                            .setFhirVersion(medicalResource.getFhirVersion())
                            .setData(fhirResource.getData())
                            .setDataSourceId(dataSourceId);
            requests.add(request);
        }
        return requests;
    }

    private static <T> List<T> joinLists(List<T>... lists) {
        return Stream.of(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
