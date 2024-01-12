/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.sceneviewlightingoptionsapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.mapping.view.SpaceEffect
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.toolkit.geocompose.SceneViewpointOperation
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene = remember {
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // add base surface for elevation data
            val surface = Surface()
            surface.elevationSources.add(
                ArcGISTiledElevationSource(
                    "https://elevation3d.arcgis" +
                            ".com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
                )
            )
            baseSurface = surface
        }
    }
    val viewpointOperation = SceneViewpointOperation.SetCamera(
        Camera(
            Point(
                -73.0815,
                -49.3272,
                4059.0,
                SpatialReference.wgs84()
            ), 11.0, 82.0, 0.0
        )
    )

    val lightingOptionsState = remember {
        LightingOptionsState(
            mutableStateOf(Instant.parse("2000-09-22T15:00:00Z")),
            mutableStateOf(LightingMode.LightAndShadows),
            mutableStateOf(Color(220, 220, 220, 255)),
            mutableStateOf(AtmosphereEffect.HorizonOnly),
            mutableStateOf(SpaceEffect.Stars)
        )
    }

    Scaffold(
        topBar = {
            var optionsExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("SceneView Lighting Options App")
                },
                actions = {
                    IconButton(onClick = { optionsExpanded = !optionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    LightingOptionsDropDownMenu(
                        expanded = optionsExpanded,
                        lightingOptionsState = lightingOptionsState,
                        onDismissRequest = { optionsExpanded = false },
                    )
                }
            )
        },
    ) { innerPadding ->
        SceneView(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            arcGISScene = arcGISScene,
            viewpointOperation = viewpointOperation,
            sunTime = lightingOptionsState.sunTime.value,
            sunLighting = lightingOptionsState.sunLighting.value,
            ambientLightColor = lightingOptionsState.ambientLightColor.value,
            atmosphereEffect = lightingOptionsState.atmosphereEffect.value,
            spaceEffect = lightingOptionsState.spaceEffect.value
        )
    }
}

/**
 * A drop down menu providing a selection of Lighting Options that can be changed
 */
@Composable
fun LightingOptionsDropDownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onDismissRequest: (() -> Unit) = {},
    lightingOptionsState: LightingOptionsState
) {
    val items = remember {
        listOf(
            "Sun Time",
            "Sun Lighting",
            "Ambient Light Color",
            "Atmosphere Effect",
            "Space Effect"
        )
    }
    var showSunTimeOptions by remember { mutableStateOf(false) }
    var showSunLightingOptions by remember { mutableStateOf(false) }
    var showAmbientLightColorOptions by remember { mutableStateOf(false) }
    var showAtmosphereEffectOptions by remember { mutableStateOf(false) }
    var showSpaceEffectOptions by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        items.forEach {
            DropdownMenuItem(
                text = {
                    Text(text = it)
                },
                onClick = {
                    when (it) {
                        "Sun Time" -> showSunTimeOptions = true
                        "Sun Lighting" -> showSunLightingOptions = true
                        "Ambient Light Color" -> showAmbientLightColorOptions = true
                        "Atmosphere Effect" -> showAtmosphereEffectOptions = true
                        "Space Effect" -> showSpaceEffectOptions = true
                    }
                })
        }
    }

    if (showSunTimeOptions) {
        SunTimeOptions(
            currentSunTime = lightingOptionsState.sunTime.value,
            setTime = { lightingOptionsState.sunTime.value = it }
        ) {
            showSunTimeOptions = false
            onDismissRequest()
        }
    }

    if (showSunLightingOptions) {
        SunLightingOptions(
            currentLightingMode = lightingOptionsState.sunLighting.value,
            setSunLighting = { lightingOptionsState.sunLighting.value = it }
        ) {
            showSunLightingOptions = false
            onDismissRequest()
        }
    }

    if (showAmbientLightColorOptions) {
        AmbientLightColorOptions(
            currentColor = lightingOptionsState.ambientLightColor.value,
            onSetColor = { lightingOptionsState.ambientLightColor.value = it }
        ) {
            showAmbientLightColorOptions = false
            onDismissRequest()
        }
    }

    if (showAtmosphereEffectOptions) {
        AtmosphereEffectOptions(
            currentAtmosphereEffect = lightingOptionsState.atmosphereEffect.value,
            onSetAtmosphereEffect = { lightingOptionsState.atmosphereEffect.value = it }
        ) {
            showAtmosphereEffectOptions = false
            onDismissRequest()
        }
    }

    if (showSpaceEffectOptions) {
        SpaceEffectOptions(
            currentSpaceEffect = lightingOptionsState.spaceEffect.value,
            onSetSpaceEffect = { lightingOptionsState.spaceEffect.value = it }
        ) {
            showSpaceEffectOptions = false
            onDismissRequest()
        }
    }
}

