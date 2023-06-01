package com.arcgismaps.toolkit.templateapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _currentViewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    override val currentViewpoint: StateFlow<Viewpoint?> = _currentViewpoint.asStateFlow()
    override val resetMapRotation: StateFlow<Boolean>
        get() = TODO("Not yet implemented")

    private val _mapRotation: MutableStateFlow<Double> = MutableStateFlow(0.0)
    val mapRotation: StateFlow<Double> = _mapRotation.asStateFlow()

    override fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) { }

    override fun onMapRotationChanged(rotation: Double) {
        _mapRotation.value = rotation
    }

    override fun onViewpointChanged(viewpoint: Viewpoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapRotationReset() {
        TODO("Not yet implemented")
    }

    fun setViewpoint(viewpoint: Viewpoint) {
        _currentViewpoint.update { viewpoint }
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
