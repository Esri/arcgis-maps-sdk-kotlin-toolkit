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

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView

/**
 * Displays a composable [MapView] to show a Callout on the tapped Feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(viewModel: MapViewModel) {
    val selectedGeoElement = viewModel.selectedGeoElement.collectAsState().value
    val selectedLayerName = viewModel.selectedLayerName.collectAsState().value
    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    var nullTapLocation by rememberSaveable { mutableStateOf(false) }
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Callout options") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "SettingsIcon") },
                    onClick = { showBottomSheet = true }
                )
            }
        }
    ) { contentPadding ->
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            arcGISMap = viewModel.arcGISMapWithFeatureLayer,
            mapViewProxy = viewModel.mapViewProxy,
            insets = PaddingValues(horizontal = 12.dp),
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                viewModel.apply {
                    // clears the tapped location and reset selected geoelement
                    clearTapLocationAndGeoElement()
                    // sets the new tapped location and adds a graphic
                    setTapLocation(singleTapConfirmedEvent.mapPoint, nullTapLocation)
                    // identify the tapped layer and the features attributes
                    identify(singleTapConfirmedEvent)
                    // animate the map to recenter to tapped point
                    recenterMap(singleTapConfirmedEvent.mapPoint)
                }
            },
            content = if (selectedGeoElement != null && calloutVisibility) {
                {
                    Callout(
                        modifier = Modifier
                            .wrapContentSize()
                            .height(200.dp)
                            .widthIn(max = 500.dp),
                        geoElement = selectedGeoElement,
                        tapLocation = viewModel.tapLocation.value,
                    ) {
                        CalloutContent(
                            onCloseIconClick = viewModel::clearTapLocationAndGeoElement,
                            selectedElementAttributes = filterAttributes(selectedGeoElement.attributes),
                            layerName = selectedLayerName
                        )
                    }
                }
            } else {
                null
            }
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier.padding(contentPadding),
                onDismissRequest = { showBottomSheet = false },
                sheetState = modalBottomSheetState
            ) {
                Box(Modifier.navigationBarsPadding()) {
                    CalloutOptions(
                        calloutVisibility = calloutVisibility,
                        onVisibilityToggled = { calloutVisibility = !calloutVisibility },
                        passNullTapLocation = nullTapLocation,
                        onNullTapLocationToggled = { nullTapLocation = !nullTapLocation }
                    )
                }
            }
        }
    }
}

/**
 * Content for the Callout to display information on the tapped Layer and it's [selectedElementAttributes]
 */
@Composable
fun CalloutContent(
    onCloseIconClick: () -> Unit,
    selectedElementAttributes: Map<String, Any?>,
    layerName: String
) {
    LazyColumn(contentPadding = PaddingValues(8.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Layer: $layerName",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onCloseIconClick) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null
                    )
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }
        selectedElementAttributes.forEach { attribute ->
            item {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${attribute.key}:",
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "${attribute.value}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Callout visibility and tap location options, displayed in a BottomSheet.
 */
@Composable
fun CalloutOptions(
    calloutVisibility: Boolean,
    onVisibilityToggled: () -> Unit,
    passNullTapLocation: Boolean,
    onNullTapLocationToggled: () -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Enable Callout:", style = MaterialTheme.typography.labelMedium)
            Checkbox(
                checked = calloutVisibility,
                onCheckedChange = { onVisibilityToggled() }
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Use tapped location:", style = MaterialTheme.typography.labelMedium)
            Checkbox(
                checked = !passNullTapLocation,
                onCheckedChange = { onNullTapLocationToggled() }
            )
        }
    }
}

/**
 * Filter undesired feature attributes like, empty or null values and GlobalIDs.
 */
private fun filterAttributes(attributes: Map<String, Any?>): Map<String, Any?> {
    return attributes
        .filter { attribute -> attribute.value != null }
        .filter { attribute -> attribute.value.toString().trim().isNotEmpty() }
        .filter { attribute -> !attribute.key.contains("GlobalID") }
}
