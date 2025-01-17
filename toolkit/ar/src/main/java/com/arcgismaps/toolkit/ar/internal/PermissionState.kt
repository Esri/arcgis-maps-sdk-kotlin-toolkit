package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PermissionState(private val context: Context, vararg permissions: String) {
    private val permissions: Array<String> = arrayOf(*permissions)

    private val _isGrantedState =
        MutableStateFlow(this.permissions.all { checkPermissionGranted(it) })
    val isGrantedState = _isGrantedState.asStateFlow()

    private val _hasLaunchedRequest = MutableStateFlow(false)
    val hasLaunchedRequest = _hasLaunchedRequest.asStateFlow()

    private fun checkPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun onLauncherResult(granted: Boolean): Unit {
        _isGrantedState.value = granted
    }

    private fun onLauncherResult(granted: Map<String, Boolean>): Unit {
        _isGrantedState.value = granted.all { it.value }
    }

    fun getPermissionsGranted(): Map<String, Boolean> {
        return permissions.associateWith { checkPermissionGranted(it) }
    }

    @Composable
    fun LaunchRequest() {
        if (permissions.size == 1) {
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                onLauncherResult(isGranted)
            }
            SideEffect {
                requestPermissionLauncher.launch(permissions[0])
                _hasLaunchedRequest.value = true
            }
        } else {
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted ->
                onLauncherResult(isGranted)
            }
            SideEffect {
                requestPermissionLauncher.launch(permissions)
                _hasLaunchedRequest.value = true
            }
        }
    }
}