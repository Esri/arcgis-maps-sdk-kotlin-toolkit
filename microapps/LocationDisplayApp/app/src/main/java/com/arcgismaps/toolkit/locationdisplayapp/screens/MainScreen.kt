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

package com.arcgismaps.toolkit.locationdisplayapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.rememberLocationDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Displays a [Map] with a [LocationDisplay] obtained with [rememberLocationDisplay].
 * The location display can be started/stopped using a [Switch].
 * If the location display fails to start, an error message is displayed below the switch.
 * An action button provides a choice of [LocationDisplayAutoPanMode] options. Upon selecting an
 * option, the auto pan mode of the location display is set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
    var checked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val locationDisplay = rememberLocationDisplay {
        start(scope) {
            checked = it.isSuccess
            it.onFailure { error ->
                errorMessage = error.message ?: "Failed to start location display"
            }
        }
    }

    Scaffold(
        topBar = {
            var actionsExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Location Display App")
                },
                actions = {
                    Column {
                        Switch(
                            checked,
                            onCheckedChange = {
                                if (locationDisplay.isStarted) {
                                    locationDisplay.stop(scope)
                                    checked = false
                                } else {
                                    locationDisplay.start(scope) { result ->
                                        checked = result.isSuccess
                                        result.onFailure { error ->
                                            errorMessage = error.message ?: "Failed to start location display"
                                        }
                                    }
                                }
                            }
                        )
                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage)
                        }
                    }
                    IconButton(
                        onClick = {
                            actionsExpanded = !actionsExpanded
                        }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    AutoPanModeDropDownMenu(
                        locationDisplay = locationDisplay,
                        expanded = actionsExpanded,
                        onDismissRequest = {
                            actionsExpanded = false
                        }
                    )
                }
            )
        },
    ) { innerPadding ->
        Map(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            arcGISMap = arcGISMap,
            locationDisplay = locationDisplay
        )
    }
}

/**
 * A drop down menu providing auto pan options for [locationDisplay].
 */
@Composable
fun AutoPanModeDropDownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onDismissRequest: (() -> Unit) = {},
    locationDisplay: LocationDisplay
) {
    val items = remember {
        listOf(
            LocationDisplayAutoPanMode.Off,
            LocationDisplayAutoPanMode.CompassNavigation,
            LocationDisplayAutoPanMode.Navigation,
            LocationDisplayAutoPanMode.Recenter
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        items.forEach {
            DropdownMenuItem(
                text = {
                    Text(text = it.label)
                },
                onClick = { 
                    locationDisplay.setAutoPanMode(it)
                    onDismissRequest()
                })
        }
    }
}

/**
 * Extension property on [LocationDisplay] to check its started status.
 */
private val LocationDisplay.isStarted: Boolean
    get() = dataSource.status.value == LocationDataSourceStatus.Started

/**
 * Extension function on [LocationDisplay] to start [this].
 * [onCompletion] is called when starting has completed.
 */
private fun LocationDisplay.start(
    scope: CoroutineScope,
    onCompletion: ((Result<Unit>) -> Unit)? = null
) {
    scope.launch {
        dataSource.start().let { result ->
            onCompletion?.let {
                it.invoke(result)
            }
        }
    }
}

/**
 * Extension function on [LocationDisplay] to stop [this].
 */
private fun LocationDisplay.stop(scope: CoroutineScope) {
    scope.launch {
        dataSource.stop()
    }
}

/**
 * A label string for a [LocationDisplayAutoPanMode].
 */
private val LocationDisplayAutoPanMode.label: String
    get() = when (this) {
        LocationDisplayAutoPanMode.Off -> "Off"
        LocationDisplayAutoPanMode.Navigation -> "Navigation"
        LocationDisplayAutoPanMode.Recenter -> "Recenter"
        LocationDisplayAutoPanMode.CompassNavigation -> "Compass Navigation"
    }
