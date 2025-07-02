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

package com.arcgismaps.toolkit.arflyoverapp

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.arflyoverapp.screens.MainScreen
import com.esri.microappslib.theme.MicroAppTheme
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private var userRequestedInstall: Boolean = true

    // Flow to track if Google Play Services for AR is installed on the device.
    // By using `collectAsStateWithLifecycle()` in the composable, the UI will recompose when the
    // value changes.
    private val isGooglePlayServicesArInstalled = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        setContent {
            MicroAppTheme {
                if (isGooglePlayServicesArInstalled.collectAsStateWithLifecycle().value) {
                    ArFlyoverApp()
                } else {
                    Text(text = stringResource(R.string.arcore_not_installed_screen_message))
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        checkGooglePlayServicesArInstalled()
    }

    /**
     * Check if Google Play Services for AR is installed on the device. If it's not installed, this
     * method must get called twice: once to request the installation and once to ensure it was
     * installed when the activity resumes.
     */
    private fun checkGooglePlayServicesArInstalled() {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    userRequestedInstall = false
                    return
                }

                ArCoreApk.InstallStatus.INSTALLED -> {
                    isGooglePlayServicesArInstalled.value = true
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("ArFlyoverApp", "Error checking Google Play Services for AR: ${e.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArFlyoverApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MicroAppTheme {
        ArFlyoverApp()
    }
}
