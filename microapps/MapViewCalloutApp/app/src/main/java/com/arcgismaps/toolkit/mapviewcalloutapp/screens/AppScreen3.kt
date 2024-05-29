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

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geoviewcompose.MapView

// Case c.
//- Show a MapView with a map with a GraphicLayer with some graphics
//- Display callout using Point(tap location) on a Graphic (with some HTML content ??)
//- Display a graphic at the tapped location
//- add switch to enable/disable animation

@Composable
fun AppScreen3(){
    Box(modifier = Modifier.fillMaxSize()){
        MapView(
            arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)
        )
    }
}
