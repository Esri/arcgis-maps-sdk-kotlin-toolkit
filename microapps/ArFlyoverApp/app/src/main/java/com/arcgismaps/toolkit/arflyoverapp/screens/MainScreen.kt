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

package com.arcgismaps.toolkit.arflyoverapp.screens

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.IntegratedMeshLayer
import com.arcgismaps.toolkit.ar.FlyoverSceneView
import com.arcgismaps.toolkit.ar.FlyoverSceneViewStatus
import com.arcgismaps.toolkit.ar.rememberFlyoverSceneViewProxy
import com.arcgismaps.toolkit.ar.rememberFlyoverSceneViewStatus
import com.arcgismaps.toolkit.arflyoverapp.R

private const val KEY_PREF_ACCEPTED_PRIVACY_INFO = "ACCEPTED_PRIVACY_INFO"

@Composable
fun MainScreen() {

    val elevationSource =
        ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")

    val integratedMeshLayer =
        IntegratedMeshLayer("https://tiles.arcgis.com/tiles/z2tnIkrLQ2BRzr6P/arcgis/rest/services/Girona_Spain/SceneServer")

    val arcGISScene = remember {
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            baseSurface.elevationSources.add(elevationSource)
            operationalLayers.add(integratedMeshLayer)
        }
    }

    // Display privacy info dialog if user has not already accepted Google's privacy info
    val sharedPreferences = LocalContext.current.getSharedPreferences("", Context.MODE_PRIVATE)
    var acceptedPrivacyInfo by rememberSaveable {
        mutableStateOf(
            sharedPreferences.getBoolean(
                KEY_PREF_ACCEPTED_PRIVACY_INFO, false
            )
        )
    }
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

    // Display FlyoverSceneView only if user has accepted the privacy info
    if (acceptedPrivacyInfo) {
        val flyoverSceneViewProxy = rememberFlyoverSceneViewProxy(
            location = Point(
                2.82407,
                41.99101,
                230.0,
                SpatialReference.wgs84()
            ),
            heading = 160.0
        )

        Box {
            var initializationStatus by rememberFlyoverSceneViewStatus()

            FlyoverSceneView(
                arcGISScene = arcGISScene,
                flyoverSceneViewProxy = flyoverSceneViewProxy,
                onInitializationStatusChanged = {
                    initializationStatus = it
                },
                translationFactor = 1000.0,
            )

            val sceneLoadStatus =
                arcGISScene.loadStatus.collectAsStateWithLifecycle().value

            when (val status = initializationStatus) {
                is FlyoverSceneViewStatus.Initializing -> {
                    TextWithScrim(text = stringResource(R.string.setting_up_ar))
                }

                is FlyoverSceneViewStatus.FailedToInitialize -> {
                    val message = status.error.message ?: status.error
                    TextWithScrim(text = stringResource(R.string.failed_to_initialize_ar, message))
                }

                else -> {
                    when (sceneLoadStatus) {
                        is LoadStatus.NotLoaded, LoadStatus.Loading -> {
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

                        // successfully loaded so nothing more to do
                        LoadStatus.Loaded -> {}
                    }
                }
            }
        }
    } else {
        // otherwise display a message and a button that causes the privacy info dialog to be shown
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
    }
}

/**
 * An alert dialog that asks the user to accept or deny
 * [ARCore's privacy requirements](https://developers.google.com/ar/develop/privacy-requirements).
 *
 * @param hasCurrentlyAccepted indicates if user has already accepted
 * @param onUserResponse called to indicate if user accepted or declined
 * @since 200.8.0
 */
@Composable
private fun PrivacyInfoDialog(
    hasCurrentlyAccepted: Boolean,
    onUserResponse: (accepted: Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = { onUserResponse(hasCurrentlyAccepted) }
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
                        onClick = { onUserResponse(false) }
                    ) {
                        Text(stringResource(R.string.decline))
                    }

                    TextButton(
                        onClick = { onUserResponse(true) }
                    ) {
                        Text(stringResource(R.string.accept))
                    }
                }
            }
        }
    }
}

/**
 * Displays the required privacy information for use of ARCore.
 *
 * @since 200.8.0
 */
@Composable
private fun LegalTextArCore() {
    val textLinkStyle = TextLinkStyles(style = SpanStyle(color = Color.Blue))
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
            append(" (ARCore), which is provided by Google and governed by the ")
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

/**
 * Displays the provided [text] on top of a half-transparent gray background.
 *
 * @param text the text to display
 * @since 200.8.0
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
