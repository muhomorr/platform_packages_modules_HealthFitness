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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.health.connect.Constants.MAXIMUM_ALLOWED_CURSOR_COUNT;

import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_UNIQUE_NON_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorUUID;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.datatypes.MedicalDataSource;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for MedicalDataSource.
 *
 * @hide
 */
public class MedicalDataSourceHelper {
    @VisibleForTesting
    static final String MEDICAL_DATA_SOURCE_TABLE_NAME = "medical_data_source_table";

    @VisibleForTesting static final String DISPLAY_NAME_COLUMN_NAME = "display_name";
    @VisibleForTesting static final String FHIR_BASE_URI_COLUMN_NAME = "fhir_base_uri";
    @VisibleForTesting static final String PACKAGE_NAME_COLUMN_NAME = "package_name";
    private static final List<Pair<String, Integer>> UNIQUE_COLUMNS_INFO =
            List.of(new Pair<>(UUID_COLUMN_NAME, UpsertTableRequest.TYPE_BLOB));

    @NonNull
    public static String getMainTableName() {
        return MEDICAL_DATA_SOURCE_TABLE_NAME;
    }

    // TODO(b/344781394): Remove the package_name column and add app_info_id column once the table
    // is created with a foreign key to the application_info_id_table.
    @NonNull
    private static List<Pair<String, String>> getColumnInfo() {
        return List.of(
                Pair.create(PRIMARY_COLUMN_NAME, PRIMARY),
                Pair.create(PACKAGE_NAME_COLUMN_NAME, TEXT_NOT_NULL),
                Pair.create(DISPLAY_NAME_COLUMN_NAME, TEXT_NOT_NULL),
                Pair.create(FHIR_BASE_URI_COLUMN_NAME, TEXT_NOT_NULL),
                Pair.create(UUID_COLUMN_NAME, BLOB_UNIQUE_NON_NULL));
    }

