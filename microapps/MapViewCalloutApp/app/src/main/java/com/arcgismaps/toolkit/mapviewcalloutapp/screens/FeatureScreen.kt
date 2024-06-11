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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlin.math.roundToInt

// TODO Case b.
//- Show a MapView with a map with a Feature layer with features
//- Display callout using Point(tap location) on a feature (with some text)
//- Display a graphic at the tapped location
//- add switch to enable/disable animation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(viewModel: MapViewModel) {
    val selectedGeoElement = viewModel.selectedGeoElement.collectAsState().value
    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    var nullTapLocation by rememberSaveable { mutableStateOf(false) }
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        sheetContent = {
            CalloutOptions(
                viewModel = viewModel,
                calloutVisibility = calloutVisibility,
                onVisibilityToggled = { calloutVisibility = !calloutVisibility },
                passNullTapLocation = nullTapLocation,
                onNullTapLocationToggled = { nullTapLocation = !nullTapLocation }
            )
        },
        scaffoldState = bottomSheetScaffoldState,
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = viewModel.arcGISMapWithFeatureLayer,
            mapViewProxy = viewModel.mapViewProxy,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                viewModel.clearTapLocationAndGeoElement()
                viewModel.setTapLocation(singleTapConfirmedEvent.mapPoint, nullTapLocation)
                viewModel.identify(singleTapConfirmedEvent)
            },
            content = if (selectedGeoElement != null && calloutVisibility) {
                {
                    val tapLocation = viewModel.tapLocation.value
                    Callout(
                        geoElement = selectedGeoElement,
                        modifier = Modifier.wrapContentSize(),
                        tapLocation = viewModel.tapLocation.value,
                    ) {
                        Text("Tapped location: ${tapLocation?.x?.roundToInt()},${tapLocation?.y?.roundToInt()}")
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
    viewModel: MapViewModel,
    calloutVisibility: Boolean,
    onVisibilityToggled: () -> Unit,
    passNullTapLocation: Boolean,
    onNullTapLocationToggled: () -> Unit,
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
            Text(text = "Pass null TapLocation")
            Checkbox(
                checked = passNullTapLocation,
                onCheckedChange = { onNullTapLocationToggled() }
            )
        }
        Button(onClick = { viewModel.clearTapLocationAndGeoElement() }) {
            Text(text = "Clear Tap Location")
        }
    }
}
