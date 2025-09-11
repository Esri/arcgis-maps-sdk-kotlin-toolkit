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

package com.arcgismaps.toolkit.calloutapp.screens

import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.Ref
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.GeoViewScope
import com.arcgismaps.toolkit.geoviewcompose.LeaderPosition
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import kotlin.math.roundToInt

/**
 * Displays a composable [MapView] to show a Callout at the tapped location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapLocationScreen(
    viewModel: MapViewModel,
    screenTitle: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit
) {
    val mapPoint = viewModel.point.collectAsState().value
    val offset = viewModel.offset.collectAsState().value
    val tapLocationGraphicsOverlay = viewModel.tapLocationGraphicsOverlay.collectAsState().value
    val isGeoViewMapView = viewModel.isGeoViewMapView.collectAsState().value
    var rotateOffsetWithGeoView by rememberSaveable { mutableStateOf(false) }
    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // animate to a visible transition state
    val calloutVisibleState = remember { MutableTransitionState(false) }.apply {
        targetState = mapPoint != null && calloutVisibility
    }

    Scaffold(
        topBar = { CalloutAppBar(screenTitle, canNavigateBack, navigateUp) },
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
        @Composable
        fun GeoViewScope.GeoViewScopeContent() {
            val lastMapPoint = remember { Ref<Point>() }
            lastMapPoint.value = mapPoint ?: lastMapPoint.value

            AnimatedVisibility(
                calloutVisibleState,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                lastMapPoint.value?.let {
                    Callout(
                        modifier = Modifier.wrapContentSize(),
                        location = it,
                        leaderPosition = LeaderPosition.Automatic,
                        rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                        offset = offset
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            HtmlText(
                                html = "<b>Tapped location</b>:<br>" +
                                        "<i>x</i>    = ${it.x.roundToInt()}<br>" +
                                        "<i>y</i>    = ${it.y.roundToInt()}<br>" +
                                        "<i>wkid</i> = ${it.spatialReference?.wkid}",
                                htmlFlag = HtmlCompat.FROM_HTML_MODE_COMPACT,
                                textColor = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }
        if (isGeoViewMapView) {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy,
                graphicsOverlays = listOf(tapLocationGraphicsOverlay),
                onSingleTapConfirmed = viewModel::setPoint,
                content = { GeoViewScopeContent() }
            )
        } else {
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                arcGISScene = viewModel.arcGISScene,
                sceneViewProxy = viewModel.sceneViewProxy,
                graphicsOverlays = listOf(tapLocationGraphicsOverlay),
                onSingleTapConfirmed = viewModel::setPoint,
                content = { GeoViewScopeContent() }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier.padding(contentPadding),
                onDismissRequest = { showBottomSheet = false },
                sheetState = modalBottomSheetState
            ) {
                Box(Modifier.navigationBarsPadding()) {
                    CalloutOptions(
                        isGeoViewMapView = isGeoViewMapView,
                        calloutVisibility = calloutVisibility,
                        isCalloutRotationEnabled = rotateOffsetWithGeoView,
                        offset = offset,
                        mapPoint = mapPoint,
                        onOffsetChange = { viewModel.setOffset(it) },
                        onGeoViewToggled = { viewModel.toggleGeoView() },
                        onVisibilityToggled = { calloutVisibility = !calloutVisibility },
                        onClearMapPointRequest = { viewModel.clearPoint() },
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
fun HtmlText(modifier: Modifier = Modifier, html: String, htmlFlag: Int, textColor: Color) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = {
            it.text = HtmlCompat.fromHtml(html, htmlFlag)
            it.setTextColor(textColor.hashCode())
        }
    )
}

/**
 * Callout visibility and offset options displayed in a BottomSheet.
 */
@Composable
fun CalloutOptions(
    isGeoViewMapView: Boolean,
    calloutVisibility: Boolean,
    isCalloutRotationEnabled: Boolean,
    offset: Offset,
    mapPoint: Point?,
    onGeoViewToggled: () -> Unit,
    onVisibilityToggled: () -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onCalloutOffsetRotationToggled: () -> Unit,
    onClearMapPointRequest: () -> Unit,
) {
    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Select GeoView")
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    onClick = onGeoViewToggled,
                    selected = isGeoViewMapView,
                    label = { Text("MapView") },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    )
                )
                SegmentedButton(
                    onClick = onGeoViewToggled,
                    selected = !isGeoViewMapView,
                    label = { Text("SceneView") },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Show Callout")
            Checkbox(
                checked = calloutVisibility,
                onCheckedChange = { onVisibilityToggled() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Rotate offset")
            Checkbox(
                checked = isCalloutRotationEnabled,
                onCheckedChange = { onCalloutOffsetRotationToggled() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