    // TODO(b/344781394): Add the foreign key to the application_info_table and the relevant logic
    // to populate that when creating a {@link MedicalDataSource} row.
    @NonNull
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(MEDICAL_DATA_SOURCE_TABLE_NAME, getColumnInfo());
    }

    /** Creates the medical_data_source table. */
    public static void onInitialUpgrade(@NonNull SQLiteDatabase db) {
        createTable(db, getCreateTableRequest());
    }

    /** Creates {@link ReadTableRequest} for the given list of {@code ids}. */
    @NonNull
    public static ReadTableRequest getReadTableRequest(@NonNull List<String> ids) {
        return new ReadTableRequest(getMainTableName())
                .setWhereClause(getReadTableWhereClause(ids));
    }

    @NonNull
    private static WhereClauses getReadTableWhereClause(@NonNull List<String> ids) {
        List<UUID> uuids = ids.stream().map(UUID::fromString).toList();
        return new WhereClauses(AND)
                .addWhereInClauseWithoutQuotes(
                        UUID_COLUMN_NAME, StorageUtils.getListOfHexStrings(uuids));
    }

    /**
     * Returns List of {@link MedicalDataSource}s from the cursor. If the cursor contains more than
     * {@link MAXIMUM_ALLOWED_CURSOR_COUNT} data sources, it throws {@link
     * IllegalArgumentException}.
     */
    @NonNull
    private static List<MedicalDataSource> getMedicalDataSources(@NonNull Cursor cursor) {
        if (cursor.getCount() > MAXIMUM_ALLOWED_CURSOR_COUNT) {
            throw new IllegalArgumentException(
                    "Too many data sources in the cursor. Max allowed: "
                            + MAXIMUM_ALLOWED_CURSOR_COUNT);
        }
        List<MedicalDataSource> medicalDataSources = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                medicalDataSources.add(getMedicalDataSource(cursor));
            } while (cursor.moveToNext());
        }
        return medicalDataSources;
    }

    @NonNull
    private static MedicalDataSource getMedicalDataSource(@NonNull Cursor cursor) {
        return new MedicalDataSource.Builder(
                        /* id= */ getCursorUUID(cursor, UUID_COLUMN_NAME).toString(),
                        /* packageName= */ getCursorString(cursor, PACKAGE_NAME_COLUMN_NAME),
                        /* fhirBaseUri= */ getCursorString(cursor, FHIR_BASE_URI_COLUMN_NAME),
                        /* displayName= */ getCursorString(cursor, DISPLAY_NAME_COLUMN_NAME))
                .build();
    }

    /**
     * Inserts the {@link MedicalDataSource} created from the given {@link
     * CreateMedicalDataSourceRequest} and {@code packageName} into the HealthConnect database.
     *
     * @param request a {@link CreateMedicalDataSourceRequest}.
     * @param packageName is the package name of the application wanting to create a {@link
     *     MedicalDataSource}.
     * @return The {@link MedicalDataSource} created and inserted into the database.
     */
    @NonNull
    public static MedicalDataSource createMedicalDataSource(
            @NonNull CreateMedicalDataSourceRequest request, @NonNull String packageName) {
        // TODO(b/344781394): Add support for access logs.
        UUID dataSourceUuid = UUID.randomUUID();
        UpsertTableRequest upsertTableRequest =
                getUpsertTableRequest(dataSourceUuid, request, packageName);
        TransactionManager.getInitialisedInstance().insert(upsertTableRequest);
        return buildMedicalDataSource(dataSourceUuid, request, packageName);
    }

    /**
     * Reads the {@link MedicalDataSource}s stored in the HealthConnect database using the given
     * list of {@code ids}.
     *
     * @param ids a list of {@link MedicalDataSource} ids.
     * @return List of {@link MedicalDataSource}s read from medical_data_source table based on ids.
     */
    @NonNull
    public static List<MedicalDataSource> getMedicalDataSources(@NonNull List<String> ids)
            throws SQLiteException {
        ReadTableRequest readTableRequest = getReadTableRequest(ids);
        try (Cursor cursor = TransactionManager.getInitialisedInstance().read(readTableRequest)) {
            return getMedicalDataSources(cursor);
        }
    }

    /**
     * Creates {@link UpsertTableRequest} for the given {@link CreateMedicalDataSourceRequest} and
     * {@code packageName}.
     */
    @NonNull
    public static UpsertTableRequest getUpsertTableRequest(
            @NonNull UUID uuid,
            @NonNull CreateMedicalDataSourceRequest createMedicalDataSourceRequest,
            @NonNull String packageName) {
        ContentValues contentValues =
                getContentValues(uuid, createMedicalDataSourceRequest, packageName);
        return new UpsertTableRequest(getMainTableName(), contentValues, UNIQUE_COLUMNS_INFO);
    }

    /**
     * Creates a {@link MedicalDataSource} for the given {@code uuid}, {@link
     * CreateMedicalDataSourceRequest} and the {@code packageName}.
     */
    @NonNull
    public static MedicalDataSource buildMedicalDataSource(
            @NonNull UUID uuid,
            @NonNull CreateMedicalDataSourceRequest request,
            @NonNull String packageName) {
        return new MedicalDataSource.Builder(
                        uuid.toString(),
                        packageName,
                        request.getFhirBaseUri(),
                        request.getDisplayName())
                .build();
    }

    @NonNull
    private static ContentValues getContentValues(
            @NonNull UUID uuid,
            @NonNull CreateMedicalDataSourceRequest createMedicalDataSourceRequest,
            @NonNull String packageName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UUID_COLUMN_NAME, StorageUtils.convertUUIDToBytes(uuid));
        contentValues.put(
                DISPLAY_NAME_COLUMN_NAME, createMedicalDataSourceRequest.getDisplayName());
        contentValues.put(
                FHIR_BASE_URI_COLUMN_NAME, createMedicalDataSourceRequest.getFhirBaseUri());
        contentValues.put(PACKAGE_NAME_COLUMN_NAME, packageName);
        return contentValues;
    }
}