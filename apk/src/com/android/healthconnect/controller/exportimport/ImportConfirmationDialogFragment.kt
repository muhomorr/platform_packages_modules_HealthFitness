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

import android.app.Dialog
import android.content.ContentResolver
import android.content.DialogInterface
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Slog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.api.ImportFlowViewModel
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.logging.ErrorPageElement
import com.android.healthconnect.controller.utils.logging.ImportConfirmationDialogElement
import dagger.hilt.android.AndroidEntryPoint

/** Fragment to get the user to confirm that they have selected the right import file. */
@AndroidEntryPoint(DialogFragment::class)
class ImportConfirmationDialogFragment : Hilt_ImportConfirmationDialogFragment() {

    companion object {
        const val IMPORT_FILE_URI_KEY = "importFileUri"
        const val TAG = "ImportConfirmationDialogFragment"
    }

    private val viewModel: ImportFlowViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val importFileUriString = arguments?.getString(IMPORT_FILE_URI_KEY) ?: ""
        val importFileName = getFileName(importFileUriString)
        // TODO: b/325917283 - Add proper logging for this container
        return AlertDialogBuilder(requireContext(), ErrorPageElement.UNKNOWN_ELEMENT)
            .setIcon(R.attr.importIcon)
            .setTitle(R.string.import_confirmation_dialog_title)
            .setMessage(importFileName)
            .setPositiveButton(
                R.string.import_confirmation_dialog_import_button,
                ImportConfirmationDialogElement.IMPORT_CONFIRMATION_DONE_BUTTON) {
                    _: DialogInterface,
                    _: Int ->
                    Slog.i(TAG, "positive button clicked")
                    Slog.i(TAG, importFileUriString)
                    viewModel.triggerImportOfSelectedFile(Uri.parse(importFileUriString))
                    requireActivity().finish()
                }
            .setNeutralButton(
                R.string.import_confirmation_dialog_cancel_button,
                ImportConfirmationDialogElement.IMPORT_CONFIRMATION_CANCEL_BUTTON) {
                    _: DialogInterface,
                    _: Int ->
                    Slog.i(TAG, "neutral button clicked")
                }
            .create()
    }

    // TODO(b/344888909): move off UI thread to avoid blocking
    private fun getFileName(uriString: String): String {
        val uri: Uri = Uri.parse(uriString)
        var fileName: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor.use { c: Cursor? ->
                if (c != null) {
                    if (c.moveToFirst()) {
                        fileName = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
        }
        return fileName ?: uri.lastPathSegment.toString()
    }
}