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

    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _viewpoint: MutableStateFlow<FlowData<Viewpoint?>> = MutableStateFlow(FlowData(null))
    override val viewpoint = _viewpoint.asStateFlow()

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
        return TemplateMapViewModel(arcGISMap, mapInsets) as T
    }
}
