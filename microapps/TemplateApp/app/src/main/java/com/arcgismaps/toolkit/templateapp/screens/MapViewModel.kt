package com.arcgismaps.toolkit.templateapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapData
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel (
    mapData: MapData
) : ViewModel(), MapInterface {
    private val _mapData: MutableStateFlow<MapData> = MutableStateFlow(mapData)
    override val mapData = _mapData.asStateFlow()

    override suspend fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {}
}

class MapViewModelFactory(private val mapData: MapData) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(mapData) as T
    }
}
