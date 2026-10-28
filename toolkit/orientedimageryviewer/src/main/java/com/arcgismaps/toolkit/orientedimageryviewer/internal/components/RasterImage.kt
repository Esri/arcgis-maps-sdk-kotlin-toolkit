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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.awaitCancellation

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
    val viewerUiState = viewerState.uiState
    val imageRevision = viewerUiState.activeImageRevision
    val coroutineScope = key(viewerState, imageRevision) { rememberCoroutineScope() }

    val proxy = remember {
        MapViewProxy()
    }

    val graphicsOverlay = remember {
        GraphicsOverlay()
    }

    val referenceOverlay = remember { GraphicsOverlay() }
    val graphicOverlays = remember {
        listOf(graphicsOverlay, referenceOverlay)
    }

    val rasterPath = viewerUiState.activeImageInfo?.dataUri ?: return
    val rasterLayer = remember(rasterPath) {
        RasterLayer(Raster.createWithPath(rasterPath))
    }

    val arcGISMap = remember(rasterLayer) {
        ArcGISMap(Basemap(rasterLayer))
    }

    DisposableEffect(viewerState, rasterLayer, viewerUiState.activeImageRevision) {
        val toRasterPoint: (DoubleXY) -> Point? = { imagePoint ->
            computeRasterPoint(imagePoint, rasterLayer)
        }
        viewerState.registerMarkerOverlayInImage(graphicsOverlay, toRasterPoint)
        onDispose {
            viewerState.unregisterMarkerOverlayInImage(toRasterPoint)
        }
    }

    MapView(
        modifier = modifier,
        arcGISMap = arcGISMap,
        mapViewProxy = proxy,
        graphicsOverlays = graphicOverlays,
        isAttributionBarVisible = false,
        backgroundGrid = BackgroundGrid().apply { isVisible = false },
        mapViewInteractionOptions = MapViewInteractionOptions(isEnabled = true),
        onSingleTapConfirmed = onTap@{ event ->
            if (!viewerState.isMarkingLocation ||
                viewerState.uiState.activeImageRevision != imageRevision
            ) return@onTap
            val rasterPoint = event.mapPoint ?: return@onTap
            val imagePoint = computeImagePoint(rasterPoint, rasterLayer) ?: return@onTap
            coroutineScope.launch {
                if (viewerState.uiState.activeImageRevision != imageRevision) return@launch
                viewerState.addMarkerToRasterImage(
                    imagePoint,
                    rasterPoint,
                    graphicsOverlay
                )
            }
        },
        onViewpointChangedForBoundingGeometry = { viewpoint ->
            val currentUiState = viewerState.uiState
            if (currentUiState.activeImageRevision == imageRevision &&
                currentUiState.activeImageInfo != null && currentUiState.isActiveFootprintVisible &&
                !currentUiState.isSequentialNavigationEnabled
            ) {
                val imageCoordinates = computePixelCorners(viewpoint, rasterLayer)
                if (imageCoordinates != null) {
                    viewerState.updateFootprints(imageCoordinates)
                }
            }
            Log.i("RasterImage", "Viewpoint changed for bounding geometry: ${viewpoint.targetGeometry.toJson()}")
        }
    )

    val sequentialNavigation = viewerState.supportsSequentialNavigation && viewerUiState.isSequentialNavigationEnabled
    val inputLocationData = viewerUiState.inputLocationData.takeUnless { sequentialNavigation }
    LaunchedEffect(viewerState, rasterLayer, imageRevision, sequentialNavigation, inputLocationData) {
        var referenceGraphic: Graphic? = null
        fun isCurrent() = viewerState.uiState.activeImageRevision == imageRevision
        try {
            rasterLayer.load().getOrThrow()
            ensureActive()
            if (!isCurrent()) return@LaunchedEffect
            rasterLayer.fullExtent?.let { fullExtent ->
                proxy.setViewpointGeometry(fullExtent)
            }
            ensureActive()
            if (!isCurrent()) return@LaunchedEffect
            val imagePoint = if (sequentialNavigation) {
                val center = computeImageCenter(rasterLayer) ?: return@LaunchedEffect
                val location = viewerState.imageToLocation(center)?.getOrThrow() ?: return@LaunchedEffect
                ensureActive()
                if (!isCurrent()) return@LaunchedEffect
                viewerState.setInputLocation(location)
                center
            } else {
                val location = inputLocationData?.toPoint() ?: return@LaunchedEffect
                viewerState.locationToImage(location)?.getOrThrow() ?: return@LaunchedEffect
            }
            ensureActive()
            if (!isCurrent()) return@LaunchedEffect
            val rasterPoint = computeRasterPoint(imagePoint, rasterLayer) ?: return@LaunchedEffect
            referenceGraphic = Graphic(
                geometry = rasterPoint,
                symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.red, 20f)
            ).also { referenceOverlay.graphics.add(it) }
            // Keep ownership until this image/effect is replaced or disposed.
            awaitCancellation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ensureActive()
            if (isCurrent()) Log.e("RasterImage", "Unable to initialize the raster image", error)
        } finally {
            referenceGraphic?.let { referenceOverlay.graphics.remove(it) }
        }
    }
}

