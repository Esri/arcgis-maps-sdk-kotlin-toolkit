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

package com.arcgismaps.toolkit.scalebarapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.scalebar.Scalebar

@Composable
fun MainScreen(modifier: Modifier) {
    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                initialViewpoint = Viewpoint(
                    latitude = 39.8,
                    longitude = -98.6,
                    scale = 10e7
                )
            }
        )
    }
    var viewpoint: Viewpoint? by remember { mutableStateOf(null) }
    var unitsPerDip by remember { mutableDoubleStateOf(Double.NaN) }
    var spatialReference: SpatialReference? by remember { mutableStateOf(null) }
    // show composable MapView with a Scalebar
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = arcGISMap,
            onSpatialReferenceChanged = { spatialReference = it },
            onUnitsPerDipChanged = { unitsPerDip = it },
            onViewpointChangedForCenterAndScale = { viewpoint = it }
        )
        Scalebar(
            modifier = Modifier
                .padding(25.dp)
                .align(Alignment.BottomStart),
            maxWidth = 175.dp,
            unitsPerDip = unitsPerDip,
            viewpoint = viewpoint,
            spatialReference = spatialReference
        )
    }
}
