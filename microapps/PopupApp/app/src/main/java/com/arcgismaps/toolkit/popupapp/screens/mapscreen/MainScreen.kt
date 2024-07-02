/*
 *
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
 *  
 */

package com.arcgismaps.toolkit.popupapp.screens.mapscreen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.realtime.DynamicEntityObservation
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.popup.Popup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun unselectFeature(feature: GeoElement?, layer: Layer?) {
    if (feature is Feature && layer is FeatureLayer) {
        layer.unselectFeature(feature)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MapViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    BottomSheetScaffold(
        sheetContent = {
            AnimatedVisibility(
                visible = viewModel.popup != null,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "popup",
                modifier = Modifier.heightIn(min = 0.dp, max = 400.dp)
            ) {
                if (viewModel.popup != null) {
                    Popup(
                        viewModel.popup!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = true,
        topBar = null
    ) { padding ->
        // show the composable map using the mapViewModel
        MapView(
            arcGISMap = viewModel.map,
            mapViewProxy = viewModel.proxy,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onSingleTapConfirmed = {
                scope.launch {
                    viewModel.proxy.identify(
                        layer = viewModel.dynamicEntityLayer,
                        screenCoordinate = it.screenCoordinate,
                        tolerance = 220.dp,
                        returnPopupsOnly = true
                    ).onSuccess { results ->
                        val observation =
                            results.geoElements.filterIsInstance<DynamicEntityObservation>().firstOrNull()
                        val dynamicEntity = observation?.dynamicEntity
                        if (dynamicEntity != null) {
                            viewModel.setLayer(viewModel.dynamicEntityLayer)
                            viewModel.setGeoElement(dynamicEntity)
                            viewModel.setPopup(results.popups.first())
                            scaffoldState.bottomSheetState.expand()
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "did not tap on DynamicEntity",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                }
            }
        )
    }
}

private fun GeoElement?.sameSelection(other: GeoElement): Boolean =
    if (this == null) {
        false
    } else {
        val geometriesEqual = if (geometry != null && other.geometry != null) {
            this.geometry == other.geometry
        } else if (geometry == null && other.geometry == null) {
            true
        } else {
            false
        }

        if (geometriesEqual) {
            attributes.entries.all {
                other.attributes[it.key]?.let { otherValue ->
                    it.value == otherValue
                } ?: (it.value == null)
            }
        } else {
            false
        }
    }
