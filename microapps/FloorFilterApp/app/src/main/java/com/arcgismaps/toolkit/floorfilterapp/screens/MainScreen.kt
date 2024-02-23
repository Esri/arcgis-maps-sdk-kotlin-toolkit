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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import com.arcgismaps.toolkit.indoors.FloorFilter
import com.arcgismaps.toolkit.indoors.FloorFilterSelection
import com.arcgismaps.toolkit.indoors.FloorFilterState

@Composable
fun MainScreen() {
    val floorAwareWebMap by remember {
        mutableStateOf(
            ArcGISMap(
                PortalItem(
                    Portal("https://arcgis.com/"),
                    "b4b599a43a474d33946cf0df526426f5"
                )
            )
        )
    }

    val mapViewProxy = remember { MapViewProxy() }

    val coroutineScope = rememberCoroutineScope()

    // use default UI properties
    val floorFilterState: FloorFilterState by remember {
        mutableStateOf(FloorFilterState(
            geoModel = floorAwareWebMap,
            coroutineScope = coroutineScope
        ) { floorFilterSelection ->
            when (floorFilterSelection.type) {
                is FloorFilterSelection.Type.FloorSite -> {
                    val floorFilterSelectionType =
                        floorFilterSelection.type as FloorFilterSelection.Type.FloorSite
                    floorFilterSelectionType.site.geometry?.let {
                        mapViewProxy.setViewpoint(Viewpoint(getEnvelopeWithBuffer(it)))
                    }
                }

                is FloorFilterSelection.Type.FloorFacility -> {
                    val floorFilterSelectionType =
                        floorFilterSelection.type as FloorFilterSelection.Type.FloorFacility
                    floorFilterSelectionType.facility.geometry?.let {
                        mapViewProxy.setViewpoint(Viewpoint(getEnvelopeWithBuffer(it)))
                    }
                }

                else -> {}
            }
        })
    }

    MapView(
        floorAwareWebMap,
        modifier = Modifier.fillMaxSize(),
        mapViewProxy = mapViewProxy
    )
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        FloorFilter(floorFilterState = floorFilterState)
    }
}

/**
 * Returns the envelope with an added buffer factor applied to the given Geometry's extent.
 *
 * @since 200.2.0
 */
private fun getEnvelopeWithBuffer(geometry: Geometry): Envelope {
    val bufferFactor = 1.25
    val envelope = geometry.extent
    return Envelope(envelope.center, envelope.width * bufferFactor, envelope.height * bufferFactor)
}
