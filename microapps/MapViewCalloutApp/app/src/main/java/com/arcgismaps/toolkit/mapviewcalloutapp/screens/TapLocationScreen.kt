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

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
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
    var showBottomSheet by remember { mutableStateOf(false) }
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mapViewInsets: Dp = LocalConfiguration.current.screenWidthDp.dp / 4

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
            arcGISMap = viewModel.arcGISMap,
            insets = PaddingValues(horizontal = mapViewInsets),
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = viewModel::setMapPoint,
            content = if (mapPoint != null && calloutVisibility) {
                {
                    Callout(
                        modifier = Modifier.fillMaxWidth(),
                        location = mapPoint,
                        rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                        offset = offset
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                HtmlText(
                                    html = "<b>Tapped location</b>:<br>" +
                                            "<i>x</i>    = ${mapPoint.x.roundToInt()}<br>" +
                                            "<i>y</i>    = ${mapPoint.y.roundToInt()}<br>" +
                                            "<i>wkid</i> = ${mapPoint.spatialReference?.wkid}",
                                    htmlFlag = HtmlCompat.FROM_HTML_MODE_COMPACT
                                )
                            }

                            IconButton(onClick = viewModel::clearMapPoint) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null
                                )
                            }
                        }
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
                        isCalloutRotationEnabled = rotateOffsetWithGeoView,
                        offset = offset,
                        viewModel = viewModel,
                        onVisibilityToggled = { calloutVisibility = !calloutVisibility },
                        onCalloutOffsetRotationToggled = {
                            rotateOffsetWithGeoView = !rotateOffsetWithGeoView
                        },
                    )
                }
            }
        }
    }
}

/**
 * AndroidView wrapper for the view-based [TextView] which is able to display a styled spannable [html].
 * Currently, Compose does not provide a tool to [buildAnnotatedString] for HTML styled spannable text.
 */
@Composable
fun HtmlText(modifier: Modifier = Modifier, html: String, htmlFlag: Int) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = { it.text = HtmlCompat.fromHtml(html, htmlFlag) }
    )
}

/**
 * Callout visibility and offset options displayed in a BottomSheet.
 */
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
    }
}

/**
 * ExposedDropdownMenuBox to display the list of Offset's to choose from.
 */
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
