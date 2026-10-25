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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GeoViewModel(application: Application) : AndroidViewModel(application) {

    val arcGISMap: ArcGISMap =
        ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=c820cf66dc4a45aea7f84a94bd071a8d")

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

    private val _isLoading: MutableState<Boolean> = mutableStateOf(true)
    val isLoading: Boolean
        get() = _isLoading.value

    private val _isHandlingTap: MutableState<Boolean> = mutableStateOf(false)
    val isHandlingTap: Boolean
        get() = _isHandlingTap.value

    private val _statusMessage: MutableState<String?> = mutableStateOf(null)
    val statusMessage: String?
        get() = _statusMessage.value

    private var loadGeoViewJob: Job? = null
    private var tapJob: Job? = null

    init {
        loadGeoView(geoViewType)
    }

    private fun loadGeoView(geoViewType: GeoViewType) {
        loadGeoViewJob?.cancel()
        val geoModel = when (geoViewType) {
            GeoViewType.MapViewType -> arcGISMap
            GeoViewType.SceneViewType -> arcGISScene
        }
        val graphicsOverlay = when (geoViewType) {
            GeoViewType.MapViewType -> mapGraphicsOverlay
            GeoViewType.SceneViewType -> sceneGraphicsOverlay
        }
        val modelName = when (geoViewType) {
            GeoViewType.MapViewType -> "map"
            GeoViewType.SceneViewType -> "scene"
        }

        _isLoading.value = true
        _statusMessage.value = null
        loadGeoViewJob = viewModelScope.launch {
            var failureMessage = "Failed to load the $modelName."
            try {
                val needsLoading = geoModel.loadStatus.value != LoadStatus.Loaded
                if (needsLoading) {
                    geoModel.load().getOrThrow()
                }
                currentCoroutineContext().ensureActive()

                val orientedImageryLayer = geoModel.operationalLayers
                    .filterIsInstance<OrientedImageryLayer>()
                    .firstOrNull()
                if (orientedImageryLayer == null) {
                    _statusMessage.value = "No OrientedImageryLayers found in the $modelName."
                    return@launch
                }

                failureMessage = "Failed to load the oriented imagery layer."
                if (orientedImageryLayer.loadStatus.value != LoadStatus.Loaded) {
                    orientedImageryLayer.load().getOrThrow()
                }
                currentCoroutineContext().ensureActive()

                if (needsLoading) {
                    orientedImageryLayer.fullExtent?.let { extent ->
                        when (geoViewType) {
                            GeoViewType.MapViewType -> mapViewProxy.setViewpoint(Viewpoint(extent))
                            GeoViewType.SceneViewType -> sceneviewProxy.setViewpoint(Viewpoint(extent))
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                _orientedImageryViewerState.value = OrientedImageryViewerState(
                    orientedImageryLayer,
                    graphicsOverlay,
                    viewModelScope
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _statusMessage.value = failureMessage
                Log.e("GeoViewModel", "$failureMessage ${error.message}", error)
            } finally {
                if (currentCoroutineContext().isActive) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun onSingleTapOnGeoView(event: SingleTapConfirmedEvent) {
        if (isLoading || isHandlingTap) return
        val viewerState = orientedImageryViewerState ?: return
        val geoViewType = geoViewType
        val isMarkingLocation = viewerState.isMarkingLocation
        _statusMessage.value = null
        _isHandlingTap.value = true
        tapJob = viewModelScope.launch {
            val failureMessage = if (isMarkingLocation) {
                "Failed to mark the selected location."
            } else {
                "Search for oriented images failed."
            }
            try {
                val mapPoint = when (geoViewType) {
                    GeoViewType.MapViewType -> event.mapPoint
                    GeoViewType.SceneViewType -> sceneviewProxy
                        .screenToLocation(event.screenCoordinate).getOrThrow()
                } ?: throw IllegalStateException("No map location was found for the selected point.")
                currentCoroutineContext().ensureActive()
                viewerState.onSingleTapOnGeoView(mapPoint).getOrThrow()
                currentCoroutineContext().ensureActive()
                if (!isMarkingLocation && !viewerState.hasActiveImage) {
                    _statusMessage.value = "No oriented images were found at the selected location."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _statusMessage.value = error.message?.let { "$failureMessage $it" } ?: failureMessage
                Log.e("GeoViewModel", failureMessage, error)
            } finally {
                if (currentCoroutineContext().isActive) {
                    _isHandlingTap.value = false
                }
            }
        }
    }

    fun toggleGeoViewType() {
        tapJob?.cancel()
        _isHandlingTap.value = false
        resetGraphicsOverlay()
        _orientedImageryViewerState.value = null
        _geoViewType.value = when (_geoViewType.value) {
            GeoViewType.MapViewType -> GeoViewType.SceneViewType
            GeoViewType.SceneViewType -> GeoViewType.MapViewType
        }
        loadGeoView(geoViewType)
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
