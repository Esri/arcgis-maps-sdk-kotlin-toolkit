/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.floorfilterapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.composablemap.MapInterfaceImpl
import com.arcgismaps.toolkit.indoors.FloorFilterSelection
import com.arcgismaps.toolkit.indoors.FloorFilterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MapViewModel(
    arcGISMap: ArcGISMap
) : ViewModel(), MapInterface by MapInterfaceImpl(arcGISMap) {

    val floorFilterState: FloorFilterState = FloorFilterState(this.map.value, viewModelScope) { floorFilterSelection ->
        when (floorFilterSelection.type) {
            is FloorFilterSelection.Type.FloorSite -> {
                val floorFilterSelectionType =
                    floorFilterSelection.type as FloorFilterSelection.Type.FloorSite
                floorFilterSelectionType.site.geometry?.let {
                    this.setViewpoint(Viewpoint(it))
                }
            }
            is FloorFilterSelection.Type.FloorFacility -> {
                val floorFilterSelectionType =
                    floorFilterSelection.type as FloorFilterSelection.Type.FloorFacility
                floorFilterSelectionType.facility.geometry?.let {
                    this.setViewpoint(Viewpoint(it))
                }
            }
            else -> {}
        }
    }

}

class MapViewModelFactory(
    private val arcGISMap: ArcGISMap
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(arcGISMap) as T
    }
}
