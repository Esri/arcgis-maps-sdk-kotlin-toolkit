package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.formInfoJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A view model for the FeatureForms MapView UI
 * @constructor to be invoked by the [MapViewModelFactory]
 */
class MapViewModel(
    arcGISMap: ArcGISMap
) : ViewModel(),
    MapInterface by MapInterface(arcGISMap),
    FeatureFormState by FeatureFormState() {

    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            val layer = if (this@MapView.map?.item?.itemId == "0f6864ddc35241649e5ad2ee61a3abe4") {
                
                map.value.operationalLayers.filterIsInstance<FeatureLayer>().first {
                    it.name == "CityworksDynamic - Water Hydrants"
                }
            } else{
                map.value.operationalLayers.filterIsInstance<FeatureLayer>().first()
            }
            
            this@MapView.identifyLayer(
                layer = layer,
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.0,
                returnPopupsOnly = false
            ).onSuccess { results ->
                results.geoElements.firstOrNull { it is ArcGISFeature }?.let {
                    val feature = it as ArcGISFeature
                    feature.load().onSuccess {
                        try {
                            FeatureFormDefinition.fromJsonOrNull(layer.formInfoJson!!)
                                ?.let { featureFormDefinition ->
                                    // update the feature on the featureFormDefinition
                                    featureFormDefinition.feature = feature
                                    // update the FeatureFormState's FormDefinition
                                    setFormDefinition(featureFormDefinition)
                                    // set the FeatureFormState to an editing state to bring up the
                                    // FeatureForm UI
                                    setTransactionState(true)
                                }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "could not get the form definition from unsupported JSON.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }.onFailure { println("failed to load tapped Feature") }
                } ?: println("tap was not on a feature")
            }
        }
    }
}

/**
 * Factory for the [MapViewModel]
 */
class MapViewModelFactory(
    private val arcGISMap: ArcGISMap
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(arcGISMap) as T
    }
}
