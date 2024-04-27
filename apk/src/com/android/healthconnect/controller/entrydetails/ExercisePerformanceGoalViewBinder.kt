/**
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.entrydetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.ExercisePerformanceGoalEntry
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder
import com.android.healthconnect.controller.utils.logging.EntryDetailsElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import dagger.hilt.android.EntryPointAccessors

class ExercisePerformanceGoalViewBinder : ViewBinder<ExercisePerformanceGoalEntry, View> {
    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(context, HealthConnectLoggerEntryPoint::class.java)
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_entry_content, parent, false)
    }

    override fun bind(view: View, data: ExercisePerformanceGoalEntry, index: Int) {
        val title = view.findViewById<TextView>(R.id.item_data_entry_content)
        title.setPaddingRelative(
            view.context.resources.getDimension(R.dimen.spacing_small).toInt(),
            /* top= */ 0,
            /* end= */ 0,
            /* bottom= */ 0)
        title.text = view.context.getString(R.string.bulleted_content, data.title)
        title.contentDescription = data.titleA11y
        logger.logImpression(EntryDetailsElement.EXERCISE_PERFORMANCE_GOAL_ENTRY_VIEW)
    }
}