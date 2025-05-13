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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.mapping.view.GeoView

/**
 * OverviewMap is a small, secondary [MapView] which shows a representation of the main [GeoView]'s
 * current [Viewpoint].
 *
 * Choose this overload if your main view is a [MapView]. If a non-default symbol is provided the
 * symbol must be suitable for a polygon geometry such as a [SimpleFillSymbol].
 *
 * @param viewpoint the viewpoint of the main view. This should be provided as a
 * [ViewpointType.CenterAndScale]
 * @param visibleArea the visible area of the main view
 * @param modifier the modifier to apply
 * @param symbol the symbol to apply. Must be suitable for a polygon geometry.
 * @param scaleFactor the factor to multiply the main view's scale by. The OverviewMap will
 * display at the product of mainGeoViewScale * scaleFactor.
 * @param arcGISMap the map to display
 *
 * @since 200.8.0
 */
@Composable
public fun OverviewMap(
    viewpoint: Viewpoint?,
    visibleArea: Polygon?,
    modifier: Modifier = Modifier,
    symbol: Symbol = remember {
        SimpleFillSymbol(
            outline = SimpleLineSymbol(color = Color.red),
            color = Color.transparent
        )
    },
    scaleFactor: Double = 25.0,
    arcGISMap: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) }
) {
    OverviewMapImpl(
        viewpoint = viewpoint,
        arcGISMap = arcGISMap,
        modifier = modifier,
        visibleArea = visibleArea,
        symbol = symbol,
        scaleFactor = scaleFactor,
    )
}

/**
 * OverviewMap is a small, secondary [MapView] which shows a representation of the main [GeoView]'s
 * current [Viewpoint].
 *
 * Choose this overload if your main view is a [SceneView]. If a non-default symbol is provided the
 * symbol must be suitable for a point geometry such as a [SimpleMarkerSymbol].
 *
 * @param viewpoint the viewpoint of the main view, this should be provided as a
 * [ViewpointType.CenterAndScale]
 * @param modifier the modifier to apply
 * @param symbol the symbol to apply. Must be suitable for a point geometry.
 * @param scaleFactor the factor to multiply the main view's scale by. The OverviewMap will
 * display at the product of mainGeoViewScale * scaleFactor.
 * @param arcGISMap the map to display
 *
 * @since 200.8.0
 */
@Composable
public fun OverviewMap(
    viewpoint: Viewpoint?,
    modifier: Modifier = Modifier,
    scaleFactor: Double = 25.0,
    symbol: Symbol = remember {
        SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.red,
            size = 20.0f
        )
    },
    arcGISMap: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) }
) {
    OverviewMapImpl(
        viewpoint = viewpoint,
        arcGISMap = arcGISMap,
        modifier = modifier,
        symbol = symbol,
        scaleFactor = scaleFactor,
    )
}

/**
 * Internal implementation of the OverViewMap.
 *
 * @param viewpoint the viewpoint of the main view this overview map is for, this be provided as a
 * [ViewpointType.CenterAndScale]
 * @param arcGISMap the map to display
 * @param modifier the modifier to apply
 * @param visibleArea the visible area. The visible area is only applicable when the main view is
 * a [MapView] and should be null for a [SceneView].
 * @param symbol the symbol used to visualize the main view's viewpoint. When the main view is a
 * [MapView] the symbol should be suitable for a polygon geometry, and when the main view is a
 * [SceneView] the symbol should be suitable for a point geometry.
 * @param scaleFactor the scale factor applied to the viewpoint
 * @since 200.8.0
 */
@Composable
private fun OverviewMapImpl(
    viewpoint: Viewpoint?,
    arcGISMap: ArcGISMap,
    modifier: Modifier = Modifier,
    visibleArea: Polygon? = null,
    symbol: Symbol? = null,
    scaleFactor: Double = 25.0,
) {
    val proxy = remember {
        MapViewProxy()
    }

    val graphic = remember {
        Graphic()
    }

    val graphicsOverlay = remember {
        GraphicsOverlay(listOf(graphic))
    }

    val graphicsOverlays = remember {
        listOf(graphicsOverlay)
    }

    MapView(
        modifier = modifier,
        arcGISMap = remember {
            arcGISMap.apply {
                viewpoint?.let {
                    initialViewpoint = scaledViewpoint(viewpoint, scaleFactor)
                }
            }
        },
        mapViewProxy = proxy,
        graphicsOverlays = graphicsOverlays,
        isAttributionBarVisible = false,
        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = false)
    )

    viewpoint?.let {
        if (viewpoint.viewpointType == ViewpointType.CenterAndScale) {
            proxy.setViewpoint(
                scaledViewpoint(viewpoint, scaleFactor)
            )
            if (visibleArea == null) {
                graphic.geometry = it.targetGeometry
            } else {
                graphic.geometry = visibleArea
            }
            graphic.symbol = symbol
        }
    }
}

internal fun scaledViewpoint(viewpoint: Viewpoint, scaleFactor: Double): Viewpoint {
    return if (viewpoint.viewpointType == ViewpointType.CenterAndScale) {
        Viewpoint(
            center = viewpoint.targetGeometry as Point,
            scale = viewpoint.targetScale * scaleFactor
        )
    } else {
        viewpoint
    }
}
