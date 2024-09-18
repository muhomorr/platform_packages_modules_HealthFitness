/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.permission;

import static android.content.pm.PackageManager.GET_PERMISSIONS;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class with PackageInfo-related methods for {@link FirstGrantTimeManager}
 *
 * @hide
 */
public final class PackageInfoUtils {
    private static final String TAG = "HCPackageInfoUtils";

    @Nullable private static volatile PackageInfoUtils sPackageInfoUtils;

    /**
     * Store PackageManager for each user. Keys are users, values are PackageManagers which get from
     * each user.
     */
    private final Map<UserHandle, PackageManager> mUsersPackageManager = new ArrayMap<>();

    private PackageInfoUtils() {}

    /** Returns singleton instance of PackageInfoUtils */
    public static synchronized PackageInfoUtils getInstance() {
        if (sPackageInfoUtils == null) {
            sPackageInfoUtils = new PackageInfoUtils();
        }

        return requireNonNull(sPackageInfoUtils);
    }

    Map<String, Set<Integer>> collectSharedUserNameToUidsMappingForUser(
            List<PackageInfo> packageInfos, UserHandle user) {
        Map<String, Set<Integer>> sharedUserNameToUids = new ArrayMap<>();
        for (PackageInfo info : packageInfos) {
            if (info.sharedUserId != null) {
                if (sharedUserNameToUids.get(info.sharedUserId) == null) {
                    sharedUserNameToUids.put(info.sharedUserId, new ArraySet<>());
                }
                sharedUserNameToUids.get(info.sharedUserId).add(info.applicationInfo.uid);
            }
        }
        return sharedUserNameToUids;
    }

    public List<PackageInfo> getPackagesHoldingHealthPermissions(UserHandle user, Context context) {
        // TODO(b/260707328): replace with getPackagesHoldingPermissions
        List<PackageInfo> allInfos =
                getPackageManagerAsUser(user, context)
                        .getInstalledPackages(PackageManager.PackageInfoFlags.of(GET_PERMISSIONS));
        List<PackageInfo> healthAppsInfos = new ArrayList<>();

        for (PackageInfo info : allInfos) {
            if (anyRequestedHealthPermissionGranted(context, info)) {
                healthAppsInfos.add(info);
            }
        }
        return healthAppsInfos;
    }

    @SuppressWarnings("NullAway")
    // TODO(b/317029272): fix this suppression
    boolean hasGrantedHealthPermissions(String[] packageNames, UserHandle user, Context context) {
        for (String packageName : packageNames) {
            PackageInfo info = getPackageInfoWithPermissionsAsUser(packageName, user, context);
            if (anyRequestedHealthPermissionGranted(context, info)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    String[] getPackagesForUid(int packageUid, UserHandle user, Context context) {
        return getPackageManagerAsUser(user, context).getPackagesForUid(packageUid);
    }

    /**
     * Checks if the given package had any read/write permissions to Health Connect.
     *
     * @param context Context
     * @param packageInfo Package to check
     * @return If the given package is connected to Health Connect.
     */
    public static boolean anyRequestedHealthPermissionGranted(
            @Nullable Context context, @Nullable PackageInfo packageInfo) {
        if (context == null || packageInfo == null || packageInfo.requestedPermissions == null) {
            Log.w(TAG, "Can't extract requested permissions from the package info.");
            return false;
        }

        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            String currPerm = packageInfo.requestedPermissions[i];
            if (HealthConnectManager.isHealthPermission(context, currPerm)
                    && ((packageInfo.requestedPermissionsFlags[i]
                                    & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                            != 0)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public PackageInfo getPackageInfoWithPermissionsAsUser(
            String packageName, UserHandle user, Context context) {
        try {
            return getPackageManagerAsUser(user, context)
                    .getPackageInfo(
                            packageName, PackageManager.PackageInfoFlags.of(GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException e) {
            // App not found.
            Log.e(TAG, "NameNotFoundException for " + packageName);
            return null;
        }
    }

    @Nullable
    String getSharedUserNameFromUid(int uid, Context context) {
        UserHandle user = UserHandle.getUserHandleForUid(uid);
        PackageManager packageManager = getPackageManagerAsUser(user, context);
        String[] packages = packageManager.getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            Log.e(TAG, "Can't get package names for UID: " + uid);
            return null;
        }
        try {
            PackageInfo info =
                    packageManager.getPackageInfo(
                            packages[0], PackageManager.PackageInfoFlags.of(0));
            return info.sharedUserId;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package " + packages[0] + " not found.");
            return null;
        }
    }

    Optional<String> getPackageNameFromUid(int uid) {
        String[] packages = getPackageNamesForUid(uid);
        if (packages.length != 1) {
            Log.w(TAG, "Can't get one package name for UID: " + uid);
            return Optional.empty();
        }
        return Optional.of(packages[0]);
    }

    String[] getPackageNamesForUid(int uid) {
        PackageManager packageManager =
                mUsersPackageManager.get(UserHandle.getUserHandleForUid(uid));
        if (packageManager == null) {
            return new String[] {};
        }
        String[] packages = packageManager.getPackagesForUid(uid);
        return packages != null ? packages : new String[] {};
    }

    @Nullable
    Integer getPackageUid(String packageName, UserHandle user, Context context) {
        Integer uid = null;
        try {
            uid =
                    getPackageManagerAsUser(user, context)
                            .getPackageUid(
                                    packageName,
                                    PackageManager.PackageInfoFlags.of(/* flags= */ 0));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFound exception for " + packageName);
        }
        return uid;
    }

    private PackageManager getPackageManagerAsUser(UserHandle user, Context context) {
        PackageManager packageManager = mUsersPackageManager.get(user);
        if (packageManager == null) {
            packageManager = context.getPackageManager();
            mUsersPackageManager.put(user, packageManager);
        }
        return packageManager;
    }
}
