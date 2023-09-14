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
package com.android.healthconnect.controller.route

import android.app.Activity
import android.content.Intent
import android.health.connect.HealthConnectManager.EXTRA_EXERCISE_ROUTE
import android.health.connect.HealthConnectManager.EXTRA_SESSION_ID
import android.health.connect.datatypes.ExerciseRoute
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.formatters.ExerciseSessionFormatter
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.maybeShowWhatsNewDialog
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.showMigrationInProgressDialog
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.showMigrationPendingDialog
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationState
import com.android.healthconnect.controller.route.ExerciseRouteViewModel.SessionWithAttribution
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.map.MapView
import com.android.healthconnect.controller.utils.FeatureUtils
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.logging.ErrorPageElement
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/** Request route activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class RouteRequestActivity : Hilt_RouteRequestActivity() {

    companion object {
        private const val TAG = "RouteRequestActivity"
    }

    @Inject lateinit var appInfoReader: AppInfoReader

    @Inject lateinit var featureUtils: FeatureUtils

    @VisibleForTesting var dialog: AlertDialog? = null

    @VisibleForTesting lateinit var infoDialog: AlertDialog

    private val viewModel: ExerciseRouteViewModel by viewModels()
    private val migrationViewModel: MigrationViewModel by viewModels()

    private val sessionIdExtra: String?
        get() = intent.getStringExtra(EXTRA_SESSION_ID)

    private var requester: String? = null
    private var migrationState = MigrationState.UNKNOWN
    private var sessionWithAttribution: SessionWithAttribution? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!featureUtils.isExerciseRouteEnabled()) {
            Log.e(TAG, "Exercise routes not available, finishing.")
            finishCancelled()
            return
        }

        if (sessionIdExtra == null || callingPackage == null) {
            Log.e(TAG, "Invalid Intent Extras, finishing.")
            finishCancelled()
            return
        }

        val callingPackageName = callingPackage!!

        if (!viewModel.isReadRoutesPermissionDeclared(callingPackageName)) {
            Log.e(TAG, "Read permission not declared")
            finishCancelled()
            return
        }

        viewModel.getExerciseWithRoute(sessionIdExtra!!)
        runBlocking { requester = appInfoReader.getAppMetadata(callingPackageName).appName }
        viewModel.exerciseSession.observe(this) { session ->
            this.sessionWithAttribution = session
            setupRequestDialog(session, callingPackageName)
        }

        migrationViewModel.migrationState.observe(this) { migrationState ->
            when (migrationState) {
                is MigrationViewModel.MigrationFragmentState.WithData -> {
                    maybeShowMigrationDialog(migrationState.migrationState)
                    this.migrationState = migrationState.migrationState
                }
                else -> {
                    // do nothing
                }
            }
        }
    }

    private fun setupRequestDialog(data: SessionWithAttribution?, callingPackage: String) {
        if (data == null ||
            data.session.route == null ||
            data.session.route?.routeLocations.isNullOrEmpty()) {
            Log.e(TAG, "No route or empty route, finishing.")
            finishCancelled()
            return
        }

        val session = data.session
        val route = session.route!!

        if (session.metadata.dataOrigin.packageName == callingPackage) {
            finishWithResult(route)
            return
        }

        if (viewModel.isSessionInaccessible(callingPackage, session)) {
            Log.i(TAG, "Requested exercise session is inaccessible.")
            finishCancelled()
            return
        }

        if (viewModel.isReadRoutesPermissionGranted(callingPackage)) {
            finishWithResult(route)
            return
        }

        if (viewModel.isReadRoutesPermissionUserFixed(callingPackage)) {
            finishCancelled()
            return
        }

        val sessionDetails =
            applicationContext.getString(
                R.string.date_owner_format,
                LocalDateTimeFormatter(applicationContext).formatLongDate(session.startTime),
                data.appInfo.appName)
        val sessionTitle =
            if (session.title.isNullOrBlank())
                ExerciseSessionFormatter.Companion.getExerciseType(
                    applicationContext, session.exerciseType)
            else session.title
        val view = layoutInflater.inflate(R.layout.route_request_dialog, null)

        val title = applicationContext.getString(R.string.request_route_header_title, requester)

        view.findViewById<MapView>(R.id.map_view).setRoute(session.route!!)
        view.findViewById<TextView>(R.id.session_title).text = sessionTitle
        view.findViewById<TextView>(R.id.date_app).text = sessionDetails

        view.findViewById<LinearLayout>(R.id.more_info).setOnClickListener {
            dialog?.hide()
            setupInfoDialog()
            infoDialog.show()
        }

        view.findViewById<Button>(R.id.route_dont_allow_button).setOnClickListener {
            finishCancelled()
        }

        val allowAllButton: Button = view.findViewById<Button>(R.id.route_allow_all_button)

        allowAllButton.setOnClickListener {
            viewModel.grantReadRoutesPermission(callingPackage)
            finishWithResult(route)
        }

        val shouldShowAllowAllRoutesButton = featureUtils.isExerciseRouteReadAllEnabled()

        allowAllButton.visibility =
            if (shouldShowAllowAllRoutesButton) {
                VISIBLE
            } else {
                GONE
            }

        view.findViewById<Button>(R.id.route_allow_button).setOnClickListener {
            finishWithResult(route)
        }

        dialog =
            AlertDialogBuilder(this)
                .setCustomIcon(R.attr.healthConnectIcon)
                .setCustomTitle(title)
                .setView(view)
                .setCancelable(false)
                .create()
        if (!dialog!!.isShowing &&
            migrationState in
                listOf(
                    MigrationState.IDLE,
                    MigrationState.COMPLETE,
                    MigrationState.COMPLETE_IDLE,
                    MigrationState.ALLOWED_MIGRATOR_DISABLED,
                    MigrationState.ALLOWED_ERROR)) {
            dialog?.show()
        }
    }

    private fun setupInfoDialog() {
        val view = layoutInflater.inflate(R.layout.route_sharing_info_dialog, null)
        infoDialog =
            AlertDialogBuilder(this)
                .setCustomIcon(R.attr.privacyPolicyIcon)
                .setCustomTitle(getString(R.string.request_route_info_header_title))
                .setNegativeButton(R.string.back_button, ErrorPageElement.UNKNOWN_ELEMENT) { _, _ ->
                    dialog?.show()
                }
                .setView(view)
                .setCancelable(false)
                .create()
    }

    private fun maybeShowMigrationDialog(migrationState: MigrationState) {
        when (migrationState) {
            MigrationState.IN_PROGRESS -> {
                showMigrationInProgressDialog(
                    this,
                    applicationContext.getString(
                        R.string.migration_in_progress_permissions_dialog_content, requester)) {
                        _,
                        _ ->
                        finish()
                    }
            }
            MigrationState.ALLOWED_PAUSED,
            MigrationState.ALLOWED_NOT_STARTED,
            MigrationState.APP_UPGRADE_REQUIRED,
            MigrationState.MODULE_UPGRADE_REQUIRED -> {
                showMigrationPendingDialog(
                    this,
                    applicationContext.getString(
                        R.string.migration_pending_permissions_dialog_content, requester),
                    positiveButtonAction = { _, _ -> dialog?.show() },
                    negativeButtonAction = { _, _ -> finishCancelled() })
            }
            MigrationState.COMPLETE -> {
                maybeShowWhatsNewDialog(this) { _, _ -> dialog?.show() }
            }
            else -> {
                // Show the request dialog
                dialog?.show()
            }
        }
    }

    override fun onDestroy() {
        dialog?.dismiss()
        super.onDestroy()
    }

    private fun finishWithResult(route: ExerciseRoute) {
        val result = Intent()
        result.putExtra(EXTRA_SESSION_ID, sessionIdExtra)
        result.putExtra(EXTRA_EXERCISE_ROUTE, route)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun finishCancelled() {
        val result = Intent()
        sessionIdExtra?.let { result.putExtra(EXTRA_SESSION_ID, it) }
        setResult(Activity.RESULT_CANCELED, result)
        finish()
    }
}
