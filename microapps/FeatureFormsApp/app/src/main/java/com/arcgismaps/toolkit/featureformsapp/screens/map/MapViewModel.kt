package com.arcgismaps.toolkit.featureformsapp.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.composablemap.MapInterfaceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A view model for the FeatureForms MapView UI
 * @constructor to be invoked by the [MapViewModelFactory]
 */
class MapViewModel(
    arcGISMap: ArcGISMap,
    val onFeatureIdentified: (FeatureLayer, ArcGISFeature) -> Unit
) : ViewModel(), MapInterface by MapInterfaceImpl(arcGISMap) {
    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            val layer = this@MapView.map?.operationalLayers?.filterIsInstance<FeatureLayer>()?.first()
                ?: throw IllegalStateException("map should have layers")
            this@MapView.identifyLayer(
                layer = layer,
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.0,
                returnPopupsOnly = false
            )
                .onSuccess { results ->
                    results.geoElements.firstOrNull { it is ArcGISFeature }?.let {
                        val feature = it as ArcGISFeature
                        feature
                            .load()
                            .onSuccess { onFeatureIdentified(layer, feature) }
                            .onFailure { println("failed to load tapped Feature") }
                    } ?: println("tap was not on a feature")
                }
        }
    }
}

/**
 * Factory for the [MapViewModel]
 */
class MapViewModelFactory(
    private val arcGISMap: ArcGISMap,
    private val onFeatureIdentified: (FeatureLayer, ArcGISFeature) -> Unit = { _, _ -> }
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(arcGISMap, onFeatureIdentified) as T
    }
}
