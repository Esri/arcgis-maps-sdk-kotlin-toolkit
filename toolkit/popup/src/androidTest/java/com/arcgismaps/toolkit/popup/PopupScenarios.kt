/*
 *  Copyright 2026 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.popup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapView

/**
 * ViewModel for the [PopupTests] that contains the [PopupScenario]
 * helper functions for testing functionality.
 *
 * @since 300.2.0
 */
class MapViewModel internal constructor(internal val itemId: String) : ViewModel() {

    val map = ArcGISMap(
        item = PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            itemId = itemId
        )
    )
    private val _popupStates = mutableListOf<PopupState>()
    suspend fun load(): List<PopupState> {
        map.load().getOrThrow()
        val featureLayer = map.operationalLayers[0] as FeatureLayer
        val definition = featureLayer.popupDefinition
            ?: error("PopupDefinition is null")
        val queryParameters = QueryParameters().apply { whereClause = "1=1" }
        val queryResult = featureLayer.featureTable
            ?.queryFeatures(queryParameters)
            ?.getOrThrow()
            ?: error("QueryResult is null")

        _popupStates.clear()
        queryResult.forEach { feature ->
            val popup = Popup(geoElement = feature, popupDefinition = definition)
            _popupStates.add(PopupState(popup = popup, scope = viewModelScope))

        }
        return _popupStates.toList()
    }
}

/**
 * Composable Scenarios for the [PopupTests]
 *
 * @since 300.2.0
 */
@Composable
fun PopupScenario(map: ArcGISMap, popupState: PopupState) {
    var popupVisible by remember(popupState) { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        MapView(
            arcGISMap = map,
            isAttributionBarVisible = false,
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5F)
        )
        if (popupVisible) {
            Popup(
                popupState = popupState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.5F),
                onDismiss = { popupVisible = false }
            )
        }
    }
}
