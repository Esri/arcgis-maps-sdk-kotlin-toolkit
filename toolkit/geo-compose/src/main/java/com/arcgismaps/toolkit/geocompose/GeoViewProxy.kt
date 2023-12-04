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

import androidx.compose.ui.unit.Dp
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.IdentifyGraphicsOverlayResult
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.ScreenCoordinate

/**
 * Used to perform operations on a composable MapView or SceneView.
 *
 * @since 200.3.0
 */
public sealed class GeoViewProxy {

    private var geoView: GeoView? = null

    protected fun setGeoView(geoView: GeoView?) {
        this.geoView = geoView
        // The first time it's not null, we will set this to the appropriate class name
        geoView?.let {
            classNameForErrorMessage = when (it) {
                is MapView -> "MapView"
                is SceneView -> "SceneView"
            }
        }
    }

    private var classNameForErrorMessage = "GeoView"

    private val nullGeoViewErrorMessage: String
        // use a get() here so that this gets recalculated after the geoView is sete
        get() {
            return "$classNameForErrorMessage must be part of the composition when this function is called."
        }


    /**
     * Initiate an Identify operation on the specified [graphicsOverlay].
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any graphics
     * that intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance
     * of 0 tests just the physical pixel at [screenCoordinate].
     * Tolerance values above 0 are in DIPs and specify a circular region centered on [screenCoordinate], with radius equal
     * to [tolerance]. The maximum allowed tolerance value is 100 DIPs, resulting in an identify circle of diameter
     * 200 DIPs.
     *
     * The [returnPopupsOnly] parameter controls what properties are populated in the [IdentifyGraphicsOverlayResult]
     * instance that is returned by the identify operation:
     * * true: only the [IdentifyGraphicsOverlayResult.popups] property will be populated with results. If the overlay
     * does not have popups an error will be returned.
     * * false: the [IdentifyGraphicsOverlayResult.graphics] property will be populated, and the
     * [IdentifyGraphicsOverlayResult.popups] property will be populated if the overlay has popups.
     *
     * @param graphicsOverlay overlay on which to run the identify
     * @param screenCoordinate location at which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the graphics property of the result is populated
     * @param maximumResults maximum size of the result set of ref@Graphic to return. -1 indicates unlimited results
     * @return A [Result] containing an [IdentifyGraphicsOverlayResult], or failure
     * @since 200.0.0
     */
    public suspend fun identify(
        graphicsOverlay: GraphicsOverlay,
        screenCoordinate: ScreenCoordinate,
        tolerance: Dp,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int = 1
    ) : Result<IdentifyGraphicsOverlayResult> {
        return geoView?.identifyGraphicsOverlay(
            graphicsOverlay, screenCoordinate, tolerance.value.toDouble(), returnPopupsOnly, maximumResults
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Initiate an Identify operation on all graphics overlays.
     *
     * Results are returned in top-to-bottom order.
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any graphics that
     * intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance of
     * 0 tests just the physical pixel at [screenCoordinate]. Tolerance values above 0 are in DIPs and specify a
     * circular region centered on [screenCoordinate], with radius equal to [tolerance]. The maximum allowed tolerance
     * value is 100 DIPs, resulting in an identify circle of diameter 200 DIPs.
     *
     * The [returnPopupsOnly] parameter controls what properties are populated in the [IdentifyGraphicsOverlayResult]
     * instances that are returned by the identify operation:
     * * true: only [IdentifyGraphicsOverlayResult.popups] properties will be populated. Overlays
     * without popups will be omitted.
     * * false: [IdentifyGraphicsOverlayResult.graphics] properties will be populated. Overlays
     * with popups will also return popups.
     *
     * @param screenCoordinate location on which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the graphics property of the results are populated
     * @return A [Result] containing a [List] of [IdentifyGraphicsOverlayResult] containing one entry for each
     * overlay in the view, or failure. Each entry holds a [GraphicsOverlay] and a [List] of [com.arcgismaps.mapview.Graphic]s
     * @since 200.0.0
     */
    public suspend fun identifyGraphicsOverlays(
        screenCoordinate: ScreenCoordinate,
        tolerance: Double,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int = 1
    ) : Result<List<IdentifyGraphicsOverlayResult>> {
        return geoView?.identifyGraphicsOverlays(
            screenCoordinate, tolerance, returnPopupsOnly, maximumResults
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Initiate an Identify operation on the specified [layer].
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any GeoElements
     * that intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance of
     * 0 tests just the physical pixel at [screenCoordinate]. Tolerance values above 0 are in DIPs and specify a circular
     * region centered on [screenCoordinate], with radius equal to [tolerance]. The maximum allowed tolerance value is 100 DIPs,
     * resulting in an identify circle of diameter 200 DIPs. The [returnPopupsOnly] parameter controls what properties
     * are populated in the [IdentifyLayerResult] instance that is returned by the identify operation:
     *
     * * true: only the [IdentifyLayerResult.popups] property will be populated with results.
     * If the layer does not have popups an error will be returned.
     *
     * * false: the [IdentifyLayerResult.geoElements] property will be populated.
     * The [IdentifyLayerResult.popups] property will be populated if the layer has popups.
     *
     * @param layer layer on which to run the identify
     * @param screenCoordinate location at which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the [IdentifyLayerResult.geoElements] property of the result is populated
     * @param maximumResults maximum size of the result set of GeoElements (element type dependent on target layer) to
     * return per layer or sublayer. -1 indicates unlimited results
     * @return A [Result] containing an [IdentifyLayerResult], or failure
     * @since 200.0.0
     */
    public suspend fun identify(
        layer: Layer,
        screenCoordinate: ScreenCoordinate,
        tolerance: Double,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int = 1
    ) : Result<IdentifyLayerResult> {
        return geoView?.identifyLayer(
            layer, screenCoordinate, tolerance, returnPopupsOnly, maximumResults
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }

    /**
     * Initiate an Identify operation on all layers in the view.
     *
     * Results are returned in top-to-bottom order.
     *
     * The [tolerance] parameter determines the extent of the region used during the identify operation. Any GeoElements
     * that intersect this test region when rendered are returned, up to the [maximumResults] limit. A tolerance of 0 tests just the
     * physical pixel at [screenCoordinate]. Tolerance values above 0 are in DIPs and specify a circular region centered on
     * [screenCoordinate], with radius equal to [tolerance]. The maximum allowed tolerance value is 100 DIPs, resulting in an
     * identify circle of diameter 200 DIPs. The [returnPopupsOnly] parameter controls what properties are populated in
     * the [IdentifyLayerResult] instances that are returned by the identify operation:
     * * true: each [IdentifyLayerResult.popups] property will be populated with results. Layers without
     * popups will be omitted.
     * * false: GeoElements will be populated. Layers with popups will also return popups.
     *
     * @param screenCoordinate location on which to run identify in screen coordinates
     * @param tolerance extent of the region used during the identify operation
     * @param returnPopupsOnly whether the [IdentifyLayerResult.geoElements] property of the results are populated
     * @param maximumResults maximum number of GeoElements to return per layer or sublayer. -1 indicates
     * unlimited results
     * @return A [Result] containing a [List] of [IdentifyLayerResult], containing one entry for each layer in the
     * view that supports identify, or failure. Each entry contains a [Layer] and a [List] of elements of the type
     * contained by the layer (e.g. [com.arcgismaps.data.Feature] for an [com.arcgismaps.mapping.layers.FeatureLayer])
     * @since 200.0.0
     */
    public suspend fun identifyLayers(
        screenCoordinate: ScreenCoordinate,
        tolerance: Double,
        returnPopupsOnly: Boolean = false,
        maximumResults: Int = 1
    ): Result<List<IdentifyLayerResult>> {
        return geoView?.identifyLayers(
            screenCoordinate, tolerance, returnPopupsOnly, maximumResults
        ) ?: Result.failure(IllegalStateException(nullGeoViewErrorMessage))
    }
}
