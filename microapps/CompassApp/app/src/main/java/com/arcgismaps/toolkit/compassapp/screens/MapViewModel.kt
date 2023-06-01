package com.arcgismaps.toolkit.compassapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val _resetMapRotation: MutableSharedFlow<Unit> = MutableSharedFlow()
    override val resetMapRotation: SharedFlow<Unit> = _resetMapRotation.asSharedFlow()

    private val _currentMapRotation: MutableStateFlow<Double> = MutableStateFlow(0.0)
    val currentMapRotation: StateFlow<Double> = _currentMapRotation.asStateFlow()

    override fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) { }

    override fun onMapRotationChanged(rotation: Double) {
        _currentMapRotation.value = rotation
    }

    fun setViewpoint(viewpoint: Viewpoint) {
        _currentViewpoint.value = viewpoint
    }

    fun resetMapRotation() {
        viewModelScope.launch {
            _resetMapRotation.emit(Unit)
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
