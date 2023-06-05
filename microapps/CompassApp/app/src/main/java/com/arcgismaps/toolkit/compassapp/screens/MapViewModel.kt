package com.arcgismaps.toolkit.compassapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.FlowData
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {
    // unique id for this class when emitting flows
    private val flowProducer : UUID = UUID.randomUUID()

    // StateFlow for the map property
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map = _map.asStateFlow()

    // StateFlow for the map insets
    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets = _insets.asStateFlow()

    // StateFlow for the map viewpoint
    private val _viewpoint: MutableStateFlow<FlowData<Viewpoint?>> = MutableStateFlow(FlowData(null, flowProducer))
    override val viewpoint = _viewpoint.asStateFlow()

    // StateFlow for the map rotation
    private val _mapRotation: MutableStateFlow<FlowData<Double>> = MutableStateFlow(FlowData(0.0, flowProducer))
    override val mapRotation = _mapRotation.asStateFlow()

    override suspend fun onMapViewpointChanged(viewpoint: Viewpoint, flowProducer: UUID?) {
        _viewpoint.emit(FlowData(viewpoint, flowProducer))
    }

    override suspend fun onMapRotationChanged(rotation: Double, flowProducer: UUID?) {
        _mapRotation.emit(FlowData(rotation, flowProducer))
    }

    override fun setViewpoint(viewpoint: Viewpoint) {
        viewModelScope.launch {
            _viewpoint.emit(FlowData(viewpoint, flowProducer))
        }
    }

    override fun setViewpointRotation(angleDegrees: Double) {
        viewModelScope.launch {
            _mapRotation.emit(FlowData(angleDegrees, flowProducer))
        }
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
