/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.artabletopapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.arcgismaps.toolkit.ar.TableTopSceneViewProxy
import com.esri.microappslib.theme.MicroAppTheme
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private var isARCoreSupported = false

    private val message = MutableStateFlow("Setting up ARCore...")
    private var isGooglePlayServicesArInstalled = false
    private var userRequestedInstall = true

    private val session = MutableStateFlow<Session?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        enableEdgeToEdge()
        checkARCoreSupport()
        checkGooglePlayServicesArInstalled()
        createSession()
        requestCameraPermission()
        configureSession()

        // required for displayRotationHelper
        // if this is not called, the session will be paused when the screen is turned off and the app crashes
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch {
            message.collect {
                Log.d("MainActivity", it)
            }
        }

        setContent {
            MicroAppTheme {
                val arcGISScene = remember {
                    ArcGISScene(BasemapStyle.OsmStandard)
                }
                val tableTopSceneViewProxy = remember { TableTopSceneViewProxy() }
                var tappedLocation by remember { mutableStateOf<Point?>(null) }
                // Using this to ensure we have a non=null session before even thinking about rendering
                session.collectAsState().value?.let { session ->
                    TableTopSceneView(
                        arcGISScene = arcGISScene,
                        session = session,
                        modifier = Modifier.fillMaxSize(),
                        tableTopSceneViewProxy = tableTopSceneViewProxy,
                        onSingleTapConfirmed = {
                            val location = tableTopSceneViewProxy.screenToBaseSurface(it.screenCoordinate)
                            location?.let { point ->
                                tappedLocation = point
                            }
                        }
                    ) {
                        tappedLocation?.let {
                            Callout(location = it, modifier = Modifier.wrapContentSize()) {
                                Text("Lat: ${it.y.roundToInt()}, Lon: ${it.x.roundToInt()}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkARCoreSupport() {
        // Check if ARCore is supported on the device
        message.value = "Checking ARCore support..."
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        isARCoreSupported = availability.isSupported
        message.value = if (isARCoreSupported) {
            "ARCore is supported on this device."
        } else {
            "ARCore is not supported on this device."
        }
    }

    private fun checkGooglePlayServicesArInstalled() {
        message.value = "Checking Google Play Services for AR..."
        // Check if Google Play Services for AR is installed on the device
        try {
            if (session.value == null) {
                //Note: we should use the async method here but possibly this is causing some timing issues
                // so we are using the sync method for now
                when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        message.value = "Google Play Services for AR installation requested."
                        userRequestedInstall = false
                        return
                    }

                    ArCoreApk.InstallStatus.INSTALLED -> {
                        isGooglePlayServicesArInstalled = true
                        message.value = "Google Play Services for AR is installed."
                        return
                    }

                }
            }
        } catch (e: Exception) {
            message.value = "Error checking Google Play Services for AR: ${e.message}"
        }
    }

    private fun createSession() {
        if (isARCoreSupported && isGooglePlayServicesArInstalled) {
            session.value = Session(this)
            message.value = "ARCore session created."
        }
    }

    private fun requestCameraPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                message.value = "Camera permission granted."
            } else {
                message.value = "Camera permission denied."
            }
        }
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun configureSession() {
        session.value?.let { session ->
            message.value = "Configuring ARCore session..."
            // TODO: Configure the ARCore session
            session.configure(session.config/*.apply{}*/)
            message.value = "ARCore session configured."
        }
    }

    // ensure full screen and immersive mode
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // https://developer.android.com/training/system-ui/immersive.html#sticky
            this
                .getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    override fun onResume() {
        super.onResume()
        session.value?.resume()
    }

    override fun onPause() {
        super.onPause()
        session.value?.pause()
    }

    override fun onDestroy() {
        session.value?.close()
        super.onDestroy()
    }
}