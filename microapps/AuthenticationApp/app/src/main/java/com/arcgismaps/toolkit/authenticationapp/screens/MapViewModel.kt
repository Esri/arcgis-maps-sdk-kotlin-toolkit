package com.arcgismaps.toolkit.authenticationapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.FlowData
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _viewpoint: MutableStateFlow<FlowData<Viewpoint?>> = MutableStateFlow(FlowData<Viewpoint?>(null))
    override val viewpoint: StateFlow<FlowData<Viewpoint?>> = _viewpoint.asStateFlow()

    private val _mapRotation: MutableStateFlow<FlowData<Double>> = MutableStateFlow(FlowData(0.0))
    override val mapRotation: StateFlow<FlowData<Double>> = _mapRotation.asStateFlow()

    override fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) { }

    override fun setViewpoint(viewpoint: Viewpoint) {
        _viewpoint.value = FlowData(viewpoint)
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
