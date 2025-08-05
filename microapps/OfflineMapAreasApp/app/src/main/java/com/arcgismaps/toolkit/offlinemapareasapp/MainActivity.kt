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

package com.arcgismaps.toolkit.offlinemapareasapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.offlinemapareasapp.screens.MainScreen
import com.esri.microappslib.theme.MicroAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        setContent {
            MicroAppTheme {
                OfflineMapAreasApp()
                RequestNotificationPermission(
                    onResult = { isGranted ->
                        if (!isGranted) {
                            Log.e("OfflineMapAreas", "Notification permission request was denied.")
                        }
                    })
            }
        }
    }
}

@Composable
fun OfflineMapAreasApp() {
    MainScreen()
}

@Composable
private fun RequestNotificationPermission(
    onResult: (granted: Boolean) -> Unit
) {
    // Explicit notification permissions not required for versions < 33
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return onResult(true)
    }

    // Use the context to check for permissions
    val context = LocalContext.current

    // Track current permission state
    var hasPermission by remember {
        mutableStateOf(
            value = ContextCompat.checkSelfPermission(/* context = */ context,/* permission = */
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // If permission is already granted
    if (hasPermission) {
        return onResult(true)
    }

    // Launcher for the permission dialog
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        hasPermission = granted
        onResult(granted)
    }

    // If permissions is not already granted, show dialog to grant request
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            launcher.launch(POST_NOTIFICATIONS)
        }
    }
}
