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

package com.arcgismaps.toolkit.sceneviewlightingoptionsapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.mapping.view.SpaceEffect
import com.arcgismaps.toolkit.geoviewcompose.SceneView
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
            val point = Point(
                -73.0815,
                -49.3272,
                4059.0,
                SpatialReference.wgs84()
            )
            initialViewpoint = Viewpoint(
                center = point,
                scale = 17000.0,
                camera = Camera(
                    locationPoint = point,
                    heading = 11.0,
                    pitch = 82.0,
                    roll = 0.0
                )
            )
        }
    }

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
            arcGISScene,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
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
 *
 * @param expanded whether the dropdown is currently expanded
 * @param onDismissRequest called when the menu should be dismissed
 * @param lightingOptionsState the [LightingOptionsState] that will be modified when lighting options are selected
 * @since 200.4.0
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
            onSetSunTime = { lightingOptionsState.sunTime.value = it }
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
 *
 * @param currentSunTime the current sun time the composable SceneView is set to
 * @param onSetSunTime called when the sun time should be changed
 * @param onDismissRequest called when the dialog should be dismissed
 * @since 200.4.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunTimeOptions(
    currentSunTime: Instant,
    onSetSunTime: (Instant) -> Unit,
    onDismissRequest: () -> Unit
) {
    val currentTime = currentSunTime.atOffset(ZoneOffset.UTC)
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = true
    )
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Set Sun Time",
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onDismissRequest
                    ) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            onSetSunTime(
                                OffsetDateTime.now(ZoneId.of("UTC"))
                                    .withHour(timePickerState.hour)
                                    .withMinute(timePickerState.minute)
                                    .toInstant()
                            )
                            onDismissRequest()
                        }
                    ) { Text("OK") }
                }
            }
        }
    }
}

/**
 * Displays an AlertDialog with a dropdown selection of different [LightingMode]s
 *
 * @param currentLightingMode the current sun lighting mode set on the composable SceneView
 * @param setSunLighting called when the sun lighting mode should be changed
 * @param onDismissRequest called when the dialog should be dismissed
 * @since 200.4.0
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
 *
 * @param currentColor the current ambient light color set on the composable SceneView
 * @param onSetColor called when the ambient light color should be changed
 * @param onDismissRequest called when this dialog should be dismissed
 * @since 200.4.0
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
 *
 * @param currentAtmosphereEffect the current atmosphere effect set on the composable SceneView
 * @param onSetAtmosphereEffect called when the atmosphere effect should be changed
 * @param onDismissRequest called when this dialog should be dismissed
 * @since 200.4.0
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
 *
 * @param currentSpaceEffect the current space effect set on the composable SceneView
 * @param onSetSpaceEffect called when the space effect should be changed
 * @param onDismissRequest called when this dialog should be dismissed
 * @since 200.4.0
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

/**
 * Represents various lighting options that can be used to configure a composable [SceneView]
 *
 * @property sunTime defines the position of the sun in the scene
 * @property sunLighting configures how light and shadows are displayed in the scene
 * @property ambientLightColor defines the color of the ambient light when the scene uses lighting
 * @property atmosphereEffect configures how the atmosphere in the scene is displayed
 * @property spaceEffect configures how outer space is displayed in the scene
 * @since 200.4.0
 */
data class LightingOptionsState(
    val sunTime: MutableState<Instant>,
    val sunLighting: MutableState<LightingMode>,
    val ambientLightColor: MutableState<Color>,
    val atmosphereEffect: MutableState<AtmosphereEffect>,
    val spaceEffect: MutableState<SpaceEffect>
)

