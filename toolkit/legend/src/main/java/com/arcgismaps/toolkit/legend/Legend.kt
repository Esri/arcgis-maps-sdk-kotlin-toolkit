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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import com.arcgismaps.mapping.layers.LegendInfo
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.toolkit.legend.internal.generateLegendContent
import com.arcgismaps.toolkit.legend.internal.getGeoModelLayersInOrder
import com.arcgismaps.toolkit.legend.internal.loadLayers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
public fun Legend(
//    viewModel: LegendInterface,
    geoModel: GeoModel,
    geoViewLayerViewStateChanged: GeoView.GeoViewLayerViewStateChanged?,
//    viewPoint: Viewpoint?,
    modifier: Modifier = Modifier,
    reverseLayerOrder: Boolean = false,
    respectScaleRange: Boolean = true,
    title: String = "Legend"
    ) {

//    val geoModelLayers = remember(geoModel, reverseLayerOrder) {
//        getGeoModelLayersInOrder(geoModel, reverseLayerOrder)
//    }
//
//    if (geoModelLayers.isNotEmpty()) {
//        LaunchedEffect(geoModel, geoViewLayerViewStateChanged) {
//            loadLayers(geoModelLayers)
//        }
//    }
    val initializationStatus = rememberSaveable { mutableStateOf(InitializationStatus.NOT_INITIALIZED) }

    when (initializationStatus.value) {
        InitializationStatus.NOT_INITIALIZED -> {
            // Handle not initialized state
            getLegendContent(
                geoModel,
                geoViewLayerViewStateChanged,
                reverseLayerOrder,
                respectScaleRange,
                updateInitializationStatus = {
                    initializationStatus.value = it
                }
            )

        }

        InitializationStatus.INITIALIZING -> {
            // Handle initializing state
            Text("Initializing")
        }

        InitializationStatus.INITIALIZED -> {
            // Handle initialized state
            Log.e("Legend **", "Initialized")
        }
    }

//    getLegendContent(geoModel, geoViewLayerViewStateChanged, reverseLayerOrder, respectScaleRange)

}

@Composable
internal fun getLegendContent(
    geoModel: GeoModel,
    geoViewLayerViewStateChanged: GeoView.GeoViewLayerViewStateChanged?,
    reverseLayerOrder: Boolean,
    respectScaleRange: Boolean,
    updateInitializationStatus: (InitializationStatus) -> Unit
): Unit {
//        SnapshotStateList<Any> {
    val LegendContent = remember {
        mutableStateListOf<Any>()
    }
    val legendInfos = remember {
        mutableStateMapOf<LayerContent, List<LegendInfo>>()
    }
//    val geoModelLoadStatus = geoModel.loadStatus.collectAsState()
//    Log.d("getLegendContent **", "geoModelLoadStatus: ${geoModelLoadStatus.value}")
//    if (geoModelLoadStatus.value != LoadStatus.Loaded) {
////        return remember { mutableStateListOf() }
//        Log.d("getLegendContent **", "GeoModel is not loaded")
//        return
//    }

    val geoModelLayersInOrder: SnapshotStateList<Layer> = getGeoModelLayersInOrder(geoModel, reverseLayerOrder)

    if (geoModelLayersInOrder.isNotEmpty()) {
        LaunchedEffect(geoModel, geoViewLayerViewStateChanged) {
            geoModel.load().onSuccess {
                Log.d("getLegendContent **", "LaunchedEffect started")
                loadLayers(geoModelLayersInOrder, legendInfos)
                generateLegendContent(LegendContent, geoModelLayersInOrder, legendInfos)
                Log.d("getLegendContent **", "Updating initialization status to INITIALIZED")
                updateInitializationStatus(InitializationStatus.INITIALIZED)
            }
        }
    }


//    return remember { mutableStateListOf() }
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

internal enum class InitializationStatus {
    NOT_INITIALIZED,
    INITIALIZING,
    INITIALIZED
}

