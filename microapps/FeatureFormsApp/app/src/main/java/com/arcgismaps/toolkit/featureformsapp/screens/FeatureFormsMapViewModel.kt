package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeatureFormsMapViewModel(
    arcGISMap: ArcGISMap,
    mapInsets: MapInsets = MapInsets()
) : ViewModel(), MapInterface {
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(arcGISMap)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()
    
    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(mapInsets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()
    
    private val _currentViewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    override val currentViewpoint: StateFlow<Viewpoint?> = _currentViewpoint.asStateFlow()
    private fun editFeature(feature: ArcGISFeature) {}
    
    private fun onIdentifyLayers(results: List<IdentifyLayerResult>) {
        val popup = results.firstOrNull { result ->
            result.popups.isNotEmpty()
        }?.popups?.firstOrNull() ?: return
    
        val feature = popup.geoElement as? ArcGISFeature ?: return
        viewModelScope.launch {
            feature.load().onSuccess { editFeature(feature) }
        }
    }

    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            onSingleTapConfirmed.collect {
                identifyLayers(
                    it.screenCoordinate,
                    22.0,
                    true,
                    -1
                ).onSuccess { results ->
                    onIdentifyLayers(results)
                }
            }
        }
    }
}

class MapViewModelFactory(
    private val arcGISMap: ArcGISMap,
    private val mapInsets: MapInsets = MapInsets()
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormsMapViewModel(arcGISMap, mapInsets) as T
    }
}
