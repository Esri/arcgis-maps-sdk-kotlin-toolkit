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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.MapView

/**
 * MapInsets control the active visible area, instructing the Map to ignore parts that may be obstructed
 * by overlaid UI elements and affecting the Map's logical center, the reported visible area and
 * the location display.
 * Units for start, top, end and bottom are specified in device-independent pixels (dp).
 *
 * @since 200.3.0
 */
public data class MapInsets(
    var start: Double = 30.0,
    var top: Double = 30.0,
    var end: Double = 30.0,
    var bottom: Double = 30.0
)

/**
 * A compose equivalent of the [MapView].
 *
 * @param modifier Modifier to be applied to the Map
 * @param arcGISMap the [ArcGISMap] to be rendered by this composable
 * @param mapInsets the start, top, end and bottom insets on the Map
 * @param overlay the composable overlays to display on top of the Map. Example, a compass, floorfilter etc.
 * @since 200.3.0
 */
@Composable
public fun Map(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap? = null,
    mapInsets: MapInsets = MapInsets(),
    overlay: @Composable () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mapView = remember { MapView(context) }.apply {
        map = arcGISMap
    }

    Box(modifier = Modifier.semantics {
        contentDescription = "MapContainer"
    }) {
        AndroidView(modifier = modifier
            .semantics {
                contentDescription = "MapView"
            }, factory = { mapView }) {
            it.setViewInsets(mapInsets.start, mapInsets.end, mapInsets.top, mapInsets.bottom)
        }

        overlay()
    }

    LaunchedEffect(Unit) {
        mapView.setViewInsets(mapInsets.start, mapInsets.end, mapInsets.top, mapInsets.bottom)
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
            mapView.onDestroy(lifecycleOwner)
        }
    }
}

@Preview
@Composable
internal fun MapPreview() {
    Map()
}
