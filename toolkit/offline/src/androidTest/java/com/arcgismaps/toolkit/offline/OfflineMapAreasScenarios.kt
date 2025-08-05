/*
 *
 *  Copyright 2025 Esri
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
 *
 */

package com.arcgismaps.toolkit.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapView

/**
 * ViewModel for the [OfflineMapAreasTests] that contains the [OfflineMapAreasScenarios]
 * helper functions for testing functionality.
 *
 * @since 200.8.0
 */
class MapViewModel internal constructor(internal val mode: OfflineMapMode) : ViewModel() {

    val onlineMap
        get() = if (mode == OfflineMapMode.Preplanned)
            ArcGISMap(
                item = PortalItem(
                    portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
                    itemId = "acc027394bc84c2fb04d1ed317aac674"
                )
            ) else
            ArcGISMap(
                item = PortalItem(
                    portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
                    itemId = "3da658f2492f4cfd8494970ef489d2c5"
                )
            )

    val selectedMap = mutableStateOf<ArcGISMap?>(null)

    val displayedMap get() = selectedMap.value ?: onlineMap

    val offlineMapState = OfflineMapState(
        arcGISMap = displayedMap,
        onSelectionChanged = { offlineMap ->
            selectedMap.value = offlineMap
        }
    )
}


/**
 * Composable Scenarios for the [OfflineMapAreasTests]
 *
 * @since 200.8.0
 */
@Composable
fun OfflineScenario(mapViewModel: MapViewModel) {
    Column(Modifier.fillMaxSize()) {
        MapView(
            arcGISMap = mapViewModel.displayedMap,
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5F)
        )
        OfflineMapAreas(
            offlineMapState = mapViewModel.offlineMapState,
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5F)
        )
    }
}
