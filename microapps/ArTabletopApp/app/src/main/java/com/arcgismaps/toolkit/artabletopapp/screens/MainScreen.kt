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

package com.arcgismaps.toolkit.artabletopapp.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.arcgismaps.toolkit.ar.TableTopSceneViewProxy
import com.arcgismaps.toolkit.ar.TableTopSceneViewStatus
import com.arcgismaps.toolkit.ar.rememberTableTopSceneViewStatus
import com.arcgismaps.toolkit.artabletopapp.R
import kotlinx.coroutines.launch
import androidx.core.content.edit

private const val KEY_PREF_ACCEPTED_PRIVACY_INFO = "ACCEPTED_PRIVACY_INFO"

@Composable
fun MainScreen() {
    val arcGISSceneLayer = remember {
        ArcGISSceneLayer("https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/DevA_BuildingShells/SceneServer")
    }
    val arcGISScene = remember {
        ArcGISScene().apply {
            operationalLayers.add(arcGISSceneLayer)
            // for the purpose of this sample, we want the base surface to be fully transparent
            baseSurface.opacity = 0f
        }
    }
    val arcGISSceneAnchor = remember {
        Point(-122.68350326165559, 45.53257485106716, 0.0, arcGISScene.spatialReference)
    }

    // Tracks the currently selected building
    var identifiedBuilding by remember { mutableStateOf<IdentifiedBuilding?>(null) }

    var initializationStatus: TableTopSceneViewStatus by rememberTableTopSceneViewStatus()
    val tableTopSceneViewProxy = remember { TableTopSceneViewProxy() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        val sharedPreferences = LocalContext.current.getSharedPreferences("", Context.MODE_PRIVATE)
        var acceptedPrivacyInfo by rememberSaveable { mutableStateOf(sharedPreferences.getBoolean(
            KEY_PREF_ACCEPTED_PRIVACY_INFO, false)) }
        var showPrivacyInfo by rememberSaveable { mutableStateOf(!acceptedPrivacyInfo) }
        if (showPrivacyInfo) {
            PrivacyInfoDialog(
                acceptedPrivacyInfo,
                onUserResponse = { accepted ->
                    acceptedPrivacyInfo = accepted
                    sharedPreferences.edit { putBoolean(KEY_PREF_ACCEPTED_PRIVACY_INFO, accepted) }
                    showPrivacyInfo = false
                }
            )
        }
        if (!acceptedPrivacyInfo) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.privacy_info_not_accepted))
                Button(
                    onClick = { showPrivacyInfo = true }
                ) {
                    Text(stringResource(R.string.show_privacy_info))
                }
            }
        } else {
            TableTopSceneView(
                arcGISScene = arcGISScene,
                arcGISSceneAnchor = arcGISSceneAnchor,
                translationFactor = 400.0,
                modifier = Modifier.fillMaxSize(),
                clippingDistance = 400.0,
                tableTopSceneViewProxy = tableTopSceneViewProxy,
                onInitializationStatusChanged = {
                    initializationStatus = it
                },
                onSingleTapConfirmed = { tap ->
                    arcGISSceneLayer.clearSelection()
                    coroutineScope.launch {
                        identifiedBuilding = arcGISSceneLayer.identifyBuilding(
                            tap.screenCoordinate,
                            tableTopSceneViewProxy
                        )
                        identifiedBuilding?.let { identifiedBuilding ->
                            arcGISSceneLayer.selectFeature(identifiedBuilding.feature)
                        }
                    }
                }
            ) {
                identifiedBuilding?.let {
                    Callout(it.location) {
                        Text("Building ID: ${it.feature.attributes["OBJECTID"]}")
                    }
                }
            }

            // Show an overlay with instructions or progress indicator based on the initialization status
            when (val status = initializationStatus) {
                is TableTopSceneViewStatus.Initializing -> TextWithScrim(text = stringResource(R.string.initializing_overlay))
                is TableTopSceneViewStatus.DetectingPlanes -> TextWithScrim(text = stringResource(R.string.detect_planes_overlay))
                is TableTopSceneViewStatus.Initialized -> {
                    val sceneLoadStatus = arcGISScene.loadStatus.collectAsStateWithLifecycle().value
                    when (sceneLoadStatus) {
                        is LoadStatus.NotLoaded -> {
                            // Tell the user to tap the screen if the scene has not started loading
                            TextWithScrim(text = stringResource(R.string.tap_scene_overlay))
                        }

                        is LoadStatus.Loading -> {
                            // The scene may take a while to load, so show a progress indicator
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is LoadStatus.FailedToLoad -> {
                            TextWithScrim(
                                text = stringResource(
                                    R.string.failed_to_load_scene,
                                    sceneLoadStatus.error
                                )
                            )
                        }

                        LoadStatus.Loaded -> {} // Do nothing
                    }
                }

                is TableTopSceneViewStatus.FailedToInitialize -> {
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

/**
 * Displays the provided [text] on top of a half-transparent gray background.
 *
 * @since 200.6.0
 */
@Composable
fun TextWithScrim(text: String) {
    Column(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.5f))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}

/**
 * Identifies the building at the given [screenCoordinate] and returns the identified building.
 * If no feature is identified, or if no location can be found for the given [screenCoordinate],
 * this function returns `null`.
 *
 * @since 200.6.0
 */
private suspend fun ArcGISSceneLayer.identifyBuilding(
    screenCoordinate: ScreenCoordinate,
    proxy: TableTopSceneViewProxy
): IdentifiedBuilding? {
    val identifyLayerResult = proxy.identify(this, screenCoordinate, 50.dp).getOrElse {
        return null
    }
    val identifiedFeature =
        identifyLayerResult.geoElements.firstOrNull() as? ArcGISFeature ?: return null
    val identifiedPoint = proxy.screenToLocation(screenCoordinate).getOrNull() ?: return null
    return IdentifiedBuilding(identifiedFeature, identifiedPoint)
}

/**
 * Represents a building feature along with the location in the scene where it was identified.
 *
 * @since 200.6.0
 */
private data class IdentifiedBuilding(val feature: ArcGISFeature, val location: Point)

/**
 * An alert dialog that asks the user to accept or deny [ARCore's privacy requirements](https://developers.google.com/ar/develop/privacy-requirements).
 *
 * @since 200.7.0
 */
@Composable
private fun PrivacyInfoDialog(
    hasCurrentlyAccepted: Boolean,
    onUserResponse: (accepted: Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = {
            onUserResponse(hasCurrentlyAccepted)
        }
    ) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                LegalTextArCore()
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            onUserResponse(false)
                        }) {
                        Text(stringResource(R.string.decline))
                    }

                    TextButton(
                        onClick = {
                            onUserResponse(true)
                        }
                    ) {
                        Text(stringResource(R.string.accept))
                    }
                }
            }
        }
    }
}

/**
 * Displays the required privacy information for use of ARCore
 *
 * @since 200.7.0
 */
@Composable
private fun LegalTextArCore() {
    val textLinkStyle =
        TextLinkStyles(style = SpanStyle(color = Color.Blue))
    Text(
        text = buildAnnotatedString {
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
                    "https://policies.google.com/privacy",
                    textLinkStyle
                )
            ) {
                append("Google Privacy Policy.")
            }
        }
    )
}
