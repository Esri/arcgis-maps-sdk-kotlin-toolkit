package com.arcgismaps.toolkit.legendapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.launch

class LegendViewModel : ViewModel() {

    private val sanDiegoShortlist = "1966ef409a344d089b001df85332608f"

    val arcGISMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            sanDiegoShortlist
        )
    )

    init {
        viewModelScope.launch {
            arcGISMap.load()
        }
    }

}
