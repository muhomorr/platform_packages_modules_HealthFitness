/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.healthconnect.HealthConnectManager
import android.healthconnect.datatypes.InstantRecord
import android.healthconnect.datatypes.IntervalRecord
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.units.Energy
import android.healthconnect.datatypes.units.Length
import android.healthconnect.datatypes.units.Power
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_DOUBLE
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_LONG
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_TEXT
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.fieldviews.DateTimePicker
import com.android.healthconnect.testapps.toolbox.fieldviews.EditableTextView
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldView
import com.android.healthconnect.testapps.toolbox.fieldviews.ListInputField
import com.android.healthconnect.testapps.toolbox.utils.InsertOrUpdateRecords.Companion.createRecordObject
import com.android.healthconnect.testapps.toolbox.viewmodels.InsertOrUpdateRecordsViewModel
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

class InsertRecordFragment : Fragment() {

    private lateinit var mRecordFields: Array<Field>
    private lateinit var mRecordClass: KClass<out Record>
    private lateinit var mNavigationController: NavController
    private lateinit var mFieldNameToFieldInput: HashMap<String, InputFieldView>
    private lateinit var mLinearLayout: LinearLayout
    private lateinit var mHealthConnectManager: HealthConnectManager
    private lateinit var mUpdateRecordUuid: InputFieldView

    private val mInsertOrUpdateViewModel: InsertOrUpdateRecordsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mInsertOrUpdateViewModel.insertedRecordsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is InsertOrUpdateRecordsViewModel.InsertedRecordsState.WithData -> {
                    showInsertSuccessDialog(state.entries)
                }
                is InsertOrUpdateRecordsViewModel.InsertedRecordsState.Error -> {
                    Toast.makeText(
                            context,
                            "Unable to insert record(s)! ${state.errorMessage}",
                            Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        mInsertOrUpdateViewModel.updatedRecordsState.observe(viewLifecycleOwner) { state ->
            if (state is InsertOrUpdateRecordsViewModel.UpdatedRecordsState.Error) {
                Toast.makeText(
                        context,
                        "Unable to update record(s)! ${state.errorMessage}",
                        Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(context, "Successfully updated record(s)!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        return inflater.inflate(R.layout.fragment_insert_record, container, false)
    }

    private fun showInsertSuccessDialog(records: List<Record>) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Record UUID(s)")
        builder.setMessage(records.joinToString { it.metadata.id })
        builder.setPositiveButton(android.R.string.ok) { _, _ -> }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
        alertDialog.findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavigationController = findNavController()
        mHealthConnectManager =
            requireContext().getSystemService(HealthConnectManager::class.java)!!

        val permissionType =
            arguments?.getSerializable("permissionType", HealthPermissionType::class.java)
                ?: throw java.lang.IllegalArgumentException("Please pass the permissionType.")

        mFieldNameToFieldInput = HashMap()
        mRecordFields = permissionType.recordClass?.java?.declaredFields as Array<Field>
        mRecordClass = permissionType.recordClass
        view.findViewById<TextView>(R.id.title).setText(permissionType.title)
        mLinearLayout = view.findViewById(R.id.record_input_linear_layout)

        when (mRecordClass.java.superclass) {
            IntervalRecord::class.java -> {
                setupStartAndEndTimeFields()
            }
            InstantRecord::class.java -> {
                setupTimeField("Time", "time")
            }
            else -> {
                Toast.makeText(context, R.string.not_implemented, Toast.LENGTH_SHORT).show()
                mNavigationController.popBackStack()
            }
        }
        setupRecordFields()
        setupInsertDataButton(view)
        setupUpdateDataButton(view)
    }

    private fun setupTimeField(title: String, key: String) {
        val timeField = DateTimePicker(this.requireContext(), title)
        mLinearLayout.addView(timeField)

        mFieldNameToFieldInput[key] = timeField
    }

    private fun setupStartAndEndTimeFields() {
        setupTimeField("Start Time", "startTime")
        setupTimeField("End Time", "endTime")
    }

    private fun setupRecordFields() {
        var field: InputFieldView
        for (mRecordsField in mRecordFields) {
            when (mRecordsField.type) {
                Long::class.java -> {
                    field =
                        EditableTextView(this.requireContext(), mRecordsField.name, INPUT_TYPE_LONG)
                }
                Length::class.java,
                Energy::class.java,
                Power::class.java, -> {
                    field =
                        EditableTextView(
                            this.requireContext(), mRecordsField.name, INPUT_TYPE_DOUBLE)
                }
                List::class.java -> {
                    field =
                        ListInputField(
                            this.requireContext(),
                            mRecordsField.name,
                            mRecordsField.genericType as ParameterizedType)
                }
                else -> {
                    break
                }
            }
            mLinearLayout.addView(field)
            mFieldNameToFieldInput[mRecordsField.name] = field
        }
    }

    private fun setupInsertDataButton(view: View) {
        val buttonView = view.findViewById<Button>(R.id.insert_record)

        buttonView.setOnClickListener {
            try {
                val record =
                    createRecordObject(mRecordClass, mFieldNameToFieldInput, requireContext())
                mInsertOrUpdateViewModel.insertRecordsViaViewModel(
                    listOf(record), mHealthConnectManager)
            } catch (ex: Exception) {
                Toast.makeText(
                        context,
                        "Unable to insert record: ${ex.localizedMessage}",
                        Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setupUpdateRecordUuidInputDialog() {
        mUpdateRecordUuid = EditableTextView(requireContext(), null, INPUT_TYPE_TEXT)
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter UUID")
        builder.setView(mUpdateRecordUuid)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                val record =
                    createRecordObject(
                        mRecordClass,
                        mFieldNameToFieldInput,
                        requireContext(),
                        mUpdateRecordUuid.getFieldValue().toString())
                mInsertOrUpdateViewModel.updateRecordsViaViewModel(
                    listOf(record), mHealthConnectManager)
            } catch (ex: Exception) {
                Toast.makeText(
                        context, "Unable to update: ${ex.localizedMessage}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun setupUpdateDataButton(view: View) {
        val buttonView = view.findViewById<Button>(R.id.update_record)

        buttonView.setOnClickListener { setupUpdateRecordUuidInputDialog() }
    }
}
