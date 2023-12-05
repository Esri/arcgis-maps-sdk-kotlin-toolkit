package com.arcgismaps.toolkit.mapviewidentifyapp.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Holds state and business logic for the identify app's [MainScreen].
 *
 * @since 200.4.0
 */
class IdentifyViewModel : ViewModel() {

    /**
     * The [MapViewProxy] that this viewmodel will operate on.
     *
     * @since 200.4.0
     */
    val mapViewProxy = MapViewProxy()

    /**
     * Whether a loading indicator should be displayed.
     *
     * @since 200.4.0
     */
    var showProgressIndicator by mutableStateOf(false)

    /**
     * The attribute map of the last identified geoelement.
     *
     * @since 200.4.0
     */
    var identifiedAttributes: Map<String, Any?> by mutableStateOf(emptyMap())

    /**
     * The feature layer that contains recent earthquake data, hosted on the ArcGIS Living Atlas of the World.
     *
     * @since 200.4.0
     */
    private val featureLayer =
        FeatureLayer.createWithItem(PortalItem("https://www.arcgis.com/home/item.html?id=9e2f2b544c954fda9cd13b7f3e6eebce"))

    /**
     * A map containing the [featureLayer] with recent earthquake data.
     *
     * @since 200.4.0
     */
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISDarkGray).apply {
        viewModelScope.launch(Dispatchers.Main) {
            // Show a loading indicator while the feature layer loads
            showProgressIndicator = true
            featureLayer.load()
            operationalLayers.add(featureLayer)
            showProgressIndicator = false
        }
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]. The attributes
     * of the identified geoelement are set to [identifiedAttributes] and the geoelement is selected.
     *
     * @since 200.4.0
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) = viewModelScope.launch {
        showProgressIndicator = true
        identifiedAttributes = emptyMap()
        featureLayer.clearSelection()
        val result =
            mapViewProxy.identify(featureLayer, singleTapConfirmedEvent.screenCoordinate, 20.0)
        result.onSuccess {
            val geoElement = it.geoElements.firstOrNull()
            geoElement?.attributes?.let {
                identifiedAttributes = it
            }
            (geoElement as? Feature)?.let {
                featureLayer.selectFeature(it)
            }
        }
        showProgressIndicator = false
    }
}