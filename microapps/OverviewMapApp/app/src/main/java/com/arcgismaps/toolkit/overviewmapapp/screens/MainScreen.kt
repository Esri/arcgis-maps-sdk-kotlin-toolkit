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

package com.arcgismaps.toolkit.overviewmapapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.overviewmap.OverviewMap

@Composable
fun MainScreen() {
    Column {
        val options = remember { listOf("MapView", "SceneView") }
        var selectedView by rememberSaveable { mutableIntStateOf(0) }

        Box(modifier = Modifier.weight(0.5f)) {
            val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }
            val visibleArea: MutableState<Polygon?> = remember { mutableStateOf(null) }

            if (selectedView == 0) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = remember {
                        ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        }
                    },
                    onViewpointChangedForCenterAndScale = {
                        viewpoint.value = it
                    },
                    onVisibleAreaChanged = {
                        visibleArea.value = it
                    }
                )
                OverviewMap(
                    viewpoint = viewpoint.value,
                    visibleArea = visibleArea.value,
                    modifier = Modifier.size(250.dp, 200.dp).padding(20.dp).align(Alignment.TopEnd)
                )
            } else {
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISLightGray).apply {
                            initialViewpoint = Viewpoint(
                                latitude = 39.8,
                                longitude = -98.6,
                                scale = 10e7
                            )
                        }
                    },
                    onViewpointChangedForCenterAndScale = {
                        viewpoint.value = it
                    },
                )
                OverviewMap(
                    viewpoint = viewpoint.value,
                    modifier = Modifier.size(250.dp, 200.dp).padding(20.dp).align(Alignment.TopEnd)
                )
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = {
                        selectedView = index
                    },
                    selected = index == selectedView,
                    label = { Text(label) }
                )
            }
        }
    }
}
