/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewerapp.screens

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import kotlinx.coroutines.launch

class GeoViewModel(application: Application) : AndroidViewModel(application) {

    val arcGISMap: ArcGISMap =
        ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=3be52fd477a44af2b75bb70c974e9d07")

    val arcGISScene: ArcGISScene =
        ArcGISScene("https://www.arcgis.com/home/item.html?id=aa1c036445824e06a2d1b0e12e02a924")

    val mapViewProxy = MapViewProxy()

    val sceneviewProxy: SceneViewProxy = SceneViewProxy()

    val mapGraphicsOverlay = GraphicsOverlay()

    val sceneGraphicsOverlay = GraphicsOverlay()

    private val _geoViewType : MutableState<GeoViewType> = mutableStateOf(GeoViewType.MapViewType)
    val geoViewType: GeoViewType
        get() = _geoViewType.value

    private val _orientedImageryViewerState: MutableState<OrientedImageryViewerState?> = mutableStateOf(null)
    val orientedImageryViewerState: OrientedImageryViewerState?
        get() = _orientedImageryViewerState.value

    init {
        viewModelScope.launch {
            arcGISMap.load().onSuccess {
                val index = arcGISMap.operationalLayers.filterIsInstance<OrientedImageryLayer>().lastIndex
                if (index > -1) {
                    val orientedImageryLayer = arcGISMap.operationalLayers[index] as OrientedImageryLayer
                    orientedImageryLayer.load().onSuccess {
                        orientedImageryLayer.fullExtent?.let { extent ->
                            mapViewProxy.setViewpoint(Viewpoint(extent))
                        }
                    }
                }
            }
        }
    }

    fun handleSingleTap(event: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            when (_geoViewType.value) {
                GeoViewType.MapViewType -> {
                    mapViewProxy.identifyLayers(event.screenCoordinate, 20.dp, false)
                        .onSuccess { identifyResults ->
                            createViewerState(identifyResults)
                        }.onFailure { error ->
                            Log.e("GeoViewModel", "Identify layers on map failed: ${error.message}")
                        }
                }
                GeoViewType.SceneViewType -> {
                    sceneviewProxy.identifyLayers(event.screenCoordinate, 20.dp, false)
                        .onSuccess { identifyResults ->
                            createViewerState(identifyResults)
                        }.onFailure { error ->
                            Log.e("GeoViewModel", "Identify layers on scene failed: ${error.message}")
                        }
                }
            }
            val lastMapLocation = if (_orientedImageryViewerState.value != null) {
                when (_geoViewType.value) {
                    GeoViewType.MapViewType -> event.mapPoint
                    GeoViewType.SceneViewType -> sceneviewProxy.screenToLocation(event.screenCoordinate).getOrNull()
                }
            } else {
                null
            }
            lastMapLocation?.let { point ->
                _orientedImageryViewerState.value!!.searchImages(point)
            }
        }
    }

    private fun createViewerState(identifyResults: List<IdentifyLayerResult>) {
        if (identifyResults.isNotEmpty()) {
            val identifyResult = identifyResults.firstOrNull { it.layerContent is OrientedImageryLayer }
            if (identifyResult != null) {
                val orientedImageryLayer = identifyResult.layerContent as OrientedImageryLayer
                _orientedImageryViewerState.value = OrientedImageryViewerState(
                    orientedImageryLayer, currentGraphicsOverlay(), viewModelScope
                )
            } else {
                Log.e("GeoViewModel", "No OrientedImageryLayer found in identify results")
            }
        } else {
            Log.e("GeoViewModel", "No identify results found")
        }
    }

    private fun currentGraphicsOverlay(): GraphicsOverlay = when (_geoViewType.value) {
        GeoViewType.MapViewType -> mapGraphicsOverlay
        GeoViewType.SceneViewType -> sceneGraphicsOverlay
    }

    fun toggleGeoViewType() {
        resetGraphicsOverlay()
        _geoViewType.value = when (_geoViewType.value) {
            GeoViewType.MapViewType -> GeoViewType.SceneViewType
            GeoViewType.SceneViewType -> GeoViewType.MapViewType
        }
    }

    private fun resetGraphicsOverlay() {
        mapGraphicsOverlay.graphics.clear()
        sceneGraphicsOverlay.graphics.clear()
        _orientedImageryViewerState.value?.resetAll()
    }

}

sealed class GeoViewType {
    object MapViewType : GeoViewType()
    object SceneViewType : GeoViewType()
}
