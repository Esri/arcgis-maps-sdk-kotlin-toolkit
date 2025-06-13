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

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.offline.OfflineMapState
import com.arcgismaps.toolkit.offline.OfflineRepository
import kotlinx.coroutines.launch

class OfflineViewModel(app: Application) : AndroidViewModel(app) {

    private val napervilleWaterNetwork = "acc027394bc84c2fb04d1ed317aac674"
    private val onlineMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            napervilleWaterNetwork
        )
    )
    val selectedMap = mutableStateOf<ArcGISMap?>(null)

    val arcGISMap
        get() = selectedMap.value ?: onlineMap

    val offlineMapState = OfflineMapState(arcGISMap, OfflineRepository(app.applicationContext)) {
        selectedMap.value = it
    }

    init {
        viewModelScope.launch { arcGISMap.load() }
    }
}
