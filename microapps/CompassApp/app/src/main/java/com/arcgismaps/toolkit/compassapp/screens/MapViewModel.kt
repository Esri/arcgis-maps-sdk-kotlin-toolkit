package com.arcgismaps.toolkit.compassapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.FlowData
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {

    // StateFlow for the map property
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map = _map.asStateFlow()

    // StateFlow for the map insets
    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets = _insets.asStateFlow()

    // StateFlow for the map viewpoint
    private val _viewpoint: MutableStateFlow<FlowData<Viewpoint?>> = MutableStateFlow(FlowData(null))
    override val viewpoint = _viewpoint.asStateFlow()

    // StateFlow for the map rotation
    private val _mapRotation: MutableStateFlow<FlowData<Double>> = MutableStateFlow(FlowData(0.0))
    override val mapRotation = _mapRotation.asStateFlow()

    override fun onMapViewpointChanged(viewpoint: Viewpoint, flowProducer: UUID?) {
        _viewpoint.value = FlowData(viewpoint, flowProducer)
    }

    override fun onMapRotationChanged(rotation: Double, flowProducer: UUID?) {
        _mapRotation.value = FlowData(rotation, flowProducer)
    }

    override fun setViewpoint(viewpoint: Viewpoint) {
        _viewpoint.value = FlowData(viewpoint)
    }

    override fun setViewpointRotation(angleDegrees: Double) {
        _mapRotation.value = FlowData(angleDegrees)
    }
}

class MapViewModelFactory(
    private val arcGISMap: ArcGISMap,
    private val mapInsets: MapInsets = MapInsets()
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(arcGISMap, mapInsets) as T
    }
}
