package com.arcgismaps.toolkit.templateapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.FlowData
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TemplateMapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {
    // unique id for this class when emitting flows
    private val flowProducer : UUID = UUID.randomUUID()

    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _viewpoint: MutableStateFlow<FlowData<Viewpoint?>> = MutableStateFlow(FlowData(null, flowProducer))
    override val viewpoint = _viewpoint.asStateFlow()

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
        return TemplateMapViewModel(arcGISMap, mapInsets) as T
    }
}
