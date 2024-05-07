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

package com.arcgismaps.toolkit.compassapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.MapViewScope

@Composable
fun MainScreen() {
    // create an ArcGISMap with a Topographic basemap style
    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                // set the map's viewpoint to North America
                initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            }
        )
    }

    val arcGISMap2 by remember {
        mutableStateOf(
            ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                // set the map's viewpoint to North America
                initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            }
        )
    }
    var mapRotation by remember { mutableDoubleStateOf(0.0) }
    val mapViewProxy = remember { MapViewProxy() }
    val mapViewProxy2 = remember { MapViewProxy() }
    val disneyLand = remember { Point(-117.9190, 33.8121, SpatialReference.wgs84()) }
    var calloutMapOne: (@Composable MapViewScope.() -> Unit)? by remember {
        mutableStateOf(
            { Callout(location = disneyLand) { Text("Hello, World!", color = Color.Green) } }
        )
    }

    var calloutMapTwo: (@Composable MapViewScope.() -> Unit)? by remember {
        mutableStateOf(
            { Callout(location = disneyLand) { Text("Hello, World!", color = Color.Red) } }
        )
    }
    // show composable MapView with compass

    Column {
        Box(
            modifier = Modifier
                .weight(0.5f)
                .aspectRatio(1.0f)
        ) {
            MapView(
                arcGISMap,
                modifier = Modifier.fillMaxSize(),
//            modifier = Modifier.weight(0.5f).aspectRatio(1.0f),
                mapViewProxy = mapViewProxy,
//            onMapRotationChanged = { rotation -> mapRotation = rotation },
                content = calloutMapOne
            )

        }
        Box(
            modifier = Modifier
                .weight(0.5f)
                .aspectRatio(1.0f)
        ) {
            MapView(
                arcGISMap2,
                modifier = Modifier.fillMaxSize(),
//            modifier = Modifier.weight(0.5f).aspectRatio(1.0f),
                mapViewProxy = mapViewProxy2,
//            onMapRotationChanged = { rotation -> mapRotation = rotation },
                content = calloutMapTwo
            )
        }
        Button(onClick = {
            calloutMapOne = { Callout(location = disneyLand) { Text("Hello again Map One!", color = Color.Blue) } }
            calloutMapTwo = { Callout(location = disneyLand) { Text("Hello again Map Two!", color = Color.Magenta) } }
        }) {
            Text(text = "Change Callout Location in Map 1")
        }
    }
}
