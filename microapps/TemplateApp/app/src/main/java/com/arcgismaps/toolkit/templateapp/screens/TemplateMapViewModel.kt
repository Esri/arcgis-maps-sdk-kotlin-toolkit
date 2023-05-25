package com.arcgismaps.toolkit.templateapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.toolkit.composablemap.MapData
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TemplateMapViewModel (
    mapData: MapData
) : ViewModel(), MapInterface {
    private val _mapData: MutableStateFlow<MapData> = MutableStateFlow(mapData)
    override val mapData = _mapData.asStateFlow()
    context(MapView, CoroutineScope) override suspend fun viewLogic() {}
}

class MapViewModelFactory(private val mapData: MapData) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TemplateMapViewModel(mapData) as T
    }
}
