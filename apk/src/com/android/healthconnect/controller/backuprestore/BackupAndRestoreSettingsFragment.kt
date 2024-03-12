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

package com.android.healthconnect.controller.backuprestore

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import dagger.hilt.android.AndroidEntryPoint

/** Fragment displaying backup and restore settings. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class BackupAndRestoreSettingsFragment : Hilt_BackupAndRestoreSettingsFragment() {
    // TODO: b/325914485 - Add proper logging and navigations for the backup and restore settings fragment.

    companion object {
        const val RECURRING_EXPORT_PREFERENCE_KEY = "recurring_export"
    }

    private val recurringExportPreference: HealthPreference? by lazy {
        preferenceScreen.findPreference(RECURRING_EXPORT_PREFERENCE_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.backup_and_restore_settings_screen, rootKey)

        recurringExportPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_backupAndRestoreSettingsFragment_to_exportSetupActivity)
            true
        }
    }
}