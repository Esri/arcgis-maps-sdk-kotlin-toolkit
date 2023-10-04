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

package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.ArcGISMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the state for the Map.
 *
 * @since 200.3.0
 */
public sealed interface MapState : GeoComposeState {
    public val arcGISMap: StateFlow<ArcGISMap?>

    public fun setArcGISMap(arcGISMap: ArcGISMap)
}

public fun MapState(arcGISMap: ArcGISMap? = null): MapState = MapStateImpl(arcGISMap)

private class MapStateImpl(
    arcGISMap: ArcGISMap?
) : MapState {

    private val _arcGISMap: MutableStateFlow<ArcGISMap?> = MutableStateFlow(null)
    override val arcGISMap: StateFlow<ArcGISMap?> = _arcGISMap.asStateFlow()

    override fun setArcGISMap(arcGISMap: ArcGISMap){
        _arcGISMap.value = arcGISMap
    }

    init {
        _arcGISMap.value = arcGISMap
    }
}
