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

package com.arcgismaps.toolkit.arworldscaleapp.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.Color
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewStatus
import com.arcgismaps.toolkit.ar.WorldScaleTrackingMode
import com.arcgismaps.toolkit.ar.rememberWorldScaleSceneViewStatus
import com.arcgismaps.toolkit.arworldscaleapp.R

private const val KEY_PREF_ACCEPTED_PRIVACY_INFO = "ACCEPTED_PRIVACY_INFO"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene = remember {
        val basemap = Basemap(BasemapStyle.ArcGISHumanGeography)
        ArcGISScene(basemap).apply {
            initialViewpoint = Viewpoint(
                latitude = 39.8,
                longitude = -98.6,
                scale = 10e7
            )
            // an elevation source is required for the scene to be placed at the correct elevation
            // if not used, the scene may appear far below the device position because the device position
            // is calculated with elevation
            baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
            baseSurface.backgroundGrid.isVisible = false
            baseSurface.opacity = 0.3f
            // add the Esri 3D Buildings layer
            operationalLayers.add(
                ArcGISSceneLayer("https://www.arcgis.com/home/item.html?id=b8fec5af7dfe4866b1b8ac2d2800f282")
            )
        }
    }
    var displayCalibrationView by remember { mutableStateOf(false) }
    val graphicsOverlays = remember { listOf(GraphicsOverlay()) }
    val proxy = remember { WorldScaleSceneViewProxy() }
    var initializationStatus by rememberWorldScaleSceneViewStatus()
    var trackingMode by rememberSaveable(
        saver = Saver(
            save = {
                it.value.name
            },
            restore = {
                val mode = when (it) {
                    "Geospatial" -> WorldScaleTrackingMode.Geospatial()
                    else -> WorldScaleTrackingMode.World()
                }
                mutableStateOf(mode)
            }
        )
    ) {
        mutableStateOf<WorldScaleTrackingMode>(WorldScaleTrackingMode.World())
    }
    val sharedPreferences = LocalContext.current.getSharedPreferences("", Context.MODE_PRIVATE)
    var acceptedPrivacyInfo by rememberSaveable {
        mutableStateOf(
            sharedPreferences.getBoolean(
                KEY_PREF_ACCEPTED_PRIVACY_INFO,
                false
            )
        )
    }
    var showPrivacyInfo by rememberSaveable { mutableStateOf(!acceptedPrivacyInfo) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("AR World Scale - ${trackingMode::class.java.simpleName}") },
            actions = {
                var actionsExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                    Icon(Icons.Default.MoreVert, "More")
                }

                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("World Tracking") },
                        onClick = {
                            trackingMode = WorldScaleTrackingMode.World()
                            actionsExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Geospatial Tracking") },
                        onClick = {
                            trackingMode = WorldScaleTrackingMode.Geospatial()
                            actionsExpanded = false
                        }
                    )
                    DropdownMenuItem(text = { Text("Show Privacy Info") }, onClick = {
                        showPrivacyInfo = true
                        actionsExpanded = false
                    })
                }
            }
        )
    }) {
        if (showPrivacyInfo) {
            PrivacyInfoDialog(onShowPrivacyInfoChanged = { showPrivacyInfo = it },
                onAcceptedPrivacyInfoChanged = {
                    sharedPreferences.edit().putBoolean(KEY_PREF_ACCEPTED_PRIVACY_INFO, it).apply()
                    acceptedPrivacyInfo = it
                })
        }
        if (!acceptedPrivacyInfo) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Privacy Info not accepted")
                Button(onClick = { showPrivacyInfo = true }) {
                    Text("Show Privacy Info")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                WorldScaleSceneView(
                    arcGISScene = arcGISScene,
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),
                    worldScaleTrackingMode = trackingMode,
                    onInitializationStatusChanged = {
                        initializationStatus = it
                    },
                    worldScaleSceneViewProxy = proxy,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        proxy.screenToBaseSurface(singleTapConfirmedEvent.screenCoordinate)
                            ?.let { point ->
                                graphicsOverlays.first().graphics.add(
                                    Graphic(
                                        point,
                                        SimpleMarkerSceneSymbol(
                                            SimpleMarkerSceneSymbolStyle.Diamond,
                                            Color.green,
                                            height = 1.0,
                                            width = 1.0,
                                            depth = 1.0
                                        )
                                    )
                                )
                            }
                    },
                    graphicsOverlays = graphicsOverlays
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (displayCalibrationView) {
                            CalibrationView(
                                onDismiss = { displayCalibrationView = false },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        } else {
                            FloatingActionButton(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(32.dp),
                                onClick = { displayCalibrationView = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_straighten_24),
                                    contentDescription = stringResource(R.string.calibration_view_button_description)
                                )
                            }
                        }
                    }
                }

                when (val status = initializationStatus) {
                    is WorldScaleSceneViewStatus.Initializing -> {
                        TextWithScrim(text = stringResource(R.string.ar_initializing))
                    }

                    is WorldScaleSceneViewStatus.Initialized -> {
                        val sceneLoadStatus =
                            arcGISScene.loadStatus.collectAsStateWithLifecycle().value
                        when (sceneLoadStatus) {
                            is LoadStatus.Loading, LoadStatus.NotLoaded -> {
                                // The scene may take a while to load, so show a progress indicator
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            is LoadStatus.FailedToLoad -> {
                                TextWithScrim(
                                    text = stringResource(
                                        R.string.failed_to_load_scene,
                                        sceneLoadStatus.error
                                    )
                                )
                            }

                            else -> {}
                        }
                    }

                    is WorldScaleSceneViewStatus.FailedToInitialize -> {
                        TextWithScrim(
                            text = stringResource(
                                R.string.failed_to_initialize_overlay,
                                status.error.message ?: status.error
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PrivacyInfoDialog(
    onShowPrivacyInfoChanged: (Boolean) -> Unit, onAcceptedPrivacyInfoChanged: (Boolean) -> Unit
) {
    BasicAlertDialog(onDismissRequest = {
        onShowPrivacyInfoChanged(false)
    }) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                LegalTextArCore()
                Spacer(Modifier.height(16.dp))
                LegalTextGeospatial()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        onAcceptedPrivacyInfoChanged(false)
                        onShowPrivacyInfoChanged(false)
                    }) {
                        Text("Decline")
                    }

                    TextButton(onClick = {
                        onAcceptedPrivacyInfoChanged(true)
                        onShowPrivacyInfoChanged(false)
                    }) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

/**
 * Displays the provided [text] on top of a half-transparent gray background.
 *
 * @since 200.6.0
 */
@Composable
fun TextWithScrim(text: String) {
    Column(
        modifier = Modifier
            .background(androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}

@Composable
fun LegalTextArCore() {
    val textLinkStyle =
        TextLinkStyles(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Blue))
    Text(text = buildAnnotatedString {
        append("This application runs on ")
        withLink(
            LinkAnnotation.Url(
                "https://play.google.com/store/apps/details?id=com.google.ar.core",
                textLinkStyle
            )
        ) {
            append("Google Play Services for AR")
        }
        append("  (ARCore), which is provided by Google and governed by the ")
        withLink(
            LinkAnnotation.Url(
                "https://policies.google.com/privacy", textLinkStyle
            )
        ) {
            append("Google Privacy Policy.")
        }
    })
}

@Composable
fun LegalTextGeospatial() {
    Text(text = buildAnnotatedString {
        append("To power this session, Google will process sensor data (e.g., camera and location).")
        appendLine()
        withLink(
            LinkAnnotation.Url(
                "https://support.google.com/ar?p=how-google-play-services-for-ar-handles-your-data",
                TextLinkStyles(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Blue))
            )
        ) {
            append("Learn more")
        }
    })
}
