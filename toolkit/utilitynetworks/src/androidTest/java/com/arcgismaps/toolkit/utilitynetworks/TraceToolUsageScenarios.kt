/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arcgismaps.toolkit.utilitynetworks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

/**
 * Composable Scenarios for the [TraceToolTests]
 *
 * @since 200.6.0
 */
class TraceToolUsageScenarios {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MapViewWithTraceInBottomSheet(
        arcGISMap: ArcGISMap,
        mapViewProxy: MapViewProxy,
        graphicsOverlay: GraphicsOverlay,
        onDrawStatusCompleted: () -> Unit,
        trace: @Composable () -> Unit
    ) {

        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Expanded,
                skipHiddenState = true
            )
        )

        BottomSheetScaffold(
            sheetContent = {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically { h -> h },
                    exit = slideOutVertically { h -> h },
                    label = "trace tool",
                    modifier = Modifier.heightIn(min = 0.dp, max = 350.dp)
                ) {
                    trace()
                }
            },
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 100.dp,
            sheetSwipeEnabled = true,
            topBar = null
        ) {
            Column {
                MapView(
                    arcGISMap = arcGISMap,
                    mapViewProxy = mapViewProxy,
                    graphicsOverlays = listOf(graphicsOverlay),
                    modifier = Modifier
                        .fillMaxSize(),
                    onDrawStatusChanged = { drawStatus ->
                        if (drawStatus == DrawStatus.Completed) {
                            onDrawStatusCompleted()
                        }
                    }
                )
            }
        }
    }
}
