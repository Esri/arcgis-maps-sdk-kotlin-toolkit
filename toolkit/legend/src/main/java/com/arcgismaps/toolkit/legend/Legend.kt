/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.legend

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.GeoView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
public fun Legend(
    legendState: LegendState,
    geoViewLayerViewStateChanged: GeoView.GeoViewLayerViewStateChanged?,
//    viewPoint: Viewpoint?,
    modifier: Modifier = Modifier,
    title: String = "Legend"
    ) {
    val initializationStatus by legendState.initializationStatus
    LaunchedEffect(legendState) {
        legendState.initialize()
    }

    val subLayerListUpdated by legendState.sublayerListUpdated
    LaunchedEffect (subLayerListUpdated) {
        legendState.sublayersList.forEach { layerContent ->
//            Log.e("Legend **", "SubLayerContents: layername: ${layerContent.name} isVisible - ${layerContent.isVisible}" )
//            Log.d("Legend **", "Layer ${layerContent.name} sublayers updated")
            launch {
                layerContent.subLayerContents.collect { sublayers ->
                    Log.d("Legend **", "Layer ${layerContent.name} has ${sublayers.size} sublayers")
                    Log.e(
                        "Legend **",
                        "SubLayerContents: layername: ${layerContent.name} isVisible - ${layerContent.isVisible}"
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (initializationStatus) {
            is InitializationStatus.NotInitialized, InitializationStatus.Initializing -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is InitializationStatus.FailedToInitialize -> {
            }

            else -> {
               Legend(modifier = modifier, map = ArcGISMap(), geoViewLayerViewStateChanged = geoViewLayerViewStateChanged, viewPoint = null)
            }
        }
    }
}

@Composable
private fun Legend(
    modifier: Modifier = Modifier,
    map: ArcGISMap,
    geoViewLayerViewStateChanged: GeoView.GeoViewLayerViewStateChanged?,
    viewPoint: Viewpoint?
) {
    Box (modifier = modifier) {
        Text("Legend")
    }
}

@Preview
@Composable
internal fun LegendPreview() {
    val viewModel = object: LegendInterface {
        private val _someProperty: MutableStateFlow<String> = MutableStateFlow("Hello Legend Preview")
        override val someProperty: StateFlow<String> = _someProperty.asStateFlow()
    }
//    Legend(viewModel, ArcGISMap(), GeoView.GeoViewLayerViewStateChanged(), Viewpoint())
}