internal fun computeImageCenter(rasterLayer: RasterLayer): DoubleXY? {
    val rasterInfo = runCatching { rasterLayer.raster?.rasterInfo }.getOrNull() ?: return null
    val centerPoint = rasterInfo.extent?.center ?: return null
    return computeImagePoint(centerPoint, rasterLayer)
}

internal fun computeRasterPoint(imagePoint: DoubleXY, rasterLayer: RasterLayer): Point? {
    val transform = rasterTransform(rasterLayer) ?: return null
    if (!imagePoint.x.isFinite() || !imagePoint.y.isFinite()) return null
    val x = transform.extent.xMin + imagePoint.x * transform.cellSizeX
    val y = transform.extent.yMax - imagePoint.y * transform.cellSizeY
    if (!transform.contains(x, y)) return null
    return Point(x, y, rasterLayer.spatialReference)
}

internal fun computeImagePoint(rasterPoint: Point, rasterLayer: RasterLayer): DoubleXY? {
    val transform = rasterTransform(rasterLayer) ?: return null
    val sourceReference = rasterPoint.spatialReference
    val targetReference = rasterLayer.spatialReference
    val point = when {
        sourceReference == targetReference -> rasterPoint
        targetReference == null -> {
            // An unreferenced raster has no spatial reference, but MapView assigns its taps
            // a local, unit-only reference. These are already raster-display coordinates;
            // there is no geographic projection to perform. Do not accept geographic or
            // projected coordinates as though they belonged to this local image space.
            if (sourceReference != null &&
                (sourceReference.isGeographic || sourceReference.isProjected)
            ) return null
            rasterPoint
        }
        sourceReference == null -> return null
        else -> GeometryEngine.projectOrNull(rasterPoint, targetReference) ?: return null
    }
    return transform.toImagePoint(point)
}

private data class RasterTransform(val extent: Envelope, val cellSizeX: Double, val cellSizeY: Double) {
    fun contains(x: Double, y: Double): Boolean = x.isFinite() && y.isFinite() &&
        x in extent.xMin..extent.xMax && y in extent.yMin..extent.yMax

    fun toImagePoint(point: Point): DoubleXY? {
        if (!contains(point.x, point.y)) return null
        val x = (point.x - extent.xMin) / cellSizeX
        val y = (extent.yMax - point.y) / cellSizeY
        return if (x.isFinite() && y.isFinite()) DoubleXY(x, y) else null
    }
}

private fun rasterTransform(rasterLayer: RasterLayer): RasterTransform? {
    val rasterInfo = runCatching { rasterLayer.raster?.rasterInfo }.getOrNull() ?: return null
    val extent = rasterInfo.extent ?: return null
    val cellSizeX = rasterInfo.cellSizeX
    val cellSizeY = rasterInfo.cellSizeY
    if (!cellSizeX.isFinite() || !cellSizeY.isFinite() || cellSizeX <= 0.0 || cellSizeY <= 0.0 ||
        !extent.xMin.isFinite() || !extent.xMax.isFinite() ||
        !extent.yMin.isFinite() || !extent.yMax.isFinite() || extent.isEmpty
    ) return null
    return RasterTransform(extent, cellSizeX, cellSizeY)
}


private fun computeVisiblePolygon(viewpoint: Viewpoint, spatialReference: SpatialReference?): Polygon? {
    val extent = viewpoint.targetGeometry.extent
    val rotation = viewpoint.rotation.takeIf { it.isFinite() } ?: 0.0
    val center = Point(extent.center.x, extent.center.y, extent.spatialReference)
    val polygon = GeometryEngine.rotate(extent.toPolygon(), rotation, center) as? Polygon ?: return null
    if (polygon.spatialReference == spatialReference) return polygon
    if (polygon.spatialReference == null || spatialReference == null) return null
    // Binary geometry operations require equivalent vertical references, even for XY-only
    // geometries. Project the rotated viewport rather than relabeling its coordinates.
    return GeometryEngine.projectOrNull(polygon, spatialReference)?.takeUnless { it.isEmpty }
}

internal fun computePixelCorners(viewpoint: Viewpoint, rasterLayer: RasterLayer): List<DoubleXY>? {
    val imageExtent = rasterLayer.fullExtent ?: return null
    val transform = rasterTransform(rasterLayer) ?: return null

    if (!rasterLayer.isVisible) {
        return null
    }

    val visiblePolygon = computeVisiblePolygon(viewpoint, imageExtent.spatialReference) ?: return null
    val imagePolygon = imageExtent.toPolygon()
    val intersectionPolygon = GeometryEngine.intersectionOrNull(
        geometry1 = visiblePolygon,
        geometry2 = imagePolygon
    ) as? Polygon ?: return null

    if (intersectionPolygon.isEmpty) {
        return null
    }

    val pixelPoints = intersectionPolygon.parts
        .flatMap { part -> part.points }
        .map { point ->
            if (!point.x.isFinite() || !point.y.isFinite()) return null
            // Intersection has already clipped the polygon. Geometry-engine precision can leave
            // edge vertices slightly outside the extent; do not apply this clamping to taps.
            val clippedPoint = Point(
                point.x.coerceIn(transform.extent.xMin, transform.extent.xMax),
                point.y.coerceIn(transform.extent.yMin, transform.extent.yMax)
            )
            transform.toImagePoint(clippedPoint) ?: return null
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
