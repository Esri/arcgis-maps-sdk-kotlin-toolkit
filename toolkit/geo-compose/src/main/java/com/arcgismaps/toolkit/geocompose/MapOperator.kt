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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate

internal val LocalMapView = compositionLocalOf<MapView?> { null }

@Composable
public fun MapOperator(
    content: @Composable (mapOperator: MapOperator) -> Unit
) {
    val context = LocalContext.current
    val mapOperator = remember { MapOperator(MapView(context)) }

    CompositionLocalProvider(LocalMapView provides mapOperator.mapView) {
        content(mapOperator)
    }
}

public class MapOperator internal constructor(public val mapView: MapView) {
    public fun screenToLocation(screenCoordinate: ScreenCoordinate): Point? =
        mapView.screenToLocation(screenCoordinate)

    public val visibleArea: Polygon?
        get() = mapView.visibleArea

    public val graphicsOverlays: MutableList<GraphicsOverlay>
        get() = mapView.graphicsOverlays

    public suspend fun setViewpointAnimated(viewpoint: Viewpoint): Result<Boolean> =
        mapView.setViewpointAnimated(viewpoint)

    public fun setViewpoint(viewpoint: Viewpoint): Unit =
        mapView.setViewpoint(viewpoint)
}
