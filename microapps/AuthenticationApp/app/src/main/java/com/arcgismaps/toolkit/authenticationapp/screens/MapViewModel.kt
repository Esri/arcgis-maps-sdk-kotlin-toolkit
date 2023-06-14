package com.arcgismaps.toolkit.authenticationapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.composablemap.MapInterfaceImpl

class MapViewModel(
    arcGISMap: ArcGISMap
) : ViewModel(), MapInterface by MapInterfaceImpl(arcGISMap)

class MapViewModelFactory(
    private val arcGISMap: ArcGISMap
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(arcGISMap) as T
    }
}
