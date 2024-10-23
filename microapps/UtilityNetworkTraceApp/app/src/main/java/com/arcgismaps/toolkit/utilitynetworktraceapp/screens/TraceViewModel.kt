package com.arcgismaps.toolkit.utilitynetworktraceapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.GroupLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.utilitynetworks.TraceState
import kotlinx.coroutines.launch

class TraceViewModel : ViewModel() {

    private val napervilleUtilities = "471eb0bf37074b1fbb972b1da70fb310"

    val arcGISMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            napervilleUtilities
        )
    )

    val mapViewProxy = MapViewProxy()

    val graphicsOverlay = GraphicsOverlay()

    val traceState = TraceState(arcGISMap, graphicsOverlay, mapViewProxy)

    init {
        viewModelScope.launch {
            arcGISMap.load()
        }
    }

    suspend fun performQueryAndGetFeatures(): List<ArcGISFeature> {
        val featureService = (arcGISMap.operationalLayers[0] as GroupLayer).layers[3] as FeatureLayer
        val serviceFeatureTable = featureService.featureTable as ServiceFeatureTable
        serviceFeatureTable.load().getOrThrow()
        val queryParameters = QueryParameters().apply {
            geometry = serviceFeatureTable.extent
            whereClause = "1=1"
        }
        val features = serviceFeatureTable.queryFeatures(queryParameters).getOrThrow()
        val arcGISFeatures = features.map { it as ArcGISFeature }
        val arcGISFeaturesList = listOf(arcGISFeatures[0], arcGISFeatures[1])
        arcGISFeaturesList.forEach { it.load().getOrThrow() }
        return arcGISFeaturesList
    }

}
