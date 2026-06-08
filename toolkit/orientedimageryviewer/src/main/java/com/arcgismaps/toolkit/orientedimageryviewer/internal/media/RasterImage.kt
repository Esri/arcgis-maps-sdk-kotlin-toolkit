/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.media

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.BackgroundGrid
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.raster.Raster
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

/**
 * RasterImage is a small, secondary [MapView] which displays georeferenced raster image.
 *
 * @param viewerState the [OrientedImageryViewerState] which contains the active oriented image and its associated data
 * @param modifier the modifier to apply
 */
@Composable
internal fun RasterImage(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier
) {
    val proxy = remember {
        MapViewProxy()
    }

    val graphicsOverlay = remember {
        GraphicsOverlay()
    }

    val graphicOverlays = remember {
        listOf(graphicsOverlay)
    }

    val rasterPath = viewerState.activeImageInfo?.dataUri ?: return
    val rasterLayer = remember(rasterPath) {
        RasterLayer(Raster.createWithPath(rasterPath))
    }

    val arcGISMap = remember(rasterLayer) {
        ArcGISMap(Basemap(rasterLayer))
    }

    MapView(
        modifier = modifier,
        arcGISMap = arcGISMap,
        mapViewProxy = proxy,
        graphicsOverlays = graphicOverlays,
        isAttributionBarVisible = false,
        backgroundGrid = BackgroundGrid().apply { isVisible = false },
        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = true),
        onSingleTapConfirmed = { event ->
            viewerState.coroutineScope.launch {
                val result = viewerState.imageToLocation(event.screenCoordinate)
                if (result != null) {
                    result.onSuccess { _ ->
                        // TODO: add a graphic to the graphics overlay of GeoView for the returned location
                    }.onFailure { error ->
                        Log.e(
                            "RasterImage",
                            "imageToLocation failed for screen coordinates: (${event.screenCoordinate.x}, ${event.screenCoordinate.y}) with error: ${error.message}"
                        )
                    }
                } else {
                    Log.e(
                        "RasterImage",
                        "imageToLocation returned no active oriented image for screen coordinates: (${event.screenCoordinate.x}, ${event.screenCoordinate.y})"
                    )
                }
            }
        },
        onViewpointChangedForBoundingGeometry = { viewpoint ->
            if ((viewerState.activeImageInfo != null) && viewerState.isActiveFootprintVisible() && !viewerState.isSequentialNavigationEnabled) {
                val imageCoordinates = computePixelCorners(viewpoint, rasterLayer)
                if (imageCoordinates != null) {
                    viewerState.updateFootprints(imageCoordinates)
                }
            }
        }
    )

    LaunchedEffect(rasterLayer) {
        rasterLayer.load().onSuccess {
            rasterLayer.fullExtent?.let { fullExtent ->
                proxy.setViewpointGeometry(fullExtent)
            }
            if (viewerState.supportsSequentialNavigation && viewerState.isSequentialNavigationEnabled) {
                val imageCenter = computeImageCenter(rasterLayer)
                if (imageCenter != null) {
                    val graphic = Graphic(
                        geometry = Point(imageCenter.x, -imageCenter.y, rasterLayer.spatialReference),
                        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.red, 20f)
                    )
                    graphicsOverlay.graphics.clear()
                    graphicsOverlay.graphics.add(graphic)

                    val result = viewerState.imageToLocation(imageCenter)
                    if (result != null) {
                        result.onSuccess { point ->
                            viewerState.setInputLocation(point)
                        }.onFailure { error ->
                            Log.e(
                                "RasterImage",
                                "imageToLocation failed for image center: (${imageCenter.x}, ${imageCenter.y}) with error: ${error.message}"
                            )
                        }
                    } else {
                        Log.e("RasterImage", "imageToLocation returned no active oriented image for image center: (${imageCenter.x}, ${imageCenter.y})")
                    }
                } else {
                    Log.e("RasterImage", "Failed to compute image center for raster layer.")
                }
            } else {
                val inputLocation = viewerState.inputLocationData?.toPoint() ?: return@LaunchedEffect
                val result = viewerState.locationToImage(inputLocation)
                if (result != null) {
                    result.onSuccess { imagePoint ->
                        val graphic = Graphic(
                            geometry = Point(
                                imagePoint.x,
                                -imagePoint.y,
                                rasterLayer.spatialReference
                            ),
                            symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.red, 20f)
                        )
                        graphicsOverlay.graphics.clear()
                        graphicsOverlay.graphics.add(graphic)
                    }.onFailure { error ->
                        Log.e(
                            "RasterImage",
                            "locationToImage failed for map location: ${inputLocation.toJson()} with error: ${error.message}"
                        )
                    }
                } else {
                    Log.e(
                        "RasterImage",
                        "locationToImage returned no active oriented image for map location: ${inputLocation.toJson()}"
                    )
                }
            }
        }
    }
}

