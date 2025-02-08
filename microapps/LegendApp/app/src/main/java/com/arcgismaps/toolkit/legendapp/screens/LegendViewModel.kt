package com.arcgismaps.toolkit.legendapp.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.legend.LegendState
import kotlinx.coroutines.launch

class LegendViewModel : ViewModel() {

    private val completePopups2 = "f1ed0d220d6447a586203675ed5ac213" // dot net
    private val portlandTreeSurvey  = "16f1b8ba37b44dc3884afc8d5f454dd2" // dot net
    private val sanDiegoShortlist = "1966ef409a344d089b001df85332608f" // mark iOS
    private val census = "5588bd6cf0484b1a8fb92b0d8478a386" // San Diego census

    val arcGISMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            census
        )
    )

    private val mapViewProxy = MapViewProxy()

    val legendState = LegendState(arcGISMap, mapViewProxy)

    init {
        viewModelScope.launch {
            arcGISMap.load()
        }
    }

}