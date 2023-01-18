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

package com.android.health.connect.backuprestore;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.health.connect.HealthConnectManager;
import android.os.ParcelFileDescriptor;
import android.util.Slog;

/**
 * An intermediary to help with the transfer of HealthConnect data during device-to-device transfer.
 */
public class HealthConnectBackupAgent extends BackupAgent {
    private static final String TAG = "HealthConnectBackupAgent";
    private static final boolean DEBUG = false;

    private HealthConnectManager mHealthConnectManager;

    @Override
    public void onCreate() {
        if (DEBUG) {
            Slog.v(TAG, "onCreate()");
        }

        mHealthConnectManager = this.getSystemService(HealthConnectManager.class);
    }

    @Override
    public void onRestoreFinished() {}

    @Override
    public void onBackup(
            ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        // we don't do incremental backup / restore.
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) {
        // we don't do incremental backup / restore.
    }
}
