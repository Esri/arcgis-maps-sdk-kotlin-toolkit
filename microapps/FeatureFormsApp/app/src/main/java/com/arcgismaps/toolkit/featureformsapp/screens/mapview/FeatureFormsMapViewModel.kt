package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

/**
 * A view model for the FeatureForms MapView UI
 */
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
    
    private fun editFeature() {
        println("editFeature")
        // to be fleshed out with its own view model and navigation to the bottom sheet or side panel.
    }
    
    private suspend fun onIdentifyLayers(results: List<IdentifyLayerResult>) {
        val popup = results.firstOrNull { result ->
            result.popups.isNotEmpty()
        }?.popups?.firstOrNull() ?: return
        
        val feature = popup.geoElement as? ArcGISFeature ?: return
        feature.load().onSuccess { editFeature() }
    }

    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            this@MapView.identifyLayers(
                singleTapEvent.screenCoordinate,
                22.0,
                true,
                -1
            ).onSuccess { results ->
                onIdentifyLayers(results)
            }
        }
    }
}

class FeatureFormsMapViewModelFactory(
    private val arcGISMap: ArcGISMap,
    private val mapInsets: MapInsets = MapInsets()
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormsMapViewModel(arcGISMap, mapInsets) as T
    }
}
