package com.arcgismaps.toolkit.featureformsapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.toolkit.composablemap.MapData
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeatureFormsMapViewModel(
    mapData: MapData
) : ViewModel(), MapInterface {
    private val _mapData: MutableStateFlow<MapData> = MutableStateFlow(mapData)
    override val mapData = _mapData.asStateFlow()
    
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
    
    context(MapView, CoroutineScope) override fun viewLogic() {
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

class MapViewModelFactory(private val mapData: MapData) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormsMapViewModel(mapData) as T
    }
}
