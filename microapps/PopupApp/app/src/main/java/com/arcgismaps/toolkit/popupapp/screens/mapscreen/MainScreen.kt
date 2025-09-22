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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.popup.Popup
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
fun MainScreen(viewModel: MapViewModel = viewModel()) {
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
            if (viewModel.popupState != null) {
                Popup(
                    viewModel.popupState!!,
                    Modifier.animateContentSize(),
                    onDismiss = { scope.launch {
                        viewModel.updatePopupState(null)
                        unselectFeature(viewModel.geoElement, viewModel.layer)
                        scaffoldState.bottomSheetState.hide()
                    } }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
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
                    viewModel.proxy.identifyLayers(
                        screenCoordinate = it.screenCoordinate,
                        tolerance = 22.dp,
                        returnPopupsOnly = true
                    ).onSuccess { results ->
                        if (results.isEmpty()) {
                            unselectFeature(viewModel.geoElement, viewModel.layer)
                            viewModel.updatePopupState(null)
                            viewModel.setLayer(null)
                            viewModel.setGeoElement(null)
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                                scaffoldState.bottomSheetState.hide()
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Tap did not identify any Popups",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            try {
                                val result = results.first()
                                var popup = result.popups.first()
                                val newLayer = result.layerContent
                                var newGeoElement = popup.geoElement

                                // if the identified GeoElement is a DynamicEntityObservation,
                                // get the underlying DynamicEntity and create a new Popup with
                                // the same definition.
                                if (newGeoElement is DynamicEntityObservation && newGeoElement.dynamicEntity != null) {
                                    val dynamicEntity = newGeoElement.dynamicEntity
                                    if (dynamicEntity != null) {
                                        newGeoElement = dynamicEntity
                                        popup = Popup(
                                            newGeoElement,
                                            popup.popupDefinition
                                        )
                                    }
                                }

                                if (viewModel.geoElement.sameSelection(newGeoElement)) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "the same GeoElement was selected",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    unselectFeature(
                                        viewModel.geoElement,
                                        viewModel.layer
                                    )

                                    when (newLayer) {
                                        is FeatureLayer -> {
                                            // the Popup exists on a FeatureLayer
                                            if (newGeoElement is Feature) {
                                                // the tap was on a Feature
                                                // unselect any previously selected Feature
                                                newLayer.selectFeature(newGeoElement)
                                            }
                                            // otherwise the tap was on some non-Feature GeoElement
                                            viewModel.setLayer(newLayer)
                                            viewModel.setGeoElement(newGeoElement)
                                        }

                                        is Layer -> {
                                            // the popup exists on a non-FeatureLayer
                                            viewModel.setLayer(newLayer)
                                            viewModel.setGeoElement(newGeoElement)
                                        }

                                        else -> {
                                            // the popup exists on a sublayer, which is a complication
                                            // that doesn't offer any testing value for the Popup
                                            // toolkit component.
                                            throw IllegalStateException("popups on sublayers are not supported by the PopupApp")
                                        }
                                    }
                                    viewModel.updatePopupState(popup)
                                    scaffoldState.bottomSheetState.expand()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "failed to create a Popup for the GeoElement",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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
