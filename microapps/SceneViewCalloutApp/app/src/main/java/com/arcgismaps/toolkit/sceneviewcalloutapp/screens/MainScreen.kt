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

package com.arcgismaps.toolkit.sceneviewcalloutapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SceneViewModel = viewModel()) {
    val tapLocation = viewModel.tapLocation.collectAsState().value
    val offset = viewModel.offset.collectAsState().value

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )

    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    var rotateOffsetWithGeoView by rememberSaveable { mutableStateOf(false) }

    BottomSheetScaffold(
        sheetContent = {
            CalloutOptions(
                calloutVisibility = calloutVisibility,
                isCalloutRotationEnabled = rotateOffsetWithGeoView,
                offset = offset,
                onVisibilityToggled = { calloutVisibility = !calloutVisibility },
                onCalloutOffsetRotationToggled = { rotateOffsetWithGeoView = !rotateOffsetWithGeoView },
                onXAxisOffsetChanged = {
                    viewModel.setOffset(Offset(it,offset.y))
                },
                onYAxisOffsetChanged = {
                    viewModel.setOffset(Offset(offset.x,it))
                },
                onDismissCallout = viewModel::clearTapLocation
            )
        },
        scaffoldState = bottomSheetScaffoldState,
    ) {
        SceneView(
            modifier = Modifier
                .fillMaxSize(),
            arcGISScene = viewModel.arcGISScene,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            sceneViewProxy = viewModel.sceneViewProxy,
            onSingleTapConfirmed = viewModel::setTapLocation,
            content = if (tapLocation != null && calloutVisibility) {
                {
                    Callout(
                        modifier = Modifier.wrapContentSize(),
                        location = tapLocation,
                        rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                        offset = offset
                    ) {
                        Text("Tapped location:\n${tapLocation.x.roundToInt()},${tapLocation.y.roundToInt()}")
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
    onVisibilityToggled: () -> Unit,
    onCalloutOffsetRotationToggled: () -> Unit,
    onXAxisOffsetChanged: (Float) -> Unit,
    onYAxisOffsetChanged: (Float) -> Unit,
    onDismissCallout: () -> Unit
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
                onValueChange = { value ->
                    onXAxisOffsetChanged(value.toFloat())
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                label = { Text("X-Axis offset") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = offset.y.toString(),
                onValueChange = { value ->
                    onYAxisOffsetChanged(value.toFloat())
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                label = { Text("Y-Axis offset") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
        }
        Button(onClick = onDismissCallout) {
            Text(text = "Clear Tap Location")
        }
    }
}
