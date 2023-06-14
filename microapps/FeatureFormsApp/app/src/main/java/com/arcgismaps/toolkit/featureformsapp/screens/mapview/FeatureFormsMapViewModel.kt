package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.composablemap.MapInterfaceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A view model for the FeatureForms MapView UI
 * @constructor to be invoked by the ViewModel factory
 *
 * @since 200.2.0
 */
class FeatureFormsMapViewModelImpl(
    arcGISMap: ArcGISMap,
    val onFeatureIdentified: (ArcGISFeature) -> Unit
) : ViewModel(), MapInterface by MapInterfaceImpl(arcGISMap) {
    private suspend fun onIdentifyLayers(results: List<IdentifyLayerResult>) {
        val popup = results.firstOrNull { result ->
            result.popups.isNotEmpty()
        }?.popups?.firstOrNull() ?: return
        
        val feature = popup.geoElement as? ArcGISFeature ?: return
        feature.load().onSuccess { onFeatureIdentified(feature) }
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
    private val onFeatureIdentified: (ArcGISFeature) -> Unit = {}
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormsMapViewModelImpl(arcGISMap, onFeatureIdentified) as T
    }
}