/**
 * Displays a time picker in an AlertDialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunTimeOptions(
    currentSunTime: Instant,
    setTime: (Instant) -> Unit,
    onDismissRequest: () -> Unit
) {
    val currentTime = currentSunTime.atOffset(ZoneOffset.UTC)
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                setTime(
                    OffsetDateTime.now(ZoneId.of("UTC"))
                        .withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute)
                        .toInstant()
                )
                onDismissRequest()
            }) {
                Text("Confirm")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

/**
 * Displays an AlertDialog with a dropdown selection of different [LightingMode]s
 */
@Composable
fun SunLightingOptions(
    currentLightingMode: LightingMode,
    setSunLighting: (LightingMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    val lightingModes = listOf("No Light", "Light", "Light and Shadows")
    DropdownMenuAlertDialog(
        itemList = lightingModes,
        currentSelectedIndex = when (currentLightingMode) {
            LightingMode.NoLight -> 0
            LightingMode.Light -> 1
            LightingMode.LightAndShadows -> 2
        },
        title = "Select a lighting mode",
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val lightingMode = when (it) {
                0 -> LightingMode.NoLight
                1 -> LightingMode.Light
                2 -> LightingMode.LightAndShadows
                else -> LightingMode.NoLight
            }
            setSunLighting(lightingMode)
            onDismissRequest()
        }
    )
}

/**
 * Displays an AlertDialog with sliders for selecting the components of an RGBA color
 */
@Composable
fun AmbientLightColorOptions(
    currentColor: Color,
    onSetColor: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var r by remember { mutableIntStateOf((currentColor.red * 255).toInt()) }
    var g by remember { mutableIntStateOf((currentColor.green * 255).toInt()) }
    var b by remember { mutableIntStateOf((currentColor.blue * 255).toInt()) }
    var a by remember { mutableIntStateOf((currentColor.alpha * 255).toInt()) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onSetColor(Color(r, g, b, a)); onDismissRequest()
            }) {
                Text("Confirm")
            }
        },
        title = {
            Text("Input an RGBA color")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RgbaSlider(value = r, onValueChange = { r = it }, label = "Red")
                RgbaSlider(value = g, onValueChange = { g = it }, label = "Green")
                RgbaSlider(value = b, onValueChange = { b = it }, label = "Blue")
                RgbaSlider(value = a, onValueChange = { a = it }, label = "Alpha")
            }
        }
    )
}

/**
 * Displays an AlertDialog with a dropdown selection of different [AtmosphereEffect]s
 */
@Composable
fun AtmosphereEffectOptions(
    currentAtmosphereEffect: AtmosphereEffect,
    onSetAtmosphereEffect: (AtmosphereEffect) -> Unit,
    onDismissRequest: () -> Unit
) {
    val atmosphereEffects = listOf("None", "Horizon Only", "Realistic")
    DropdownMenuAlertDialog(
        itemList = atmosphereEffects,
        currentSelectedIndex = when (currentAtmosphereEffect) {
            AtmosphereEffect.None -> 0
            AtmosphereEffect.HorizonOnly -> 1
            AtmosphereEffect.Realistic -> 2
        },
        title = "Select an Atmosphere Effect",
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val atmosphereEffect = when (it) {
                0 -> AtmosphereEffect.None
                1 -> AtmosphereEffect.HorizonOnly
                2 -> AtmosphereEffect.Realistic
                else -> AtmosphereEffect.None
            }
            onSetAtmosphereEffect(atmosphereEffect)
            onDismissRequest()
        }
    )
}

/**
 * Displays an AlertDialog with a dropdown selection of different [SpaceEffect]s
 */
@Composable
fun SpaceEffectOptions(
    currentSpaceEffect: SpaceEffect,
    onSetSpaceEffect: (SpaceEffect) -> Unit,
    onDismissRequest: () -> Unit
) {
    val spaceEffects = listOf("Transparent", "Stars")
    DropdownMenuAlertDialog(
        itemList = spaceEffects,
        currentSelectedIndex = when (currentSpaceEffect) {
            SpaceEffect.Transparent -> 0
            SpaceEffect.Stars -> 1
        },
        title = "Select a Space Effect",
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val spaceEffect = when (it) {
                0 -> SpaceEffect.Transparent
                1 -> SpaceEffect.Stars
                else -> SpaceEffect.Transparent
            }
            onSetSpaceEffect(spaceEffect)
            onDismissRequest()
        }
    )
}

data class LightingOptionsState(
    val sunTime: MutableState<Instant>,
    val sunLighting: MutableState<LightingMode>,
    val ambientLightColor: MutableState<Color>,
    val atmosphereEffect: MutableState<AtmosphereEffect>,
    val spaceEffect: MutableState<SpaceEffect>
)
