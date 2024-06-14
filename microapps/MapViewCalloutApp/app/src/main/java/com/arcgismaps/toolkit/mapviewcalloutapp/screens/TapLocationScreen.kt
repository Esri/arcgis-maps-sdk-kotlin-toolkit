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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlin.math.roundToInt

/**
 * Displays a composable [MapView] that displays a [Callout] at the tapped location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapLocationScreen(viewModel: MapViewModel) {

    val mapPoint = viewModel.mapPoint.collectAsState().value
    val offset = viewModel.offset.collectAsState().value

    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    var rotateOffsetWithGeoView by rememberSaveable { mutableStateOf(false) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        sheetContent = {
            CalloutOptions(
                calloutVisibility = calloutVisibility,
                isCalloutRotationEnabled = rotateOffsetWithGeoView,
                offset = offset,
                viewModel = viewModel,
                onVisibilityToggled = { calloutVisibility = !calloutVisibility },
                onCalloutOffsetRotationToggled = {
                    rotateOffsetWithGeoView = !rotateOffsetWithGeoView
                },
            )
        },
        scaffoldState = bottomSheetScaffoldState,
    ) { paddingValues ->
        MapView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            arcGISMap = viewModel.arcGISMap,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = viewModel::setMapPoint,
            content = if (mapPoint != null && calloutVisibility) {
                {
                    Callout(
                        modifier = Modifier.wrapContentSize(),
                        location = mapPoint,
                        rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                        offset = offset
                    ) {
                        Text("Tapped location:\n${mapPoint.x.roundToInt()},${mapPoint.y.roundToInt()}")
                    }
                }
            } else {
                null
            }
        )
    }
}

@Composable
fun CalloutOptions(
    calloutVisibility: Boolean,
    isCalloutRotationEnabled: Boolean,
    offset: Offset,
    viewModel: MapViewModel,
    onVisibilityToggled: () -> Unit,
    onCalloutOffsetRotationToggled: () -> Unit,
) {
    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Show Callout")
            Checkbox(
                checked = calloutVisibility,
                onCheckedChange = { onVisibilityToggled() }
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Rotate offset")
            Checkbox(
                checked = isCalloutRotationEnabled,
                onCheckedChange = { onCalloutOffsetRotationToggled() }
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            OffsetDropDownMenu(
                modifier = Modifier.weight(1f),
                offsetValue = offset.x,
                offsetName = "X-Axis offset",
                onOffsetSelected = { xOffset ->
                    viewModel.setOffset(Offset(xOffset, offset.y))
                }
            )
            Spacer(modifier = Modifier.size(10.dp))
            OffsetDropDownMenu(
                modifier = Modifier.weight(1f),
                offsetValue = offset.y,
                offsetName = "Y-Axis offset",
                onOffsetSelected = { yOffset ->
                    viewModel.setOffset(Offset(offset.x, yOffset))
                }
            )
        }

        Button(onClick = viewModel::clearMapPoint) {
            Text(text = "Clear Tap Location")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffsetDropDownMenu(
    modifier: Modifier,
    offsetValue: Float,
    offsetName: String,
    onOffsetSelected: (Float) -> Unit,
) {
    val offsetItems = listOf(-100.0, -50.0, -25.0, 0.0, 25.0, 50.0, 100.0)
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            value = offsetValue.toString(),
            readOnly = true,
            onValueChange = { },
            label = { Text(offsetName) },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            offsetItems.forEach {
                DropdownMenuItem(
                    text = { Text(it.toString()) },
                    onClick = {
                        onOffsetSelected(it.toFloat())
                        isExpanded = false
                    })
            }
        }
    }
}
