package com.arcgismaps.toolkit.offlinemapareasapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.offline.OfflineMapState
import kotlinx.coroutines.launch

class OfflineViewModel : ViewModel() {

    private val napervilleWaterNetwork = "acc027394bc84c2fb04d1ed317aac674"

    val arcGISMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            napervilleWaterNetwork
        )
    )

    val offlineMapState = OfflineMapState(arcGISMap)

    init {
        viewModelScope.launch {
            arcGISMap.load()
        }
    }
}
