/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat


/**
 * Tracks the state of a permission request and provides a method to launch the request.
 *
 * Note: This class should not be constructed directly. Use [rememberPermissionState] instead.
 *
 * @see rememberPermissionState
 * @param context required for checking status of permissions.
 * @param permissions the permissions to request.
 * @param launcher the launcher configured for requesting multiple permissions
 * @since 200.7.0
 */
internal class PermissionState constructor(
    private val context: Context,
    private val permissions: Array<String>,
    private val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
) {

    /**
     * Returns true if all permissions are granted.
     *
     * @since 200.7.0
     */
    val allPermissionsGranted: Boolean
        get() {
            return permissions.map { checkPermissionGranted(it) }.all { it }
        }

    /**
     * Launches the permission request.
     *
     * @since 200.7.0
     */
    fun launchRequest() {
        launcher.launch(permissions)
    }

    private fun checkPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Constructs a [PermissionState] and remembers it.
 *
 * @param context required for checking status of permissions.
 * @param permissions the permissions to request.
 * @param onPermissionResult callback to be invoked when the permission request is completed.
 * @since 200.7.0
 */
@Composable
internal fun rememberPermissionState(
    context: Context,
    permissions: List<String>,
    onPermissionResult: (Map<String, Boolean>) -> Unit
): PermissionState {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantedState: Map<String, Boolean> ->
            onPermissionResult(grantedState)
        }
    return remember { PermissionState(context, permissions.toTypedArray(), launcher) }
}