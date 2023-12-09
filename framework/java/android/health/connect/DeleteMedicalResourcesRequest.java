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

package android.health.connect;

import static android.health.connect.datatypes.MedicalResource.validateMedicalResourceType;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.MedicalResource.MedicalResourceType;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Request to delete Medical resources using {@link HealthConnectManager#deleteMedicalResources}.
 *
 * <p>Each field in the request acts as a cumulative filter. So if a set of data sources and a set
 * of types are specified, then only resources which are both from data sources in the given set and
 * of types in the given set are deleted.
 *
 * <p>At least one filter must be specified - you cannot construct a request to say delete
 * everything. And for any given requirement set, it must be a non-empty set (empty means the filter
 * does not exist).
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class DeleteMedicalResourcesRequest implements Parcelable {
    @NonNull private final Set<String> mDataSourceIds;
    @NonNull @MedicalResourceType private final Set<Integer> mMedicalResourceTypes;

    private DeleteMedicalResourcesRequest(
            @NonNull Set<String> dataSourceIds,
            @NonNull @MedicalResourceType Set<Integer> medicalResourceTypes) {
        if (dataSourceIds.isEmpty() && medicalResourceTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "No restrictions specified for delete. The request must restrict by data source"
                            + " or resource type");
        }
        medicalResourceTypes.forEach(MedicalResource::validateMedicalResourceType);
        mDataSourceIds = dataSourceIds;
        mMedicalResourceTypes = medicalResourceTypes;
    }

    /**
     * Constructs this object with the data present in {@code parcel}. It should be in the same
     * order as {@link DeleteMedicalResourcesRequest#writeToParcel}.
     */
    private DeleteMedicalResourcesRequest(@NonNull Parcel in) {
        requireNonNull(in);
        ArrayList<String> dataSourceIdList = requireNonNull(in.createStringArrayList());
        int[] resourceTypes = requireNonNull(in.createIntArray());
        if (dataSourceIdList.isEmpty() && resourceTypes.length == 0) {
            throw new IllegalArgumentException("Empty data sources and resource types in parcel");
        }
        mDataSourceIds = new HashSet<>(dataSourceIdList);
        mMedicalResourceTypes = new HashSet<>();
        for (int resourceType : resourceTypes) {
            validateMedicalResourceType(resourceType);
            mMedicalResourceTypes.add(resourceType);
        }
    }

    @NonNull
    public static final Creator<DeleteMedicalResourcesRequest> CREATOR =
            new Creator<>() {
                @Override
                public DeleteMedicalResourcesRequest createFromParcel(Parcel in) {
                    return new DeleteMedicalResourcesRequest(in);
                }

                @Override
                public DeleteMedicalResourcesRequest[] newArray(int size) {
                    return new DeleteMedicalResourcesRequest[size];
                }
            };

    /**
     * Gets the ids for the data sources that are being requested to delete.
     *
     * <p>These ids should come from {@link HealthConnectManager#createMedicalDataSource}, or other
     * {@link HealthConnectManager} data source methods.
     *
     * <p>If the set is empty it means resources from any data source should be deleted.
     */
    @NonNull
    public Set<String> getDataSourceIds() {
        return new HashSet<>(mDataSourceIds);
    }

    /**
     * Gets the Medical resource types that should be deleted.
     *
     * <p>If the set is empty it means resources of all types should be deleted.
     */
    @NonNull
    @MedicalResourceType
    public Set<Integer> getMedicalResourceTypes() {
        return new HashSet<>(mMedicalResourceTypes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(new ArrayList<>(mDataSourceIds));
        dest.writeIntArray(mMedicalResourceTypes.stream().mapToInt(Integer::intValue).toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteMedicalResourcesRequest that)) return false;
        return mDataSourceIds.equals(that.mDataSourceIds)
                && mMedicalResourceTypes.equals(that.mMedicalResourceTypes);
    }

    @Override
    public int hashCode() {
        return hash(mDataSourceIds, mMedicalResourceTypes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("dataSourceIds=").append(mDataSourceIds);
        sb.append(",medicalResourceTypes=").append(mMedicalResourceTypes);
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link DeleteMedicalResourcesRequest}. */
    public static final class Builder {
        private final Set<String> mDataSourceIds = new HashSet<>();
        @MedicalResourceType private final Set<Integer> mMedicalResourceTypes = new HashSet<>();

        /** Constructs a new {@link Builder} with no data sources set. */
        public Builder() {}

        /** Constructs a new {@link Builder} copying all settings from {@code other}. */
        public Builder(@NonNull Builder other) {
            mDataSourceIds.addAll(other.mDataSourceIds);
            mMedicalResourceTypes.addAll(other.mMedicalResourceTypes);
        }

        /** Add the data source ID to request to delete. */
        @NonNull
        public Builder addDataSourceId(@NonNull String dataSourceId) {
            mDataSourceIds.add(requireNonNull(dataSourceId));
            return this;
        }

        /** Add the medical resource type to request to delete. */
        @NonNull
        public Builder addMedicalResourceType(@MedicalResourceType int resourceType) {
            validateMedicalResourceType(resourceType);
            mMedicalResourceTypes.add(resourceType);
            return this;
        }

        /** Clears all data source IDs. */
        @NonNull
        public Builder clearDataSourceIds() {
            mDataSourceIds.clear();
            return this;
        }

        /** Clears all medical resource types. */
        @NonNull
        public Builder clearMedicalResourceTypes() {
            mMedicalResourceTypes.clear();
            return this;
        }

        /**
         * Builds a {@link DeleteMedicalResourcesRequest} from this Builder.
         *
         * @throws IllegalArgumentException if no data source ids or medical resource types have
         *     been added.
         */
        @NonNull
        public DeleteMedicalResourcesRequest build() {
            return new DeleteMedicalResourcesRequest(mDataSourceIds, mMedicalResourceTypes);
        }
    }
}