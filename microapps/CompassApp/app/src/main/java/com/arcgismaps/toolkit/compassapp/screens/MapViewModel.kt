package com.arcgismaps.toolkit.compassapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _viewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    override val viewpoint: StateFlow<Viewpoint?> = _viewpoint.asStateFlow()

    private val _mapRotation: MutableSharedFlow<Double> = MutableSharedFlow()
    override val mapRotation: SharedFlow<Double> = _mapRotation.asSharedFlow()

    override suspend fun onMapViewpointChanged(viewpoint: Viewpoint) {
        _viewpoint.emit(viewpoint)
    }

    override suspend fun onMapRotationChanged(rotation: Double) {
        _mapRotation.emit(rotation)
    }

    override fun setViewpoint(viewpoint: Viewpoint) {
        viewModelScope.launch {
            _viewpoint.emit(viewpoint)
        }
    }

    override fun setViewpointRotation(angleDegrees: Double) {
        viewModelScope.launch {
            _mapRotation.emit(angleDegrees)
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
