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

package com.arcgismaps.toolkit.offlinemapareasapp.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.offline.OfflineMapState
import kotlinx.coroutines.launch

class OfflineViewModel : ViewModel() {

    private val napervilleWaterNetwork = "3da658f2492f4cfd8494970ef489d2c5"
    private val onlineMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            napervilleWaterNetwork
        )
    )
    val selectedMap = mutableStateOf<ArcGISMap?>(null)

    val arcGISMap
        get() = selectedMap.value ?: onlineMap

    val offlineMapState = OfflineMapState(arcGISMap) {
        selectedMap.value = it
    }

    init {
        viewModelScope.launch { arcGISMap.load() }
    }
}
