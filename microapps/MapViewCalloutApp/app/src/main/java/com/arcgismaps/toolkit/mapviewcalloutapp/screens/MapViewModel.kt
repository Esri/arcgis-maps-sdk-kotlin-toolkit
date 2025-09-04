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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    val mapViewProxy = MapViewProxy()
    val sceneViewProxy = SceneViewProxy()

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        )
    }

    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        )
    }

    val arcGISMapWithFeatureLayer = ArcGISMap(
        uri = "https://www.arcgis.com/home/item.html?id=16f1b8ba37b44dc3884afc8d5f454dd2"
    ).apply {
        initialViewpoint = Viewpoint(
            Point(x = -1.3659e7, y = 5.6917e6),
            scale = 50000.0,
        )
    }


    private val _isGeoViewMapView = MutableStateFlow(false)
    val isGeoViewMapView: StateFlow<Boolean> = _isGeoViewMapView.asStateFlow()

    private val _point = MutableStateFlow<Point?>(null)
    val point: StateFlow<Point?> = _point.asStateFlow()

    private val _selectedGeoElement = MutableStateFlow<GeoElement?>(null)
    val selectedGeoElement: StateFlow<GeoElement?> = _selectedGeoElement.asStateFlow()

    private val _selectedLayerName = MutableStateFlow("")
    val selectedLayerName: StateFlow<String> = _selectedLayerName.asStateFlow()

    private val _tapLocation = MutableStateFlow<Point?>(null)
    val tapLocation: StateFlow<Point?> = _tapLocation.asStateFlow()

    private val _offset = MutableStateFlow(Offset.Zero)
    val offset: StateFlow<Offset> = _offset

    private val _tapLocationGraphicsOverlay = MutableStateFlow(GraphicsOverlay())
    val tapLocationGraphicsOverlay: StateFlow<GraphicsOverlay> = _tapLocationGraphicsOverlay.asStateFlow()

    private var currentIdentifyJob: Job? = null

    fun clearPoint() {
        _point.value = null
        _tapLocationGraphicsOverlay.value.graphics.clear()
        _tapLocationGraphicsOverlay.value = GraphicsOverlay()
    }

    fun setOffset(offset: Offset) {
        _offset.value = offset
    }

    fun setPoint(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        if (_isGeoViewMapView.value)
            _point.value = singleTapConfirmedEvent.mapPoint
        else
            _point.value = sceneViewProxy.screenToBaseSurface(singleTapConfirmedEvent.screenCoordinate)

        _tapLocationGraphicsOverlay.value.graphics.clear()
        _tapLocationGraphicsOverlay.value.graphics.add(
            Graphic(
                geometry = _point.value,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }

    fun toggleGeoView() {
        clearPoint()
        _isGeoViewMapView.value = !_isGeoViewMapView.value
    }

    fun clearTapLocationAndGraphic() {
        _tapLocation.value = null
        _tapLocationGraphicsOverlay.value.graphics.clear()
    }

    fun clearSelectedGeoElement() {
        _selectedGeoElement.value = null
    }

    fun setTapLocation(tapLocation: Point?, nullTapLocation: Boolean) {
        _tapLocation.value = if (nullTapLocation) null else tapLocation

        _tapLocationGraphicsOverlay.value.graphics.clear()
        _tapLocationGraphicsOverlay.value.graphics.add(
            Graphic(
                geometry = tapLocation,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]. The
     * identified geoelement is set to [_selectedGeoElement].
     *
     * @since 200.5.0
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        currentIdentifyJob?.cancel()
        currentIdentifyJob = viewModelScope.launch {
            val result = mapViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 1.dp
            )
            result.onSuccess { identifyLayerResultList ->
                if (identifyLayerResultList.isNotEmpty()) {
                    _selectedGeoElement.value = identifyLayerResultList[0].geoElements.firstOrNull()
                    _selectedLayerName.value = identifyLayerResultList[0].layerContent.name
                } else {
                    _selectedGeoElement.value = null
                }
            }
        }
    }
}
