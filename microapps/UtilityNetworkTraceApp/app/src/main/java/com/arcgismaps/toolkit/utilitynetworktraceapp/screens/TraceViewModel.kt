package com.arcgismaps.toolkit.utilitynetworktraceapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.utilitynetworks.TraceState

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

    val traceState = TraceState(arcGISMap, viewModelScope, mapViewProxy, graphicsOverlay)

}