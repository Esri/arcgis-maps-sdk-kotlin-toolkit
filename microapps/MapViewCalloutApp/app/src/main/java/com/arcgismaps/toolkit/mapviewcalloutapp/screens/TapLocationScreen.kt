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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlin.math.roundToInt

/**
 * Displays a composable [MapView] that displays a [Callout] at the tapped location.
 */
@Composable
fun TapLocationScreen(viewModel: MapViewModel) {

    val mapPoint = viewModel.mapPoint.collectAsState().value
    val offset = viewModel.offset.collectAsState().value

    var calloutVisibility by rememberSaveable { mutableStateOf(false) }
    var rotateOffsetWithGeoView by rememberSaveable { mutableStateOf(false) }

    Column {
        CalloutOptionsBox(
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
            }
        )

        MapView(
            modifier = Modifier
                .fillMaxSize(),
            arcGISMap = viewModel.arcGISMap,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = viewModel::setMapPoint,
            onLongPress = { viewModel.clearMapPoint() },
            content = if (mapPoint != null && calloutVisibility) {
                {
                    Callout(
                        modifier = Modifier.size(175.dp, 75.dp),
                        location = mapPoint,
                        rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                        offset = offset
                    ) {
                        Text("Tapped location: ${mapPoint.x.roundToInt()},${mapPoint.y.roundToInt()}")
                    }
                }
            } else {
                null
            }
        )
    }
}

@Composable
fun CalloutOptionsBox(
    calloutVisibility: Boolean,
    isCalloutRotationEnabled: Boolean,
    offset: Offset,
    onVisibilityToggled: () -> Unit,
    onCalloutOffsetRotationToggled: () -> Unit,
    onXAxisOffsetChanged: (Float) -> Unit,
    onYAxisOffsetChanged: (Float) -> Unit
    ) {
    Column(Modifier.padding(8.dp)) {
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Offset")
            Column {
                TextField(
                    value = offset.x.toString(),
                    onValueChange = { value ->
                        onXAxisOffsetChanged(value.toFloat())
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    label = { Text("X-Axis offset") },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                )
                TextField(
                    value = offset.y.toString(),
                    onValueChange = { value ->
                        onYAxisOffsetChanged(value.toFloat())
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    label = { Text("Y-Axis offset") },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                )
            }
        }
    }
}
