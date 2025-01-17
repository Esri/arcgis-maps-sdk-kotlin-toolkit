package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

public class PermissionState constructor(
    private val context: Context,
    private val permissions: Array<String>,
    private val onPermissionResult: (Map<String, Boolean>) -> Unit,
    private val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
) {

    public val allPermissionsGranted: Boolean
        get() {
            return permissions.associateWith { checkPermissionGranted(it) }.all { it.value }
        }

    private fun checkPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    internal fun getPermissionsGranted(): Map<String, Boolean> {
        return permissions.associateWith { checkPermissionGranted(it) }
    }

    internal fun launchRequest() {
        launcher.launch(permissions)
    }
}

@Composable
internal fun rememberPermissionState(
    context: Context,
    permissions: Array<String>,
    onPermissionResult: (Map<String, Boolean>) -> Unit
): PermissionState {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantedState: Map<String, Boolean> ->
            onPermissionResult(grantedState)
        }
    return remember { PermissionState(context, permissions, onPermissionResult, launcher) }
}