internal fun computeImageCenter(rasterLayer: RasterLayer): DoubleXY? {
    val rasterInfo = runCatching { rasterLayer.raster?.rasterInfo }.getOrNull() ?: return null

    val rasterExtent = rasterInfo.extent ?: return null
    val rasterXMin = rasterExtent.xMin
    val rasterYMax = rasterExtent.yMax
    val cellSizeX = rasterInfo.cellSizeX
    val cellSizeY = rasterInfo.cellSizeY
    if (cellSizeX == 0.0 || cellSizeY == 0.0) {
        return null
    }

    val centerPoint = rasterExtent.center
    return DoubleXY(
        x = (centerPoint.x - rasterXMin) / cellSizeX,
        y = (rasterYMax - centerPoint.y) / cellSizeY
    )
}

internal fun computePixelCorners(viewpoint: Viewpoint, rasterLayer: RasterLayer): List<DoubleXY>? {
    val extent = viewpoint.targetGeometry.extent
    val rotation = if (viewpoint.rotation.isNaN()) 0.0 else viewpoint.rotation
    val imageExtent = rasterLayer.fullExtent ?: return null
    val rasterInfo = runCatching { rasterLayer.raster?.rasterInfo }.getOrNull() ?: return null

    if (!rasterLayer.isVisible) {
        return null
    }

    val visiblePolygon = GeometryEngine.rotate(extent.toPolygon(), rotation, extent.center) as? Polygon ?: return null
    val imagePolygon = imageExtent.toPolygon()
    val intersectionPolygon = GeometryEngine.intersectionOrNull(
        geometry1 = visiblePolygon,
        geometry2 = imagePolygon
    ) as? Polygon ?: return null

    if (intersectionPolygon.isEmpty) {
        return null
    }

    val rasterExtent = rasterInfo.extent ?: return null
    val rasterXMin = rasterExtent.xMin
    val rasterYMax = rasterExtent.yMax
    val cellSizeX = rasterInfo.cellSizeX
    val cellSizeY = rasterInfo.cellSizeY
    if (cellSizeX == 0.0 || cellSizeY == 0.0) {
        return null
    }

    val pixelPoints = intersectionPolygon.parts
        .flatMap { part -> part.points }
        .map { point ->
            DoubleXY(
                x = (point.x - rasterXMin) / cellSizeX,
                y = (rasterYMax - point.y) / cellSizeY
            )
        }

    return pixelPoints.toOrderedCornerRing()
}

private fun List<DoubleXY>.toOrderedCornerRing(): List<DoubleXY>? {
    if (isEmpty()) {
        return null
    }

    val uniquePoints = distinctBy { point -> point.x to point.y }
    val topLeft = uniquePoints.minBy { point -> point.x + point.y }
    val topRight = uniquePoints.maxBy { point -> point.x - point.y }
    val bottomRight = uniquePoints.maxBy { point -> point.x + point.y }
    val bottomLeft = uniquePoints.minBy { point -> point.x - point.y }

    return listOf(topLeft, topRight, bottomRight, bottomLeft, topLeft)
}

private fun Envelope.toPolygon(): Polygon {
    val spatialReference = spatialReference
    return Polygon(
        points = listOf(
            Point(xMin, yMin, spatialReference),
            Point(xMax, yMin, spatialReference),
            Point(xMax, yMax, spatialReference),
            Point(xMin, yMax, spatialReference),
            Point(xMin, yMin, spatialReference)
        ),
        spatialReference = spatialReference
    )
}
