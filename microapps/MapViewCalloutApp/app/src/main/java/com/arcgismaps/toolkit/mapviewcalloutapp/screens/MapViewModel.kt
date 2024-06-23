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
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    val mapViewProxy = MapViewProxy()

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
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

    private val _mapPoint = MutableStateFlow<Point?>(null)
    val mapPoint: StateFlow<Point?> = _mapPoint

    private val _selectedGeoElement = MutableStateFlow<GeoElement?>(null)
    val selectedGeoElement: StateFlow<GeoElement?> = _selectedGeoElement

    private val _selectedLayerName = MutableStateFlow("")
    val selectedLayerName: StateFlow<String> = _selectedLayerName

    private val _tapLocation = MutableStateFlow<Point?>(null)
    val tapLocation: StateFlow<Point?> = _tapLocation

    private val _offset = MutableStateFlow(Offset.Zero)
    val offset: StateFlow<Offset> = _offset

    val tapLocationGraphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private var currentIdentifyJob: Job? = null

    fun clearMapPoint() {
        _mapPoint.value = null
        tapLocationGraphicsOverlay.graphics.clear()
    }

    fun setOffset(offset: Offset) {
        _offset.value = offset
    }

    fun setMapPoint(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        _mapPoint.value = singleTapConfirmedEvent.mapPoint

        tapLocationGraphicsOverlay.graphics.clear()
        tapLocationGraphicsOverlay.graphics.add(
            Graphic(
                geometry = _mapPoint.value,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.red, 12.0f)
            )
        )
    }

    fun clearTapLocationAndGeoElement() {
        _tapLocation.value = null
        _selectedGeoElement.value = null
        tapLocationGraphicsOverlay.graphics.clear()
    }

    fun setTapLocation(tapLocation: Point?, nullTapLocation: Boolean) {
        _tapLocation.value = if (nullTapLocation) null else tapLocation

        tapLocationGraphicsOverlay.graphics.clear()
        tapLocationGraphicsOverlay.graphics.add(
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
            val result =
                mapViewProxy.identifyLayers(singleTapConfirmedEvent.screenCoordinate, 20.dp)
            result.onSuccess { identifyLayerResultList ->
                if (identifyLayerResultList.isNotEmpty()) {
                    _selectedGeoElement.value = identifyLayerResultList[0].geoElements.firstOrNull()
                    _selectedLayerName.value = identifyLayerResultList[0].layerContent.name
                }
            }
        }
    }

    /**
     * Recenter the viewpoint to the given [mapPoint]
     */
    fun recenterMap(mapPoint: Point?) {
        viewModelScope.launch {
            mapViewProxy.setViewpointAnimated(
                viewpoint = Viewpoint(mapPoint!!)
            )
        }
    }
}
