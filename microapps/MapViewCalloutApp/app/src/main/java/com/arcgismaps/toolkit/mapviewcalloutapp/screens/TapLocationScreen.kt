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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlin.math.roundToInt

/**
 * Displays a composable [MapView] to show a Callout at the tapped location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapLocationScreen(viewModel: MapViewModel) {

    val tappedPoint = viewModel.mapPoint.collectAsState().value
    val offset = viewModel.offset.collectAsState().value
    var calloutLocation = remember { Point(-13185535.98, 4037766.28, SpatialReference(102100)) }
    var rotateOffsetWithGeoView by rememberSaveable { mutableStateOf(false) }
    var isCalloutEnabled by rememberSaveable { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val visibleState = remember { MutableTransitionState(false) }

    if (tappedPoint != null && isCalloutEnabled) {
        calloutLocation = tappedPoint
        visibleState.targetState = true
    } else {
        visibleState.targetState = false
    }

    // the value will be equivalent to half of device width in any orientation
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
            onSingleTapConfirmed = {
                viewModel.setMapPoint(it)
            },
            content =
            {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Callout(
                        modifier = Modifier.fillMaxWidth(),
                        location = calloutLocation,
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
                                            "<i>x</i>    = ${calloutLocation.x.roundToInt()}<br>" +
                                            "<i>y</i>    = ${calloutLocation.y.roundToInt()}<br>" +
                                            "<i>wkid</i> = ${calloutLocation.spatialReference?.wkid}",
                                    htmlFlag = HtmlCompat.FROM_HTML_MODE_COMPACT
                                )
                            }
                        }
                    }
                }
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
                        calloutVisibility = isCalloutEnabled,
                        isCalloutRotationEnabled = rotateOffsetWithGeoView,
                        offset = offset,
                        mapPoint = calloutLocation,
                        onOffsetChange = { viewModel.setOffset(it) },
                        onVisibilityToggled = { isCalloutEnabled = !isCalloutEnabled },
                        onClearMapPointRequest = {
                            calloutLocation = tappedPoint ?: return@CalloutOptions
                            viewModel.clearMapPoint()
                        },
                        onCalloutOffsetRotationToggled = {
                            rotateOffsetWithGeoView = !rotateOffsetWithGeoView
                        }
                    )
                }
            }
        }
    }
}

/**
 * AndroidView wrapper for the view-based [TextView] which is able to display a styled spannable [html].
 * Currently, Compose does not provide a tool to buildAnnotatedString for HTML styled spannable text.
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
    mapPoint: Point?,
    onVisibilityToggled: () -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onCalloutOffsetRotationToggled: () -> Unit,
    onClearMapPointRequest: () -> Unit,
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
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = offset.x.toString(),
                onValueChange = { onOffsetChange(Offset(it.toFloat(), offset.y)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                label = { Text("X-Axis offset (px)") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = offset.y.toString(),
                onValueChange = { onOffsetChange(Offset(offset.x, it.toFloat())) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                label = { Text("Y-Axis offset (px)") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Button(
            enabled = mapPoint != null,
            onClick = { onClearMapPointRequest() }) {
            Text(text = "Clear Callout map point")
        }
    }
}