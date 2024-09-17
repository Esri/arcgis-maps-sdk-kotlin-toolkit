/*
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
 */

package com.arcgismaps.toolkit.utilitynetworktraceapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.Trace
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.GraphicsOverlay

import kotlinx.coroutines.launch

private val napervilleUtilities = "471eb0bf37074b1fbb972b1da70fb310"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {

    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(
                PortalItem(
                    Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
                    napervilleUtilities
                )
            )
        )
    }

    val mapViewProxy = remember { MapViewProxy() }

    val graphicsOverlay = remember { GraphicsOverlay() }

    val tapPoint = remember { mutableStateOf<Point?>(null) }

    val loadState by arcGISMap.loadStatus.collectAsState()

    val inAddStartingPointMode = remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    val onAddStartingPointModeChanged: (AddStartingPointMode) -> Unit = { addStartingPointMode ->
        if (addStartingPointMode == AddStartingPointMode.Started) {
            inAddStartingPointMode.value = true
            coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else if (addStartingPointMode == AddStartingPointMode.Stopped) {
            coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
        }
    }

    BottomSheetScaffold(
        sheetContent = {
            AnimatedVisibility(
                visible = loadState is LoadStatus.Loaded,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "popup",
                modifier = Modifier.heightIn(min = 0.dp, max = 250.dp)
            ) {
                Trace(arcGISMap, mapViewProxy, graphicsOverlay, tapPoint.value, onAddStartingPointModeChanged)
            }
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = true,
        topBar = null
    ) { padding ->
        MapView(
            arcGISMap = arcGISMap,
            mapViewProxy = mapViewProxy,
            graphicsOverlays = listOf(graphicsOverlay),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                singleTapConfirmedEvent.mapPoint?.let {
                    if (inAddStartingPointMode.value) {
                        tapPoint.value = it
                    }
//                        viewModel.traceState.addStartingPoint(it)
                }
            }
        )
    }
}
