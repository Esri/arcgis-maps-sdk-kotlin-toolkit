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

package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

@Composable
public fun ComposableMap(
    mapState: MapState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val map by mapState.map.collectAsState()
    val insets by mapState.insets.collectAsState()
    val mapView = remember {
        MapView(context).also { view ->
            with(view) {
                with(coroutineScope) {
                    launch {
                        view.onDown.collect {
                            mapState.onDown(it)
                        }
                    }
                    launch {
                        view.onUp.collect {
                            mapState.onUp(it)
                        }
                    }
                    launch {
                        view.onSingleTapConfirmed.collect {
                            mapState.onSingleTapConfirmed(it)
                        }
                    }
                    launch {
                        view.onDoubleTap.collect {
                            mapState.onDoubleTap(it)
                        }
                    }
                    launch {
                        view.onLongPress.collect {
                            mapState.onLongPress(it)
                        }
                    }
                    launch {
                        view.onTwoPointerTap.collect {
                            mapState.onTwoPointerTap(it)
                        }
                    }
                    launch {
                        view.onPan.collect {
                            mapState.onPan(it)
                        }
                    }
                    launch {
                        view.mapRotation.collect {
                            mapState.onViewpointRotationChanged(it)
                        }
                    }
                    launch {
                        view.viewpointChanged.collect {
                            view.getCurrentViewpoint(ViewpointType.CenterAndScale)?.let {
                                mapState.onViewpointChanged(it)
                            }
                        }
                    }
                }
            }
        }
    }

    mapView.map = map
    mapView.setViewInsets(
        left = insets.start,
        right = insets.end,
        top = insets.top,
        bottom = insets.bottom
    )

    LaunchedEffect(Unit) {
        launch {
            mapState.mapRotation.collect(DuplexFlow.Type.Write) {
                mapView.setViewpointRotation(it)
            }
        }
        launch {
            mapState.viewpoint.collect(DuplexFlow.Type.Write) {
                it?.let {
                    mapView.setViewpoint(it)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }
    
    Box(modifier = modifier.semantics {
        contentDescription = "MapContainer"
    }) {
        AndroidView(modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "MapView"
            }, factory = { mapView })
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.start.dp,
                end = insets.end.dp,
                top = insets.top.dp,
                bottom = insets.bottom.dp
            )
            .semantics {
                contentDescription = "Content"
            }) {
            content()
        }
    }
}
