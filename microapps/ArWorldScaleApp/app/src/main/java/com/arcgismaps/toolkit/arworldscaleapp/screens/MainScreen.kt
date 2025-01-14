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

package com.arcgismaps.toolkit.arworldscaleapp.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.location.SystemLocationDataSource
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.ar.WorldScaleSceneView

@Composable
fun MainScreen() {
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        initialViewpoint = Viewpoint(
            latitude = 39.8,
            longitude = -98.6,
            scale = 10e7
        )
    }
    WorldScaleSceneView(
        arcGISScene = arcGISScene,
        modifier = Modifier.fillMaxSize(),
        onInitializationStatusChanged = {
            Log.d("ArWorldScaleApp", "Initialization status changed: $it")
        }
    )
}
