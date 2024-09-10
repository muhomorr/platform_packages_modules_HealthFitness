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

package com.android.server.healthconnect;

import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake impl of Preference Helper for use in testing.
 *
 * <p>This is an in-memory impl, and doesn't persist changes to the database.
 */
public class FakePreferenceHelper extends PreferenceHelper {

    public FakePreferenceHelper() {
        super(null);
        mPreferences = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void insertOrReplacePreference(String key, String value) {
        getPreferences().put(key, value);
    }

    @Override
    public synchronized void removeKey(String id) {
        getPreferences().remove(id);
    }

    @Override
    public synchronized void clearCache() {
        mPreferences.clear();
    }
}
