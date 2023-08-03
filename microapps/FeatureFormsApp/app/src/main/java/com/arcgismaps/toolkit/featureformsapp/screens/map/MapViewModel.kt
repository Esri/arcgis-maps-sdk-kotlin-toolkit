package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureforms.EditingTransactionState
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemData
import com.arcgismaps.toolkit.featureformsapp.domain.PortalItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A view model for the FeatureForms MapView UI
 * @constructor to be invoked by injection
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val portalItemUseCase: PortalItemUseCase
) : ViewModel(),
    MapState by MapState(),
    FeatureFormState by FeatureFormState() {
    private val url: String = savedStateHandle["uri"]!!
    lateinit var portalItemData: PortalItemData
    
    init {
        viewModelScope.launch {
            portalItemData = portalItemUseCase(url)
            setMap(ArcGISMap(portalItemData.portalItem))
        }
    }
    
    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            val layer = map.value?.operationalLayers?.filterIsInstance<FeatureLayer>()?.firstOrNull { layer ->
                portalItemData.formLayerName?.let {
                    layer.name == it
                } ?: true
            } ?: return@launch

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
                            val featureForm = FeatureForm(feature, layer.featureFormDefinition!!)
                            // update the FeatureFormState's FeatureForm
                            setFeatureForm(featureForm)
                            // set the FeatureFormState to an editing state to bring up the
                            // FeatureForm UI
                            setTransactionState(EditingTransactionState.Editing)
                        } catch (e: Exception) {
                            e.printStackTrace() // for debugging core issues
                            Toast.makeText(
                                context,
                                "failed to create a FeatureForm for the feature and layer",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }.onFailure { println("failed to load tapped Feature") }
                } ?: println("tap was not on a feature")
            }
        }
    }
}
