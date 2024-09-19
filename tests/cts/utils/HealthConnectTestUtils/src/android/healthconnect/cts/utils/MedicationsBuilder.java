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

package android.healthconnect.cts.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test helper class for making medication related FHIR data, including <a
 * href="https://www.hl7.org/fhir/medication.html">Medication</a>, <a
 * href="https://www.hl7.org/fhir/medicationrequest.html">MedicationRequest</a> and <a
 * href="https://www.hl7.org/fhir/medicationstatement.html">MedicationStatement</a>.
 */
public class MedicationsBuilder {

    private abstract static class ResourceBuilder<T extends ResourceBuilder<T>> {
        private final JSONObject mFhir;

        private ResourceBuilder(String json) {
            try {
                this.mFhir = new JSONObject(json);
            } catch (JSONException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Help the subclasses return the correct type.
         *
         * @return the current object, correctly typed.
         */
        protected abstract T returnThis();

        /**
         * Set the FHIR id for the Observation.
         *
         * @return this Builder.
         */
        public T setId(String id) {
            return set("id", id);
        }

        /**
         * Sets an arbitrary String or JSON Object element in the observation FHIR.
         *
         * @param field the element to set.
         * @param value the value to set
         * @return this builder
         */
        public T set(String field, Object value) {
            try {
                mFhir.put(field, value);
            } catch (JSONException e) {
                throw new IllegalArgumentException(e);
            }
            return returnThis();
        }

        /** Returns the current state of this builder as a JSON FHIR string. */
        public String toJson() {
            try {
                return mFhir.toString(/* indentSpaces= */ 2);
            } catch (JSONException e) {
                // Should never happen, but JSONException is declared, and is a checked exception.
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Builder class for creating FHIR <a
     * href="https://www.hl7.org/fhir/medication.html">Medications</a>
     */
    public static class MedicationBuilder extends ResourceBuilder<MedicationBuilder> {

        private static final String DEFAULT_MEDICATION_JSON =
                "{  \"resourceType\" : \"Medication\",  "
                        + "\"id\" : \"med0311\","
                        + "\"contained\" : [{"
                        + "  \"resourceType\" : \"Substance\",    "
                        + "  \"id\" : \"sub03\",    "
                        + "\"instance\" : false,"
                        + "\"code\" : {"
                        + "  \"concept\" : {"
                        + "    \"coding\" : [{         "
                        + " \"system\" : \"http://snomed.info/sct\","
                        + "\"code\" : \"116602009\","
                        + "\"display\" : \"Prednisone (substance)\" "
                        + " }]"
                        + "}"
                        + "} "
                        + "}],"
                        + "\"code\" : {"
                        + "  \"coding\" : [{"
                        + "    \"system\" :\"http://snomed.info/sct\","
                        + "    \"code\" : \"373994007\","
                        + "    \"display\": \"Prednisone 5mg tablet (Product)\""
                        + "   }]"
                        + "},"
                        + "\"doseForm\" : {"
                        + "  \"coding\" : [{"
                        + "    \"system\" : \"http://snomed.info/sct\","
                        + "    \"code\" : \"385055001\","
                        + "    \"display\" : \"Tablet dose form (qualifier value)\" "
                        + "  }]"
                        + "},"
                        + "\"ingredient\" : [{"
                        + "  \"item\" : {"
                        + "    \"reference\" : {"
                        + "      \"reference\" : \"#sub03\""
                        + "     }"
                        + "  },"
                        + "  \"strengthRatio\" : {"
                        + "     \"numerator\" : {"
                        + "        \"value\" : 5,"
                        + "        \"system\" :\"http://unitsofmeasure.org\","
                        + "        \"code\" : \"mg\""
                        + "      },"
                        + "      \"denominator\" : {"
                        + "         \"value\" : 1,"
                        + "         \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm\","
                        + " \"code\" : \"TAB\""
                        + "}"
                        + "}"
                        + "}]}";

        private static final String TYLENOL =
                "{"
                        + "\"resourceType\" : \"Medication\","
                        + "    \"id\" : \"med0309\","
                        + "    \"code\" : {"
                        + "    \"coding\" : [{"
                        + "        \"system\" : \"http://hl7.org/fhir/sid/ndc\","
                        + "    \"code\" : \"50580-608-02\","
                        + "        \"display\" : \"Tylenol PM\""
                        + "    }]"
                        + "  },"
                        + "\"doseForm\" : {"
                        + "  \"coding\" : [{"
                        + "    \"system\" : \"http://snomed.info/sct\","
                        + "    \"code\" : \"385057009\","
                        + "    \"display\" : \"Film-coated tablet (qualifier value)\""
                        + "   }]"
                        + " },"
                        + " \"ingredient\" : [{"
                        + "   \"item\" : {"
                        + "     \"concept\" : {"
                        + "       \"coding\" : [{"
                        + "          \"system\" : \"http://www.nlm.nih.gov/research/umls/rxnorm\","
                        + "          \"code\" : \"315266\","
                        + "          \"display\" : \"Acetaminophen 500 MG\""
                        + "        }]"
                        + "      }"
                        + "    },"
                        + "    \"strengthRatio\" : {"
                        + "      \"numerator\" : {"
                        + "       \"value\" : 500,"
                        + "       \"system\" : \"http://unitsofmeasure.org\","
                        + "       \"code\" : \"mg\""
                        + "      },"
                        + "      \"denominator\" : {"
                        + "        \"value\" : 1,"
                        + "        \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm\","
                        + "         \"code\" : \"TAB\""
                        + "        }"
                        + "      }"
                        + "    },"
                        + "    {"
                        + "      \"item\" : {"
                        + "        \"concept\" : {"
                        + "          \"coding\" : [{"
                        + "            \"system\" :"
                        + " \"http://www.nlm.nih.gov/research/umls/rxnorm\","
                        + "            \"code\" : \"901813\","
                        + "            \"display\" : \"Diphenhydramine Hydrochloride 25 mg\""
                        + "          }]"
                        + "        } "
                        + "     },"
                        + "     \"strengthRatio\" : {"
                        + "       \"numerator\" : {"
                        + "          \"value\" : 25,"
                        + "          \"system\" : \"http://unitsofmeasure.org\","
                        + "          \"code\" : \"mg\""
                        + "        },"
                        + "      \"denominator\" : {"
                        + "          \"value\" : 1,"
                        + "          \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm\","
                        + "          \"code\" : \"TAB\""
                        + "        }"
                        + "      }"
                        + "    }],"
                        + "    \"batch\" : {     "
                        + "      \"lotNumber\" : \"9494788\","
                        + "      \"expirationDate\" : \"2017-05-22\""
                        + "    } "
                        + " }";

        public MedicationBuilder() {
            super(DEFAULT_MEDICATION_JSON);
        }

        /**
         * Set this medication to be Tylenol.
         *
         * @return this builder
         */
        public MedicationBuilder setTylenol() {
            try {
                JSONObject tylenol = new JSONObject(TYLENOL);
                Iterator<String> keys = tylenol.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    set(key, tylenol.get(key));
                }
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        @Override
        protected MedicationBuilder returnThis() {
            return this;
        }
    }

    /**
     * Builder class for creating FHIR <a
     * href="https://www.hl7.org/fhir/medicationstatement.html">MedicationStatements</a>
     */
    public static class MedicationStatementR4Builder
            extends ResourceBuilder<MedicationStatementR4Builder> {
        private static final String DEFAULT_MEDICATION_STATEMENT_JSON =
                "{  "
                        + "\"resourceType\" : \"MedicationStatement\","
                        + "\"id\" : \"example001\", "
                        + " \"contained\" : [],"
                        + " \"identifier\" : [{"
                        + "    \"use\" : \"official\","
                        + " \"system\" : \"http://www.bmc.nl/portal/medstatements\","
                        + "    \"value\" : \"12345689\""
                        + "  }],"
                        + "  \"status\" : \"recorded\","
                        + "  \"category\" : [{   "
                        + " \"coding\" : [{"
                        + "      \"system\" :"
                        + "\"http://terminology.hl7.org/CodeSystem/"
                        + "medication-statement-category\","
                        + "      \"code\" : \"inpatient\","
                        + "      \"display\" : \"Inpatient\""
                        + "    }]"
                        + "  }], "
                        + " \"subject\" : {"
                        + "    \"reference\" : \"Patient/pat1\","
                        + "    \"display\" : \"Donald Duck\""
                        + "  },  "
                        + "\"effectiveDateTime\" : \"2015-01-23\","
                        + " \"dateAsserted\" : \"2015-02-22\","
                        + "  \"informationSource\" : [{"
                        + " \"reference\" : \"Patient/pat1\","
                        + "    \"display\" : \"Donald Duck\""
                        + "  }], "
                        + " \"derivedFrom\" : [{"
                        + "    \"reference\" : \"MedicationRequest/medrx002\""
                        + "  }],"
                        + "  \"reason\" : [{"
                        + "    \"concept\" : {"
                        + "      \"coding\" : [{"
                        + "       \"system\" : \"http://snomed.info/sct\","
                        + "        \"code\" : \"32914008\","
                        + " \"display\" : \"Restless Legs\""
                        + "      }]"
                        + "    }"
                        + "  }],"
                        + "  \"note\" : [{"
                        + " \"text\" : \"Patient indicates they miss the occasional dose\""
                        + "}], "
                        + " \"dosage\" : [{"
                        + "    \"sequence\" : 1,"
                        + "    \"text\" : \"1-2 tablets once daily at bedtime as needed for "
                        + "       restless legs\","
                        + "    \"additionalInstruction\" : [{"
                        + "      \"text\" : \"Taking at bedtime\""
                        + "    }],"
                        + "    \"timing\" : {     "
                        + " \"repeat\" : {"
                        + "        \"frequency\" : 1,"
                        + "        \"period\" : 1,"
                        + "        \"periodUnit\" : \"d\""
                        + "      }"
                        + "    },"
                        + "    \"asNeededFor\" : [{"
                        + "      \"coding\" : [{"
                        + "        \"system\" : \"http://snomed.info/sct\","
                        + "        \"code\" :\"32914008\","
                        + "        \"display\" : \"Restless Legs\""
                        + "      }]"
                        + "    }],"
                        + " \"route\" : {"
                        + "      \"coding\" : [{"
                        + "        \"system\" :\"http://snomed.info/sct\","
                        + "        \"code\" : \"26643006\","
                        + "       "
                        + " \"display\" : \"Oral Route\""
                        + "      }]"
                        + "    },"
                        + "    \"doseAndRate\" : [{"
                        + "      \"type\" : {"
                        + "        \"coding\" : [{"
                        + "          \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/dose-rate-type\","
                        + "          \"code\" : \"ordered\","
                        + "          \"display\" : \"Ordered\""
                        + "        }]"
                        + "      },"
                        + " \"doseRange\" : {"
                        + "        \"low\" : {"
                        + "          \"value\" : 1,         "
                        + "          \"unit\" : \"TAB\","
                        + "          \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm\","
                        + " \"code\" : \"TAB\""
                        + "        },"
                        + "        \"high\" : {"
                        + "          \"value\" : 2,"
                        + "          \"unit\" : \"TAB\","
                        + "          \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm\","
                        + "          \"code\" : \"TAB\""
                        + "        }"
                        + "      }"
                        + "    }]"
                        + "  }]}";

        public MedicationStatementR4Builder() {
            super(DEFAULT_MEDICATION_STATEMENT_JSON);
            setContainedMedication(medication().setTylenol());
        }

        /**
         * Set this medication statement to contain the given medication.
         *
         * @param medication the medication to be contained (may be modified).
         * @return this builder
         */
        public MedicationStatementR4Builder setContainedMedication(MedicationBuilder medication) {
            String containedId = "med123";
            try {
                set(
                        "contained",
                        new JSONArray(
                                List.of(new JSONObject(medication.setId(containedId).toJson()))));
                set("medicationReference", new JSONObject(Map.of("reference", "#" + containedId)));
            } catch (JSONException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        @Override
        protected MedicationStatementR4Builder returnThis() {
            return this;
        }
    }

    public static class MedicationRequestBuilder extends ResourceBuilder<MedicationRequestBuilder> {

        private static final String DEFAULT_REQUEST_JSON =
                "{  \"resourceType\": \"MedicationRequest\","
                        + "  \"id\": \"medrx0331\","
                        + " \"contained\": [{"
                        + "\"resourceType\": \"Medication\","
                        + "\"id\": \"med0350\","
                        + "\"code\": {"
                        + "  \"coding\": [{"
                        + "    \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\","
                        + "    \"code\": \"114194\","
                        + "    \"display\": \"Warfarin Sodium\""
                        + "   }]"
                        + "},"
                        + "\"form\": {"
                        + "  \"coding\": [{"
                        + "    \"system\": \"http://snomed.info/sct\","
                        + "    \"code\": \"421026006\","
                        + "    \"display\": \"Oral Tablet\""
                        + "   }]"
                        + "  }"
                        + "}],"
                        + "\"identifier\": [{"
                        + "  \"use\": \"official\","
                        + "  \"system\": \"http://www.bmc.nl/portal/prescriptions\","
                        + "  \"value\": \"12345689\""
                        + "}],"
                        + "\"status\": \"active\","
                        + "\"intent\": \"order\", "
                        + "\"medicationReference\": {"
                        + "  \"reference\": \"#med0350\"  "
                        + "},"
                        + "\"subject\": {"
                        + "  \"reference\": \"Patient/pat1\","
                        + "  \"display\": \"Donald Duck\""
                        + "}, "
                        + "\"authoredOn\": \"2015-01-15\","
                        + "\"requester\": {"
                        + "  \"reference\": \"Practitioner/f007\","
                        + "  \"display\": \"Patrick Pump\""
                        + "},"
                        + " \"dosageInstruction\": [{"
                        + "   \"sequence\": 1,"
                        + "   \"text\": \"7mg once daily\","
                        + "   \"timing\": {"
                        + "     \"repeat\": {"
                        + "       \"frequency\": 1,"
                        + "       \"period\": 1,"
                        + "       \"periodUnit\": \"d\""
                        + "     }"
                        + "   },"
                        + "   \"doseAndRate\": [{"
                        + "     \"type\": {"
                        + "       \"coding\": [{"
                        + "\"system\": \"http://terminology.hl7.org/CodeSystem/dose-rate-type\","
                        + "           \"code\": \"ordered\","
                        + "           \"display\": \"Ordered\""
                        + "        }]"
                        + "      },"
                        + "      \"doseQuantity\": {"
                        + "        \"value\": 7,"
                        + "        \"unit\": \"mg\","
                        + "        \"system\": \"http://unitsofmeasure.org\","
                        + "        \"code\": \"mg\""
                        + "       }"
                        + "     }]"
                        + "   }],"
                        + " \"dispenseRequest\": {"
                        + "    \"validityPeriod\": {"
                        + "       \"start\": \"2015-01-15\","
                        + "       \"end\": \"2016-01-15\""
                        + "     },"
                        + "     \"numberOfRepeatsAllowed\": 3,"
                        + "    \"quantity\": {"
                        + "      \"value\": 30,"
                        + "      \"unit\": \"TAB\","
                        + "     \"system\":"
                        + "\"http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm\","
                        + "       \"code\": \"TAB\""
                        + "    },"
                        + "    \"expectedSupplyDuration\": {"
                        + "      \"value\": 30,"
                        + "      \"unit\": \"days\","
                        + "      \"system\": \"http://unitsofmeasure.org\","
                        + "      \"code\": \"d\""
                        + "    }"
                        + "  },"
                        + " \"substitution\": {"
                        + "    \"allowedBoolean\": true,"
                        + "    \"reason\": {"
                        + "      \"coding\": [{"
                        + "\"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActReason\","
                        + "\"code\": \"FP\","
                        + "\"display\": \"formulary policy\" "
                        + "}]"
                        + "}  }"
                        + "}";

        public MedicationRequestBuilder() {
            super(DEFAULT_REQUEST_JSON);
        }

        @Override
        protected MedicationRequestBuilder returnThis() {
            return this;
        }
    }

    /**
     * Returns a builder class that will help build <a
     * href="https://www.hl7.org/fhir/medication.html">Medication</a> FHIR JSON.
     */
    public static MedicationBuilder medication() {
        return new MedicationBuilder();
    }

    /**
     * Returns a builder class that will help build <a
     * href="https://www.hl7.org/fhir/medication.html">Medication</a> FHIR JSON, in FHIR version R4
     * format.
     */
    public static MedicationStatementR4Builder statementR4() {
        return new MedicationStatementR4Builder();
    }

    /**
     * Returns a builder class that will help build <a
     * href="https://www.hl7.org/fhir/medicationrequest.html">MedicationRequest</a> FHIR JSON.
     */
    public static MedicationRequestBuilder request() {
        return new MedicationRequestBuilder();
    }
}
