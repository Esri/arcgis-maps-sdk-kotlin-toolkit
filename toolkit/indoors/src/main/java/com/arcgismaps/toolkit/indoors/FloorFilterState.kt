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

package com.arcgismaps.toolkit.indoors

import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.floor.FloorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * An interface to pass into the FloorFilter composable function.
 */
public sealed interface FloorFilterState {
   public val floorManager: StateFlow<FloorManager?>
   public suspend fun loadFloorManager()
}

private class FloorFilterStateImpl(var geoModel: GeoModel, coroutineScope: CoroutineScope) : FloorFilterState {

    private val _floorManager: MutableStateFlow<FloorManager?> = MutableStateFlow(null)
    override val floorManager: StateFlow<FloorManager?> = _floorManager.asStateFlow()

    init {
        coroutineScope.launch {
            loadFloorManager()
        }

    }

    override suspend fun loadFloorManager() {
        geoModel.load().onSuccess {
            val floorManager: FloorManager = geoModel.floorManager
                ?: throw IllegalStateException("The map is not configured to be floor aware")
            floorManager.load().onSuccess {
                _floorManager.value = floorManager
            }.onFailure {
                throw it
            }
        }.onFailure {
            throw it
        }
    }
}

public fun FloorFilterState(geoModel: GeoModel, coroutineScope: CoroutineScope): FloorFilterState =
    FloorFilterStateImpl(geoModel, coroutineScope)
