/*
 *
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

package com.arcgismaps.toolkit.mapcomposeapp.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.MapProperties
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val mapProperties = MapProperties().apply {
        arcGISMap.value = ArcGISMap(BasemapStyle.ArcGISImagery)
    }
    val scope = rememberCoroutineScope()
    Map(
        modifier = Modifier.fillMaxSize(),
        mapProperties = mapProperties,
    ) { mapState ->

        scope.launch {
            mapState.onSingleTapConfirmed.collect {
                Log.e("Event", "Tapped Location: ${it?.mapPoint?.x},${it?.mapPoint?.y}")
                if (mapProperties.arcGISMap.value?.basemap?.value?.name?.contains("Night") == true) {
                    mapProperties.arcGISMap.value = ArcGISMap(BasemapStyle.ArcGISImagery)
                } else {
                    mapProperties.arcGISMap.value = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
                }
            }
        }
    }
}
