/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.offlinemapareasapp.screens.map

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.offline.OfflineMapState
import com.arcgismaps.toolkit.offlinemapareasapp.data.PortalItemRepository
import com.arcgismaps.toolkit.offlinemapareasapp.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base class for context aware AndroidViewModel. This class must have only a single application
 * parameter.
 */
open class BaseMapViewModel(application: Application) : AndroidViewModel(application)

/**
 * A view model for the OfflineMapAreas MapView UI
 * @constructor to be invoked by injection
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    portalItemRepository: PortalItemRepository,
    application: Application,
    @ApplicationScope private val scope: CoroutineScope
) : BaseMapViewModel(application) {
    private val itemId: String = savedStateHandle["uri"]!!

    val proxy: MapViewProxy = MapViewProxy()

    var portalItem: PortalItem = portalItemRepository(itemId)
        ?: throw IllegalStateException("portal item not found with id $itemId")

    private val onlineMap = ArcGISMap(portalItem)
    val selectedMap = mutableStateOf<ArcGISMap?>(null)

    val arcGISMap
        get() = selectedMap.value ?: onlineMap

    val offlineMapState = OfflineMapState(arcGISMap) {
        selectedMap.value = it
    }


    init {
        scope.launch {
            // load the map and set the UI state to not editing
            arcGISMap.load()
        }
    }

}