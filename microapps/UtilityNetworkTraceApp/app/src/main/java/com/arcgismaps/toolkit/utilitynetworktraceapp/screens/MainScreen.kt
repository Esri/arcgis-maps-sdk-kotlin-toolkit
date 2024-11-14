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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.Trace
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TraceViewModel) {

    val loadState by viewModel.arcGISMap.loadStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val addStartingPointMode by viewModel.traceState.addStartingPointMode
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )

    LaunchedEffect(key1 = addStartingPointMode) {
        if (addStartingPointMode == AddStartingPointMode.Started) {
            scaffoldState.bottomSheetState.partialExpand()
        } else if (addStartingPointMode == AddStartingPointMode.Stopped) {
            scaffoldState.bottomSheetState.expand()
        }
    }

    BottomSheetScaffold(
        sheetContent = {
            AnimatedVisibility(
                visible = loadState is LoadStatus.Loaded,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "trace tool",
                modifier = Modifier.heightIn(min = 0.dp, max = 350.dp)
            ) {
                Trace(traceState = viewModel.traceState)
            }
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 100.dp,
        sheetSwipeEnabled = true,
        topBar = null
    ) { padding ->
        MapView(
            arcGISMap = viewModel.arcGISMap,
            mapViewProxy = viewModel.mapViewProxy,
            graphicsOverlays = listOf(viewModel.graphicsOverlay),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                singleTapConfirmedEvent.mapPoint?.let {
                    coroutineScope.launch {
                        viewModel.traceState.addStartingPoint(it)
                    }
                }
            }
        )
    }
}
