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

package com.android.healthfitness.flags;

import static com.android.healthfitness.flags.DatabaseVersions.LAST_ROLLED_OUT_DB_VERSION;
import static com.android.internal.annotations.VisibleForTesting.Visibility.PRIVATE;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Map;
import java.util.TreeMap;

/**
 * A helper class to act as the source of truth for whether a feature is enabled or not by taking
 * into account both feature flag and DB flag. See go/hc-aconfig-and-db.
 *
 * @hide
 */
public final class AconfigFlagHelper {
    private static final String TAG = "HC" + AconfigFlagHelper.class.getSimpleName();

    // For testing purposes, this field needs to be made public instead of package-private so the
    // unit tests can access it. This is due to tests don't run in the same classloader as the
    // framework. See
    // https://groups.google.com/a/google.com/g/android-chatty-eng/c/TymmRzs3UcY/m/_JeFcynRBwAJ.
    @VisibleForTesting(visibility = PRIVATE)
    public static final TreeMap<Integer, Boolean> DB_VERSION_TO_DB_FLAG_MAP = new TreeMap<>();

    /**
     * Returns the DB version based on DB flag values, this DB version is used to initialize {@link
     * android.database.sqlite.SQLiteOpenHelper} to dictate which DB upgrades will be executed.
     */
    public static synchronized int getDbVersion() {
        if (!Flags.infraToGuardDbChanges()) {
            return LAST_ROLLED_OUT_DB_VERSION;
        }

        int dbVersion = LAST_ROLLED_OUT_DB_VERSION;
        for (Map.Entry<Integer, Boolean> entry : getDbVersionToDbFlagMap().entrySet()) {
            if (!entry.getValue()) {
                break;
            }
            dbVersion = entry.getKey();
        }

        return dbVersion;
    }

    /**
     * Returns whether the DB flag of a feature is enabled.
     * <p>
     * A DB flag is deemed to be enabled if and only if the DB flag as well as all other features
     * with smaller version numbers have their DB flags enabled.
     * <p>
     * For example, if DB_VERSION_TO_DB_FLAG_MAP contains these:
     * <pre>{@code
     *   DB_F1 = true
     *   DB_F2 = true
     *   DB_F3 = true
     *   DB_F4 = false
     * }</pre>
     * Then isDbFlagEnabled(3) will return true and isDbFlagEnabled(4) will return false.
     * <p>
     * In case the map contains a disconnected line of "true"s before the last "false" like this:
     * <pre>{@code
     *   DB_F1 = true
     *   DB_F2 = false
     *   DB_F3 = true
     *   DB_F4 = false
     * }</pre>
     * Then isDbFlagEnabled(3) will return false even though DB_F3 is mapped to true.
     *
     * @see #getDbVersion()
     * @see ag/28760234 for example of how to use this method
     */
    private static synchronized boolean isDbFlagEnabled(int dbVersion) {
        return getDbVersion() >= dbVersion;
    }

    private AconfigFlagHelper() {}

    // =============================================================================================
    // Only things in below this comment should be updated when we move DB schema changes of a
    // feature from "under development" to "finalized". "finalized" here means the DB schema changes
    // won't be changed again, they will be assigned a DB version and a DB flag, if further changes
    // are required to the DB schema, then new DB version and DB flag are required.
    // =============================================================================================

    /**
     * Returns a map of DB version => DB flag with the DB versions being keys and ordered.
     *
     * <p>Note: Because the map is initialized with aconfig flag values, hence it needs to be
     * initialized at run time via a method call rather than static block or static field, otherwise
     * the <code>@EnableFlags</code> annotations won't work in unit tests due to its evaluation
     * being done after the map has already been initialized.
     */
    private static Map<Integer, Boolean> getDbVersionToDbFlagMap() {
        if (!DB_VERSION_TO_DB_FLAG_MAP.isEmpty()) {
            return DB_VERSION_TO_DB_FLAG_MAP;
        }

        // TODO(b/357062401): When PHR DB schema changes is finalized, we'll create
        // DB_VERSION_TO_DB_FLAG_MAP, add the following line, then move advance the PHR DB flag to
        // trunk-food first, then PHR feature flag:
        // DB_VERSION_TO_DB_FLAG_MAP.put(DB_VERSION_PERSONAL_HEALTH_RECORD,
        //                                                  Flags.personalHealthRecordDatabase());

        return DB_VERSION_TO_DB_FLAG_MAP;
    }

    /** Returns a boolean indicating whether PHR feature is enabled. */
    public static synchronized boolean isPersonalHealthRecordEnabled() {
        // TODO(b/357062401): When PHR DB schema changes is finalized:
        // return Flags.personalHealthRecord() &&
        // isDbFlagEnabled(DB_VERSION_PERSONAL_HEALTH_RECORD);
        return Flags.personalHealthRecord();
    }
}
