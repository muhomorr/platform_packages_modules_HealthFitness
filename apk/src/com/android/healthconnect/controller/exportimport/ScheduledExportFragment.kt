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

package com.android.healthconnect.controller.exportimport

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportSettings
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import dagger.hilt.android.AndroidEntryPoint

/** Fragment showing the status of configured automatic fragment. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ScheduledExportFragment : Hilt_ScheduledExportFragment() {

    // TODO: b/325917283 - Add proper logging for the automatic export fragment.
    companion object {
        const val SCHEDULED_EXPORT_CONTROL_PREFERENCE_KEY = "scheduled_export_control_preference"
    }

    private val viewModel: ExportSettingsViewModel by viewModels()

    private val scheduledExportControlPreference: HealthMainSwitchPreference? by lazy {
        preferenceScreen.findPreference(SCHEDULED_EXPORT_CONTROL_PREFERENCE_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.scheduled_export_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.storedExportSettings.observe(viewLifecycleOwner) { exportSettings ->
            when (exportSettings) {
                is ExportSettings.WithData -> {
                    if (exportSettings.frequency != ExportFrequency.EXPORT_FREQUENCY_NEVER) {
                        scheduledExportControlPreference?.isChecked = true
                        scheduledExportControlPreference?.title =
                            getString(R.string.automatic_export_on)
                    } else {
                        scheduledExportControlPreference?.isChecked = false
                        scheduledExportControlPreference?.title =
                            getString(R.string.automatic_export_off)
                    }
                    viewModel.updatePreviousExportFrequency(exportSettings.frequency)
                }
                is ExportSettings.LoadingFailed ->
                    Toast.makeText(requireActivity(), R.string.default_error, Toast.LENGTH_LONG)
                        .show()
                is ExportSettings.Loading -> {
                    // Do nothing.
                }
            }
        }

        scheduledExportControlPreference?.addOnSwitchChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.previousExportFrequency.value?.let { previousExportFrequency ->
                    viewModel.updateExportFrequency(previousExportFrequency)
                }
            } else {
                viewModel.updateExportFrequency(ExportFrequency.EXPORT_FREQUENCY_NEVER)
            }
        }
    }
}