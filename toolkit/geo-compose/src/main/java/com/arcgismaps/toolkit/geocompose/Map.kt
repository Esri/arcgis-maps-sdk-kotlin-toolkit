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

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
public fun Map(modifier: Modifier = Modifier, mapState: MapState = MapState()) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    AndroidView(modifier = modifier, factory = { mapView })

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
            mapView.onDestroy(lifecycleOwner)
        }
    }

    LaunchedEffect(mapState) {
        launch {
            mapState.arcGISMap.collect {
                mapView.map = it
            }
        }

        // Collect viewpoint operations and pass the results back to the channel
        launch {
            mapState.viewpointChannel.receiveAsFlow().collect {
                launch {
                    when (it) {
                        is ViewpointOperation.ViewpointAnimated -> {
                            val result = mapView.setViewpointAnimated(
                                viewpoint = it.viewpoint,
                                durationSeconds = it.durationSeconds,
                                curve = it.curve
                            )
                            it.completeWith(result)
                        }

                        is ViewpointOperation.ViewpointCenter -> {
                            val result = mapView.setViewpointCenter(it.center)
                            it.completeWith(result)
                        }

                        is ViewpointOperation.ViewpointCenterAndScale -> {
                            val result = mapView.setViewpointCenter(it.center, it.scale)
                            it.completeWith(result)
                        }

                        is ViewpointOperation.ViewpointGeometry -> {
                            val result = mapView.setViewpointGeometry(it.boundingGeometry)
                            it.completeWith(result)
                        }

                        is ViewpointOperation.ViewpointRotation -> {
                            val result = mapView.setViewpointRotation(it.angleDegrees)
                            it.completeWith(result)
                        }

                        is ViewpointOperation.ViewpointScale -> {
                            val result = mapView.setViewpointScale(it.scale)
                            it.completeWith(result)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
internal fun MapPreview() {
    Map()
}
