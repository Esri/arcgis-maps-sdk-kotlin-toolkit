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

class IdentifyViewModel : ViewModel() {
    val mapViewProxy = MapViewProxy()
    var showProgressIndicator by mutableStateOf(false)
    var identifiedAttributes: Map<String, Any?> by mutableStateOf(emptyMap())
    val featureLayer = FeatureLayer.createWithItem(PortalItem("https://www.arcgis.com/home/item.html?id=9e2f2b544c954fda9cd13b7f3e6eebce"))
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISDarkGray).apply {
        viewModelScope.launch(Dispatchers.Main) {
            showProgressIndicator = true
            featureLayer.load()
            operationalLayers.add(featureLayer)
            showProgressIndicator = false
        }
    }

    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) = viewModelScope.launch {
        showProgressIndicator = true
        identifiedAttributes = emptyMap()
        featureLayer.clearSelection()
        val result = mapViewProxy.identify(featureLayer, singleTapConfirmedEvent.screenCoordinate, 20.0)
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