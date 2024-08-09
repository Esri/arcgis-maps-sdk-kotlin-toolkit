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

package com.arcgismaps.toolkit.tabletoparapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.ar.SceneView
import com.arcgismaps.toolkit.ar.TabletopArView
import com.arcgismaps.toolkit.ar.TabletopArView2
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun MainScreen() {
    val arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISImagery).apply { initialViewpoint = Viewpoint(34.056295, -117.195800, 100000.0) } }
    TabletopArView2 { tabletopArViewState ->
        Column {
            Text(text = "Tabletop AR")
            SceneView(
                arcGISScene = remember {
                    ArcGISScene(BasemapStyle.ArcGISTopographic)
                },
                tabletopArViewState = tabletopArViewState
            )
        }
    }
}
