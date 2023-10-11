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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.compass.Compass
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.MapState
import com.arcgismaps.toolkit.indoors.FloorFilter
import com.arcgismaps.toolkit.indoors.FloorFilterState

@Composable
fun MainScreen() {

    val portal = Portal("https://arcgis.com/")
    val portalItem = PortalItem(portal, "b4b599a43a474d33946cf0df526426f5")
    val floorAwareWebMap = ArcGISMap(portalItem)

    val mapState = remember { MapState(arcGISMap = floorAwareWebMap) }
    val floorFilterState = FloorFilterState(floorAwareWebMap, coroutineScope = rememberCoroutineScope())
    Map(
        modifier = Modifier.fillMaxSize(),
        mapState = mapState,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            FloorFilter(floorFilterState = floorFilterState)
        }
    }
}
