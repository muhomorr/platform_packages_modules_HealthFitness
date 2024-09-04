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

package android.healthconnect.cts;

import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME_EXCEEDED_CHARS;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME_MAX_CHARS;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI_EXCEEDED_CHARS;
import static android.healthconnect.cts.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI_MAX_CHARS;
import static android.healthconnect.cts.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_BASE_URI;
import static android.healthconnect.cts.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.PhrDataFactory.getCreateMedicalDataSourceRequestBuilder;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.CreateMedicalDataSourceRequest;
import android.net.Uri;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RequiresFlagsEnabled(Flags.FLAG_PERSONAL_HEALTH_RECORD)
@RunWith(AndroidJUnit4.class)
public class CreateMedicalDataSourceRequestTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_constructor() {
        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI, DATA_SOURCE_DISPLAY_NAME)
                        .build();

        assertThat(request.getFhirBaseUri()).isEqualTo(DATA_SOURCE_FHIR_BASE_URI);
        assertThat(request.getDisplayName()).isEqualTo(DATA_SOURCE_DISPLAY_NAME);
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_setAllFields() {
        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(Uri.EMPTY, "")
                        .setFhirBaseUri(DATA_SOURCE_FHIR_BASE_URI)
                        .setDisplayName(DATA_SOURCE_DISPLAY_NAME)
                        .build();

        assertThat(request.getFhirBaseUri()).isEqualTo(DATA_SOURCE_FHIR_BASE_URI);
        assertThat(request.getDisplayName()).isEqualTo(DATA_SOURCE_DISPLAY_NAME);
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_fromExistingBuilder() {
        CreateMedicalDataSourceRequest.Builder original =
                getCreateMedicalDataSourceRequestBuilder();

        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(original).build();

        assertThat(request).isEqualTo(original.build());
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_fromExistingInstance() {
        CreateMedicalDataSourceRequest original = getCreateMedicalDataSourceRequest();

        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(original).build();

        assertThat(request).isEqualTo(original);
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_maxDisplayNameCharsExceeded_throws() {
        CreateMedicalDataSourceRequest.Builder requestBuilder =
                new CreateMedicalDataSourceRequest.Builder(
                        DATA_SOURCE_FHIR_BASE_URI, DATA_SOURCE_DISPLAY_NAME_EXCEEDED_CHARS);

        var thrown = assertThrows(IllegalArgumentException.class, () -> requestBuilder.build());
        assertThat(thrown).hasMessageThat().contains("Display name cannot be longer than");
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_emptyDisplayName_throws() {
        CreateMedicalDataSourceRequest.Builder requestBuilder =
                new CreateMedicalDataSourceRequest.Builder(DATA_SOURCE_FHIR_BASE_URI, "");

        var thrown = assertThrows(IllegalArgumentException.class, () -> requestBuilder.build());
        assertThat(thrown).hasMessageThat().contains("Display name cannot be empty");
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_maxBaseUriCharsExceeded_throws() {
        CreateMedicalDataSourceRequest.Builder requestBuilder =
                new CreateMedicalDataSourceRequest.Builder(
                        DATA_SOURCE_FHIR_BASE_URI_EXCEEDED_CHARS, DATA_SOURCE_DISPLAY_NAME);

        var thrown = assertThrows(IllegalArgumentException.class, () -> requestBuilder.build());
        assertThat(thrown).hasMessageThat().contains("Fhir base uri cannot be longer than");
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_emptyBaseUri_throws() {
        CreateMedicalDataSourceRequest.Builder requestBuilder =
                new CreateMedicalDataSourceRequest.Builder(Uri.EMPTY, DATA_SOURCE_DISPLAY_NAME);

        var thrown = assertThrows(IllegalArgumentException.class, () -> requestBuilder.build());
        assertThat(thrown).hasMessageThat().contains("Fhir base uri cannot be empty");
    }

    @Test
    public void testCreateMedicalDataSourceRequestBuilder_maxCharacterLimits_succeeds() {
        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI_MAX_CHARS,
                                DATA_SOURCE_DISPLAY_NAME_MAX_CHARS)
                        .build();

        assertThat(request.getFhirBaseUri()).isEqualTo(DATA_SOURCE_FHIR_BASE_URI_MAX_CHARS);
        assertThat(request.getDisplayName()).isEqualTo(DATA_SOURCE_DISPLAY_NAME_MAX_CHARS);
    }

    @Test
    public void testCreateMedicalDataSourceRequest_toString() {
        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI, DATA_SOURCE_DISPLAY_NAME)
                        .build();
        String expectedPropertiesString =
                String.format(
                        "fhirBaseUri=%s,displayName=%s",
                        DATA_SOURCE_FHIR_BASE_URI, DATA_SOURCE_DISPLAY_NAME);

        assertThat(request.toString())
                .isEqualTo(
                        String.format(
                                "CreateMedicalDataSourceRequest{%s}", expectedPropertiesString));
    }

    @Test
    public void testCreateMedicalDataSourceRequest_equals() {
        CreateMedicalDataSourceRequest request1 = getCreateMedicalDataSourceRequest();
        CreateMedicalDataSourceRequest request2 = getCreateMedicalDataSourceRequest();

        assertThat(request1.equals(request2)).isTrue();
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void testCreateMedicalDataSourceRequest_equals_comparesAllValues() {
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();
        CreateMedicalDataSourceRequest requestDifferentBaseUri =
                new CreateMedicalDataSourceRequest.Builder(request)
                        .setFhirBaseUri(DIFFERENT_DATA_SOURCE_BASE_URI)
                        .build();
        CreateMedicalDataSourceRequest requestDifferentDisplayName =
                new CreateMedicalDataSourceRequest.Builder(request)
                        .setDisplayName(DIFFERENT_DATA_SOURCE_DISPLAY_NAME)
                        .build();

        assertThat(requestDifferentBaseUri.equals(request)).isFalse();
        assertThat(requestDifferentDisplayName.equals(request)).isFalse();
        assertThat(requestDifferentBaseUri.hashCode()).isNotEqualTo(request.hashCode());
        assertThat(requestDifferentDisplayName.hashCode()).isNotEqualTo(request.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        CreateMedicalDataSourceRequest original = getCreateMedicalDataSourceRequest();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        CreateMedicalDataSourceRequest restored =
                CreateMedicalDataSourceRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testWriteToParcelThenRestore_displayNameExceedsCharLimit_throws() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(DATA_SOURCE_FHIR_BASE_URI, 0);
        parcel.writeString(DATA_SOURCE_DISPLAY_NAME_EXCEEDED_CHARS);
        parcel.setDataPosition(0);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> CreateMedicalDataSourceRequest.CREATOR.createFromParcel(parcel));
        assertThat(thrown).hasMessageThat().contains("Display name cannot be longer than");
    }

    @Test
    public void testWriteToParcelThenRestore_fhirBaseUriExceedsCharLimit_throws() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(DATA_SOURCE_FHIR_BASE_URI_EXCEEDED_CHARS, 0);
        parcel.writeString(DATA_SOURCE_DISPLAY_NAME);
        parcel.setDataPosition(0);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> CreateMedicalDataSourceRequest.CREATOR.createFromParcel(parcel));
        assertThat(thrown).hasMessageThat().contains("Fhir base uri cannot be longer than");
    }
}
