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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.OverviewMap
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.overviewmapapp.ViewModel

/**
 * The main screen of the application consisting of tabs that show either a [MapView] or a [SceneView]
 * with an inset [OverviewMap].
 *
 *  @since 200.8.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: ViewModel = viewModel()

    Column {
        var tabIndex by rememberSaveable { mutableIntStateOf(0) }
        val tabs = listOf("MapView", "SceneView")

        Column(modifier = Modifier.fillMaxWidth()) {
            PrimaryTabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = {
                            Text(title)
                        },
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                        }
                    )
                }
            }
            when (tabIndex) {
                0 -> {
                    Box {
                        val visibleArea: MutableState<Polygon?> = remember { mutableStateOf(null) }

                        MapView(
                            modifier = Modifier.fillMaxSize(),
                            arcGISMap = remember {
                                ArcGISMap(BasemapStyle.ArcGISDarkGray).apply {
                                    initialViewpoint = viewModel.viewpointForMapView.value
                                }
                            },
                            onViewpointChangedForCenterAndScale = {
                                viewModel.viewpointForMapView.value = it
                            },
                            onVisibleAreaChanged = {
                                visibleArea.value = it
                            }
                        )
                        OverviewMap(
                            viewpoint = viewModel.viewpointForMapView.value,
                            visibleArea = visibleArea.value,
                            modifier = Modifier
                                .size(250.dp, 200.dp)
                                .padding(20.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                1 -> {
                    Box {
                        SceneView(
                            modifier = Modifier.fillMaxSize(),
                            arcGISScene = remember {
                                ArcGISScene(BasemapStyle.ArcGISDarkGray).apply {
                                    initialViewpoint = viewModel.viewpointForSceneView.value
                                }
                            },
                            onViewpointChangedForCenterAndScale = {
                                viewModel.viewpointForSceneView.value = it
                            },
                        )
                        OverviewMap(
                            viewpoint = viewModel.viewpointForSceneView.value,
                            modifier = Modifier
                                .size(250.dp, 200.dp)
                                .padding(20.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}

