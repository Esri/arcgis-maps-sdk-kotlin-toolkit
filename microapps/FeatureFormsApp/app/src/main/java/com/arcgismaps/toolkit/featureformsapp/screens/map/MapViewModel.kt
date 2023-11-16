package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureforms.EditingTransactionState
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.di.PortalItemRepo
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
    private val portalItemRepository: PortalItemRepository
) : ViewModel(),
    MapState by MapState(),
    FeatureFormState by FeatureFormState() {
    private val itemId: String = savedStateHandle["uri"]!!
    lateinit var portalItem: PortalItem

    init {
        viewModelScope.launch {
            portalItem = portalItemRepository(itemId) ?: return@launch
            setMap(ArcGISMap(portalItem))
        }
    }

    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            this@MapView.identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.0,
                returnPopupsOnly = false
            ).onSuccess { results ->
                results.firstNotNullOfOrNull { result ->
                    try {
                        result.geoElements.filterIsInstance<ArcGISFeature>()
                            .firstOrNull { feature ->
                                (feature.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            "failed to load the FeatureFormDefinition for the feature",
                            Toast.LENGTH_LONG
                        ).show()
                        null
                    }
                }?.let { feature ->
                        feature.load().onSuccess {
                            try {
                                val featureForm = FeatureForm(
                                    feature,
                                    (feature.featureTable?.layer as FeatureLayer).featureFormDefinition!!
                                )
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
                    } ?: println("identified features do not have feature forms defined")
            }.onFailure { println("tap was not on a feature") }
        }
    }
}
