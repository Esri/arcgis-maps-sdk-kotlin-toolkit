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

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.labeling.SimpleLabelExpression
import com.arcgismaps.mapping.layers.DynamicEntityLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.realtime.CustomDynamicEntityDataSource
import com.arcgismaps.realtime.DynamicEntityObservation
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.mapviewcalloutapp.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(application: Application) : AndroidViewModel(application) {

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
    val mapPoint: StateFlow<Point?> = _mapPoint.asStateFlow()

    private val _selectedGeoElement = MutableStateFlow<GeoElement?>(null)
    val selectedGeoElement: StateFlow<GeoElement?> = _selectedGeoElement.asStateFlow()

    private val _selectedDynamicEntity = MutableStateFlow<DynamicEntity?>(null)
    val selectedDynamicEntity : StateFlow<DynamicEntity?> = _selectedDynamicEntity.asStateFlow()

    private val _selectedLayerName = MutableStateFlow("")
    val selectedLayerName: StateFlow<String> = _selectedLayerName.asStateFlow()

    private val _tapLocation = MutableStateFlow<Point?>(null)
    val tapLocation: StateFlow<Point?> = _tapLocation.asStateFlow()

    private val _offset = MutableStateFlow(Offset.Zero)
    val offset: StateFlow<Offset> = _offset

    val tapLocationGraphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private var currentIdentifyJob: Job? = null

    // Create a new custom feed provider that processes observations from a JSON file.
    // This takes the path to the simulation file, field name that will be used as the entity id,
    // and the delay between each observation that is processed.
    // In this example we are using a json file as our custom data source.
    // This field value should be a unique identifier for each entity.
    // Adjusting the value for the delay will change the speed at which the entities and their
    // observations are displayed.
    private val feedProvider = CustomEntityFeedProvider(
        fileInputStream = application.resources.openRawResource(R.raw.ais_marinecadastre_selectedvessels_customdatasource),
        entityIdField = "MMSI",
        delayDuration = 25.milliseconds
    )

    private val dynamicEntityDataSource = CustomDynamicEntityDataSource(feedProvider)

    // Create the dynamic entity layer using the custom data source.
    private val dynamicEntityLayer = DynamicEntityLayer(dynamicEntityDataSource).apply {
        trackDisplayProperties.apply {
            // Set up the track display properties, these properties will be used to configure the appearance of the track line and previous observations.
            showPreviousObservations = true
            showTrackLine = true
            maximumObservations = 20
        }

        // Define the label expression to be used, in this case we will use the "VesselName" for each of the dynamic entities.
        val simpleLabelExpression = SimpleLabelExpression("[VesselName]")

        // Set the text symbol color and size for the labels.
        val labelSymbol = TextSymbol().apply {
            color = Color.red
            size = 12.0F
        }

        // Add the label definition to the dynamic entity layer and enable labels.
        labelDefinitions.add(LabelDefinition(simpleLabelExpression, labelSymbol).apply {
            // Set the label position.
            placement = LabelingPlacement.PointAboveCenter
        })
        labelsEnabled = true
    }

    val mapWithDynamicEntities =  ArcGISMap(BasemapStyle.ArcGISOceans).apply {
        initialViewpoint = Viewpoint(47.984, -123.657, 3e6)
        // Add the dynamic entity layer to the map.
        operationalLayers.add(dynamicEntityLayer)
    }

    fun loadDynamicEntities(){
        viewModelScope.launch {
            dynamicEntityLayer.load().getOrThrow()
            mapWithDynamicEntities.load().getOrThrow()
            dynamicEntityDataSource.connect().getOrThrow()
        }
    }

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
            val result = mapViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 1.dp
            )
            result.onSuccess { identifyLayerResultList ->
                if (identifyLayerResultList.isNotEmpty()) {
                    _selectedGeoElement.value = identifyLayerResultList[0].geoElements.firstOrNull()
                    _selectedLayerName.value = identifyLayerResultList[0].layerContent.name
                }
            }
        }
    }


    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]. The
     * identified [DynamicEntity] is set to [_selectedGeoElement].
     *
     * @since 200.5.0
     */
    fun identifyOnDynamicEntity(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        currentIdentifyJob?.cancel()
        currentIdentifyJob = viewModelScope.launch {
            val result = mapViewProxy.identify(
                layer = dynamicEntityLayer,
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 20.dp
            )
            result.onSuccess { identifyLayerResult ->
                val observation = identifyLayerResult.geoElements.firstOrNull() as? DynamicEntityObservation
                // set to null if no observation was identified
                if (observation == null){
                    _selectedDynamicEntity.value = null
                    return@onSuccess
                }
                // update the selected geo-element to the identified dynamic entity
                _selectedDynamicEntity.value = observation.dynamicEntity
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
