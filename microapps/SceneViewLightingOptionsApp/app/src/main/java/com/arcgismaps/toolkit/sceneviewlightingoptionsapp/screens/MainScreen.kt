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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.toolkit.geocompose.SceneView
import com.arcgismaps.toolkit.geocompose.SceneViewpointOperation
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISImagery) }
    val sofia = remember {
        Point(23.321736, 42.697703, SpatialReference.wgs84())
    }
    val camera = remember { Camera(sofia, 10000.0, 0.0, 80.0, 0.0) }
    val viewpointOperation = SceneViewpointOperation.SetCamera(camera)

    var sunTime by remember { mutableStateOf(Instant.parse("2000-09-22T15:00:00Z")) }
    var sunLighting: LightingMode by remember { mutableStateOf(LightingMode.LightAndShadows) }
    var ambientLightColor: Color by remember { mutableStateOf(Color(220, 220, 220, 255)) }

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
                    IconButton(
                        onClick = {
                            optionsExpanded = !optionsExpanded
                        }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    OptionsDropDownMenu(
                        expanded = optionsExpanded,
                        onDismissRequest = { optionsExpanded = false },
                        onSetSunTime = { sunTime = it },
                        currentLightingMode = sunLighting,
                        onSetSunLighting = { sunLighting = it },
                        currentAmbientLightColor = ambientLightColor,
                        onSetAmbientLightColor = { ambientLightColor = it }
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
            sunTime = sunTime,
            sunLighting = sunLighting,
            ambientLightColor = ambientLightColor
        )
    }
}

/**
 * A drop down menu providing auto pan options for which settings to change.
 */
@Composable
fun OptionsDropDownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onDismissRequest: (() -> Unit) = {},
    onSetSunTime: (Instant) -> Unit,
    currentLightingMode: LightingMode,
    onSetSunLighting: (LightingMode) -> Unit,
    currentAmbientLightColor: Color,
    onSetAmbientLightColor: (Color) -> Unit
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
                    }
                })
        }
    }
    if (showSunTimeOptions) {
        SunTimeOptions(setTime = onSetSunTime) {
            showSunTimeOptions = false
        }
    }
    if (showSunLightingOptions) {
        SunLightingOptions(
            currentLightingMode = currentLightingMode,
            setSunLighting = onSetSunLighting
        ) {
            showSunLightingOptions = false
        }
    }
    if (showAmbientLightColorOptions) {
        AmbientLightColorOptions(
            currentColor = currentAmbientLightColor,
            onSetColor = onSetAmbientLightColor
        ) {
            showAmbientLightColorOptions = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunTimeOptions(setTime: (Instant) -> Unit, onDismissRequest: () -> Unit) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                setTime(
                    OffsetDateTime.now(ZoneId.systemDefault())
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

@Composable
fun SunLightingOptions(
    currentLightingMode: LightingMode,
    setSunLighting: (LightingMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedLightingMode by remember { mutableStateOf(currentLightingMode) }
    val getLightingModeName: (LightingMode) -> String = remember {
        {
            when (it) {
                LightingMode.NoLight -> "No Light"
                LightingMode.Light -> "Light"
                LightingMode.LightAndShadows -> "Light and Shadows"
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                setSunLighting(selectedLightingMode)
                onDismissRequest()
            }) {
                Text("Confirm")
            }
        },
        title = {
            Text("Select a lighting mode:")
        },
        text = {
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        dropdownExpanded = !dropdownExpanded
                    }
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        getLightingModeName(selectedLightingMode),
                        modifier = Modifier.padding(8.dp)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        "Select Item",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(getLightingModeName(LightingMode.NoLight)) },
                    onClick = {
                        selectedLightingMode = LightingMode.NoLight
                        dropdownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(getLightingModeName(LightingMode.Light)) },
                    onClick = {
                        selectedLightingMode = LightingMode.Light
                        dropdownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(getLightingModeName(LightingMode.LightAndShadows)) },
                    onClick = {
                        selectedLightingMode = LightingMode.LightAndShadows
                        dropdownExpanded = false
                    }
                )
            }
        }
    )
}

@Composable
fun AmbientLightColorOptions(
    currentColor: Color,
    onSetColor: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var r by remember { mutableIntStateOf(currentColor.red.toColorComponentInt()) }
    var g by remember { mutableIntStateOf(currentColor.green.toColorComponentInt()) }
    var b by remember { mutableIntStateOf(currentColor.blue.toColorComponentInt()) }
    var a by remember { mutableIntStateOf(currentColor.alpha.toColorComponentInt()) }
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
            Column {
                RgbaTextField(value = r, onValueChange = { r = it }, label = "Red")
                RgbaTextField(value = g, onValueChange = { g = it }, label = "Green")
                RgbaTextField(value = b, onValueChange = { b = it }, label = "Blue")
                RgbaTextField(value = a, onValueChange = { a = it }, label = "Alpha")
            }
        }
    )
}

@Composable
fun RgbaTextField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = value.toString(),
        onValueChange = {
            val valueAsInt = (it.toIntOrNull() ?: 0)
                .coerceAtLeast(0)
                .coerceAtMost(255)
            onValueChange(valueAsInt)
        },
        modifier = Modifier
            .padding(8.dp),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions {
            focusManager.moveFocus(FocusDirection.Next)
        },
    )
}

private fun Float.toColorComponentInt(): Int {
    return (this * 255).toInt()
}