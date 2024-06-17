/*
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

import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Multipart
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.ProximityResult
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureRenderingMode
import com.arcgismaps.mapping.symbology.CompositeSymbol
import com.arcgismaps.mapping.symbology.MarkerSymbol
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.symbology.SymbolAngleAlignment
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsRenderingMode
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SceneLocationVisibility
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.zero
import kotlinx.coroutines.flow.transformWhile
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Creates a Callout at the specified geographical location on the MapView. The Callout is a composable
 * that can be used to display additional information about a location on the map. The additional information is
 * passed as a content composable that contains text and/or other content. It has a leader that points to
 * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
 * that contains the content lambda provided by the application. A thin border line is drawn around the entire Callout.
 *
 * Note: Only one Callout can be displayed at a time on the MapView.
 *
 * @param location the geographical location at which to display the Callout
 * @param modifier Modifier to be applied to the composable Callout
 * @param content the content of the Callout
 * @param offset the offset in screen coordinates from the geographical location at which to place the callout
 * @param rotateOffsetWithGeoView specifies whether the screen offset is rotated with the [GeoView]. The Screen offset
 *        will be rotated with the [GeoView] when true, false otherwise.
 *        This is useful if you are showing the callout for elements with symbology that does rotate with the [GeoView]
 * @since 200.5.0
 */
@Composable
public fun MapViewScope.Callout(
    location: Point,
    modifier: Modifier = Modifier,
    offset: Offset = Offset.Zero,
    rotateOffsetWithGeoView: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    if (this.calloutParams.location == null) {
        this.calloutParams = CalloutParams(
            location = location,
            modifier = modifier,
            offset = offset,
            rotateOffsetWithGeoView = rotateOffsetWithGeoView,
            content = content
        )
        this.Callout()
    }
}

/**
 * Creates a Callout at the specified [geoElement] or the [tapLocation] location on the MapView. The Callout is a composable
 * that can be used to display additional information about a location on the map. The additional information is
 * passed as a [content] composable that contains text and/or other content. It has a leader that points to
 * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
 * that contains the [content] lambda provided by the application. A thin border line is drawn around the entire Callout.
 *
 * Note: Only one Callout can be displayed at a time on the MapView.
 *
 * @param geoElement the GeoElement for which to display the Callout
 * @param modifier Modifier to be applied to the composable Callout
 * @param tapLocation a Point the user has tapped, or null if the Callout is not associated with a tap
 * @param content the content of the Callout
 * @since 200.5.0
 */
@Composable
public fun MapViewScope.Callout(
    geoElement: GeoElement,
    modifier: Modifier = Modifier,
    tapLocation: Point? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (this.calloutParams.location == null) {
        val leaderLocation = this.computeLeaderLocationForGeoelement(geoElement, tapLocation) ?: return
        this.calloutParams = CalloutParams(
            location = leaderLocation.location,
            modifier = modifier,
            offset = leaderLocation.offset,
            rotateOffsetWithGeoView = leaderLocation.rotateOffsetWithGeoView,
            content = content
        )
        this.Callout()
    }
}

/**
 * The receiver class of the MapView content lambda.
 *
 * @since 200.5.0
 */
public class MapViewScope(private var _mapView: MapView?) {

    internal var calloutParams: CalloutParams = CalloutParams()
    private val mapView: MapView
        get() = _mapView ?: error("MapView not initialized")

    internal fun reset() {
        calloutParams = CalloutParams()
    }

    /**
     * Creates a Callout at the specified geographical location on the MapView.
     *
     * @since 200.5.0
     */
    @Composable
    internal fun Callout() {

        val isMapViewReady = remember { mutableStateOf(false) }
        // We don't want to start drawing the Callout until the MapView is ready. We only collect
        // the drawStatus till the first time MapView is done drawing. the transformWhile operator
        // will stop collecting when isMapViewReady.value becomes false.
        LaunchedEffect(calloutParams.location) {
            mapView.drawStatus.transformWhile { drawStatus ->
                emit(drawStatus)
                !isMapViewReady.value
            }.collect {
                if (it == DrawStatus.Completed) {
                    isMapViewReady.value = true
                }
            }
        }

        if (!isMapViewReady.value) {
            return
        }

        // Convert the given location to a screen coordinate
        var leaderScreenCoordinate: ScreenCoordinate? by remember {
            mutableStateOf(
                getLeaderScreenCoordinate(mapView, calloutParams.location!!, calloutParams.offset, calloutParams.rotateOffsetWithGeoView)
            )
        }

        LaunchedEffect(calloutParams.location) {
            // Used to update screen coordinate when new location point is used
            leaderScreenCoordinate = getLeaderScreenCoordinate(mapView, calloutParams.location!!, calloutParams.offset, calloutParams.rotateOffsetWithGeoView)
            // Used to update screen coordinate when viewpoint is changed
            mapView.viewpointChanged.collect {
                leaderScreenCoordinate = getLeaderScreenCoordinate(mapView, calloutParams.location!!, calloutParams.offset, calloutParams.rotateOffsetWithGeoView)
            }
        }

        val localDensity = LocalDensity.current
        // Get the default shape, color & size properties for Callout
        val properties = CalloutProperties()
        leaderScreenCoordinate?.let {
            CalloutSubComposeLayout(
                leaderScreenCoordinate = it,
                maxSize = calloutContentMaxSize(
                    geoView = mapView,
                    density = LocalDensity.current,
                    displayMetrics = LocalContext.current.resources.displayMetrics
                )) {
                Box(
                    modifier = calloutParams.modifier!!
                        .drawCalloutContainer(
                            cornerRadius = with(localDensity) { properties.cornerRadius.toPx() },
                            strokeBorderWidth = with(localDensity) { properties.strokeBorderWidth.toPx() },
                            strokeColor = properties.strokeColor,
                            backgroundColor = properties.backgroundColor,
                            calloutContentPadding = properties.calloutContentPadding,
                            leaderWidth = with(localDensity) { properties.leaderSize.width.toPx() },
                            leaderHeight = with(localDensity) { properties.leaderSize.height.toPx() },
                            minSize = properties.minSize
                        )
                )
                {
                    calloutParams.content!!.invoke(this)
                }
            }
        }
    }

    /**
     * Returns the ScreenCoordinate for the location [Point] on [GeoView].
     *
     * @param geoView the GeoView
     * @param location the location in geographical coordinates
     * @param offset the offset in screen coordinates from the geographical location at which to place the Callout
     * @param rotateOffsetWithGeoView specifies whether the screen offset is rotated with the [GeoView]. The Screen offset
     *       will be rotated with the [GeoView] when true, false otherwise.
     * @return A [ScreenCoordinate] for the screen in pixels or null if the location is not visible
     * @since 200.5.0
     */
    private fun getLeaderScreenCoordinate(
        geoView: GeoView,
        location: Point,
        offset: Offset,
        rotateOffsetWithGeoView: Boolean
    ): ScreenCoordinate? {
        val geoViewRotation = geoView.rotation()
        val locationToScreen = when (geoView) {
            is MapView -> geoView.locationToScreen(location)
            is SceneView -> {
                val locationToScreenResult = geoView.locationToScreen(location)
                if (locationToScreenResult?.visibility == SceneLocationVisibility.Visible) {
                    locationToScreenResult.screenPoint
                }
                null
            }
        }
        return locationToScreen?.let { screenCoordinate ->
            if (rotateOffsetWithGeoView && geoViewRotation != 0.0) {
                val angle = AngularUnit.degrees.convertTo(AngularUnit.radians, -geoViewRotation)
                screenCoordinate.offset(offset).rotate(angle, screenCoordinate)
            } else {
                screenCoordinate.offset(offset)
            }
        }
    }

    /**
     * Analogue of Layout which allows to sub-compose the Callout container during the measuring stage,
     * and anchor the Callout leader at the given screenCoordinate. The Callout is drawn using a Box
     * and by default it is anchored to its top left corner. In order to anchor the Callout at the tip
     * of the leader we need to determine the size of its content to calculate the anchor point's
     * location before drawing the Callout on the screen.
     *
     * @param leaderScreenCoordinate Represents the x,y coordinate for the location on GeoView
     * @param maxSize The calculated maximum size of the callout container
     * @since 200.5.0
     */
    @Composable
    private fun CalloutSubComposeLayout(
        modifier: Modifier = Modifier,
        leaderScreenCoordinate: ScreenCoordinate,
        maxSize: DpSize,
        calloutContainer: @Composable () -> Unit
    ) {
        val configuration = LocalDensity.current
        val maxWidthInPx = with(configuration) {
            maxSize.width.roundToPx()
        }
        val maxHeightInPx = with(configuration) {
            maxSize.height.roundToPx()
        }

        SubcomposeLayout(modifier = modifier) { constraints ->
            // set the max width to the lesser of the available size or the maxWidth
            val layoutWidth = Integer.min(constraints.maxWidth, maxWidthInPx)
            // set the max height to the lesser of the available size or the maxHeight
            val layoutHeight = Integer.min(constraints.maxHeight, maxHeightInPx)
            // measure the content with the constraints
            val calloutContainerPlaceable = subcompose(slotId = 0) {
                calloutContainer()
            }[0].measure(
                constraints.copy(
                    maxWidth = layoutWidth,
                    maxHeight = layoutHeight
                )
            )
            // The default (0,0) value is on the top-left edge of the callout container.
            // This moves the anchor to the bottom-middle point using X,Y offsets,
            // and ensures that the leader's anchor point always represents the tapped location,
            // in this case the bottom-middle leader position.
            val calloutOffsetX =
                leaderScreenCoordinate.x.toInt() - (calloutContainerPlaceable.width / 2)
            val calloutOffsetY =
                leaderScreenCoordinate.y.toInt() - calloutContainerPlaceable.height
            // place the callout in the layout
            layout(layoutWidth, layoutHeight) {
                calloutContainerPlaceable.place(calloutOffsetX, calloutOffsetY)
            }
        }
    }

    /**
     * Calculates the appropriate placement of the Callout with the provided [GeoElement] and tap location.
     *
     * @param geoElement the geoElement for which to place the Callout
     * @param tapLocation the location user tapped at
     * @return the [LeaderLocation] of the Callout
     * @since 200.5.0
     */
    internal fun computeLeaderLocationForGeoelement(geoElement: GeoElement, tapLocation: Point?) : LeaderLocation? {
        val geometry = geoElement.geometry ?: return null
        return when (geoElement) {
            is Graphic -> computeCalloutLocationForGraphic(geoElement, tapLocation)
            is Feature -> computeCalloutLocationForFeature(geoElement, tapLocation)
            else -> LeaderLocation(
                location = tapLocation.calloutLocation(geometry),
                offset = Offset.Zero,
                rotateOffsetWithGeoView = false
            )
        }
    }

    /**
     * Calculates the appropriate placement of the Callout with the provided [Graphic] and tap location.
     * This method looks at the following properties of the graphic,
     *
     * * Geometry
     * * Symbology
     * * The overlay, it is a part of
     *
     * Along with that information and the tap location, it calculates the fitting placement
     * of the Callout.
     *
     * @param graphic the graphic for which to place the Callout
     * @param tapLocation the location user tapped at
     * @return the [LeaderLocation] of the Callout
     * @since 200.5.0
     */
    private fun computeCalloutLocationForGraphic(graphic: Graphic, tapLocation: Point?): LeaderLocation? {
        val geometry = graphic.geometry ?: return null

        val renderingMode = graphic.graphicsOverlay?.renderingMode ?: GraphicsRenderingMode.Dynamic
        val symbol = graphic.symbol ?: graphic.graphicsOverlay?.renderer?.getSymbol(graphic)

        val leaderPointOffset = LeaderPointOffset.create(
            geometry = geometry,
            symbol = symbol,
            isStaticRendering = renderingMode == GraphicsRenderingMode.Static
        )

        return LeaderLocation(
            location = tapLocation.calloutLocation(geometry),
            offset = leaderPointOffset.offset,
            rotateOffsetWithGeoView = leaderPointOffset.rotatesWithGeoView
        )
    }

    /**
     * Calculates the appropriate placement of the Callout with the provided [Feature] and
     * tap location.
     * This method looks at the following properties of the feature,
     *
     * * Geometry
     * * Symbology
     * * The overlay, it is a part of
     *
     * Along with that information and the tap location it calculates the fitting placement
     * of the Callout.
     *
     * @param feature the feature for which to place the Callout
     * @param tapLocation the location user tapped at
     * @return the [LeaderLocation] of the Callout
     * @since 200.5.0
     */
    private fun computeCalloutLocationForFeature(feature: Feature, tapLocation: Point?): LeaderLocation? {
        val geometry = feature.geometry ?: return null

        val layer = feature.featureTable?.layer as? FeatureLayer
        val isStaticRendering = (layer?.renderingMode == FeatureRenderingMode.Static)

        val symbol = layer?.renderer?.getSymbol(feature)

        val leaderPointOffset = LeaderPointOffset.create(
            geometry = geometry,
            symbol = symbol,
            isStaticRendering = isStaticRendering
        )

        return LeaderLocation(
            location = tapLocation.calloutLocation(geometry),
            offset = leaderPointOffset.offset,
            rotateOffsetWithGeoView = leaderPointOffset.rotatesWithGeoView
        )
    }

    /**
     * UI default properties for the [Callout] component.
     */
    private data class CalloutProperties(
        val cornerRadius: Dp = 10.dp,
        val strokeBorderWidth: Dp = 2.dp,
        val strokeColor: Color = Color.LightGray,
        val backgroundColor: Color = Color.White,
        val calloutContentPadding: PaddingValues = PaddingValues(
            all = cornerRadius + (strokeBorderWidth / 2)
        ),
        val leaderSize: DpSize = DpSize(
            width = 12.dp,
            height = 10.dp
        ),
        val minSize: DpSize = DpSize(
            width = strokeBorderWidth + (2 * cornerRadius),
            height = strokeBorderWidth + (2 * cornerRadius)
        )
    )
}

/**
 * Encapsulates properties used to display a Callout at a specific location on a [GeoView]
 *
 * @param location the geographical location that a Callout should be displayed at
 * @param offset the offset in screen coordinates from the geographical location in which to place the Callout
 * @param rotateOffsetWithGeoView whether the screen offset is rotated with the geo view.
 * This is useful if you are showing the Callout for elements with symbology that does not rotate with the [GeoView]
 * @since 200.5.0
 */
internal data class LeaderLocation (var location: Point, var offset: Offset = Offset.Zero, var rotateOffsetWithGeoView: Boolean = false)

/**
 * Encapsulates the screen offset for the Callout leader and if that offset should be rotated
 * with the [GeoView]
 *
 * @param offset the screen offset for the Callout leader
 * @param rotatesWithGeoView signifies whether the offset should be rotated with the [GeoView]
 * @since 200.5.0
 */
internal class LeaderPointOffset internal constructor(
    var offset: Offset = Offset.Zero,
    var rotatesWithGeoView: Boolean = false
) {
    companion object {
        /**
         * Creates an instance required for a [GeoElement] with a given geometry and symbol.
         *
         * @param geometry the geometry of the [GeoElement]
         * @param symbol the symbol used to render the [GeoElement]
         * @param isStaticRendering if static rendering is used
         * @return the [LeaderPointOffset] needed to place the Callout
         * @since 200.5.0
         */
        fun create(
            geometry: Geometry?,
            symbol: Symbol?,
            isStaticRendering: Boolean
        ): LeaderPointOffset {
            if (geometry !is Multipart) {
                val leaderPointOffsetForSymbol = when (symbol) {
                    is MarkerSymbol -> symbol.leaderPointOffset()
                    is CompositeSymbol -> symbol.leaderPointOffset()
                    else -> return LeaderPointOffset()
                }
                return LeaderPointOffset(
                    offset = leaderPointOffsetForSymbol.offset,
                    rotatesWithGeoView = leaderPointOffsetForSymbol.rotatesWithGeoView || isStaticRendering
                )
            } else {
                return LeaderPointOffset()
            }
        }
    }
}

@Immutable
internal data class CalloutParams(
    val location: Point? = null,
    val modifier: Modifier? = null,
    val offset: Offset = Offset.Zero,
    val rotateOffsetWithGeoView: Boolean = false,
    val content: (@Composable BoxScope.() -> Unit)? = null
)

private fun GeoView.rotation(): Double = when (this) {
    is SceneView -> getCurrentViewpoint(ViewpointType.CenterAndScale)?.rotation ?: 0.0
    is MapView -> mapRotation.value
}

/**
 * Returns a [DoubleXY] which is the result of offsetting this [DoubleXY] by the specified [Offset].
 *
 * @param offset the offset to be applied
 * @return the new [DoubleXY] offset point
 * @since 200.5.0
 */
private fun DoubleXY.offset(offset: Offset): DoubleXY {
    return DoubleXY(x + offset.x, y + offset.y)
}

/**
 * Returns a [DoubleXY] which is the result of rotating this [DoubleXY] by an angle around a center.
 *
 * @param rotateByAngle angle in Radians where a positive value is counter clockwise
 * @param center the center around which the resulting point will be rotated
 * @return the resulting [DoubleXY] that has been rotated
 * @since 200.5.0
 */
private fun DoubleXY.rotate(
    rotateByAngle: Double,
    center: DoubleXY = DoubleXY.zero
): DoubleXY {
    val x1 = x - center.x
    val y1 = y - center.y

    val x2 = x1 * cos(rotateByAngle) - y1 * sin(rotateByAngle)
    val y2 = x1 * sin(rotateByAngle) + y1 * cos(rotateByAngle)

    return DoubleXY(x2 + center.x, y2 + center.y)
}

/**
 * Returns the [LeaderPointOffset] for the [MarkerSymbol]
 *
 * @return the [LeaderPointOffset]
 * @since 200.5.0
 */
internal fun MarkerSymbol.leaderPointOffset(): LeaderPointOffset {
    var leaderOffset = DoubleXY(leaderOffsetX.toDouble(), leaderOffsetY.toDouble())
    if (angle != 0.0f) {
        leaderOffset = leaderOffset.rotate(AngularUnit.degrees.toRadians(angle.toDouble()))
    }
    leaderOffset = leaderOffset.offset(Offset(offsetX, offsetY))
    // we have to flip the y value (for consistency with offset coord system)
    leaderOffset = DoubleXY(leaderOffset.x, -leaderOffset.y)
    return LeaderPointOffset(
        offset = Offset(leaderOffset.x.toFloat(), leaderOffset.y.toFloat()),
        rotatesWithGeoView = angleAlignment == SymbolAngleAlignment.Map
    )
}

/**
 * Returns the [LeaderPointOffset] for the [CompositeSymbol]
 *
 * @return the [LeaderPointOffset]
 * @since 200.5.0
 */
internal fun CompositeSymbol.leaderPointOffset(): LeaderPointOffset {
    // return first MarkerSymbol to CalloutLeaderSupport
    return symbols.filterIsInstance<MarkerSymbol>().firstOrNull()?.leaderPointOffset() ?: LeaderPointOffset()
}

/**
 * The location that the Callout should be placed. Calculated by taking into account the
 * type of geometry and the tap location from a user interaction.
 *
 * @param geometry the geometry of the [GeoElement] that the Callout is being placed on
 * @return the geographic location of the Callout placement
 * @since 200.5.0
 */
private fun Point?.calloutLocation(geometry: Geometry): Point {
    if (geometry is Point) return geometry

    // if either tapLocation or its spatial reference is null, return the center of geometry's extent
    val spatialReference = this?.spatialReference ?: return geometry.extent.center

    val projectedGeometry =
        GeometryEngine.projectOrNull(geometry, spatialReference) ?: return this

    val normalizedGeometry =
        GeometryEngine.normalizeCentralMeridian(projectedGeometry) ?: return this

    val normalizedTap = GeometryEngine.normalizeCentralMeridian(this) as Point

    val proximity: ProximityResult? = if (normalizedGeometry is Multipoint) {
        GeometryEngine.nearestVertex(normalizedGeometry, normalizedTap)
    } else {
        GeometryEngine.nearestCoordinate(normalizedGeometry, normalizedTap)
    }

    return proximity?.coordinate ?: this
}

/**
 * Determines the max content size of the Callout container factoring in the
 * size of the GeoView and the Insets set on the GeoView.
 * @since 200.5.0
 */
private fun calloutContentMaxSize(
    geoView: GeoView,
    density: Density,
    displayMetrics: DisplayMetrics
): DpSize {
    // Start by getting height & width of GeoView
    var maxHeightForGeoView = geoView.height
    var maxWidthForGeoView = geoView.width
    if (maxHeightForGeoView == 0) {
        // Use height of display if view height not available yet (as happens once, before it is measured)
        maxHeightForGeoView = displayMetrics.heightPixels
    }
    if (maxWidthForGeoView == 0) {
        // Use width of display if view width not available yet (as happens once, before it is measured)
        maxWidthForGeoView = displayMetrics.widthPixels
    }
    // if we have valid insets set on the MapView, we deduct the maxHeightForGeoView & maxWidthForMapView by the specified inset sizes
    if (geoView is MapView && geoView.isViewInsetsValid) {
        maxHeightForGeoView -= with(density) { (geoView.viewInsetTop + geoView.viewInsetBottom).dp.toPx() }.roundToInt()
        maxWidthForGeoView -= with(density) { (geoView.viewInsetLeft + geoView.viewInsetRight).dp.toPx() }.roundToInt()
    }

    return DpSize(
        height = with(density) { maxHeightForGeoView.toDp() },
        width = with(density) { maxWidthForGeoView.toDp() }
    )
}

/**
 * Extension function to draw the Callout container using the given parameters.
 * It draws the shape, adds the content padding, adds padding for the leader height
 * and restricts it to the min size.
 *
 * @param cornerRadius The corner radius of the Callout shape in px.
 * @param strokeBorderWidth Width of the Callout stroke in px.
 * @param strokeColor Color used to define the outline stroke.
 * @param backgroundColor Color used to define the fill color of the Callout shape.
 * @param calloutContentPadding PaddingValues for the content placed inside the Callout.
 * @param leaderWidth Width of the Callout leader in px.
 * @param leaderHeight Height of the Callout leader in px.
 * @param minSize Minimum size the of the Callout shape.
 * @since 200.5.0
 */
@Composable
private fun Modifier.drawCalloutContainer(
    cornerRadius: Float,
    strokeBorderWidth: Float,
    strokeColor: Color,
    backgroundColor: Color,
    calloutContentPadding: PaddingValues,
    leaderWidth: Float,
    leaderHeight: Float,
    minSize: DpSize
) = then(
    sizeIn(minWidth = minSize.width, minHeight = minSize.height)
        // Set bottom padding to ensure the leader is visible
        .padding(bottom = with(LocalDensity.current) { leaderHeight.toDp() })
        .drawWithCache {
            onDrawBehind {
                // Define the Path of the callout
                val path = calloutPath(size, cornerRadius, leaderWidth, leaderHeight)
                // Fill the path's shape with the Callout's background color
                drawPath(
                    path = path,
                    color = backgroundColor,
                    style = Fill
                )
                // Outline the path's shape with the Callout's stroke color
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeBorderWidth)
                )
            }
        }
        .padding(calloutContentPadding)
)

/**
 * Create the Callout shape by returning the [Path] using the given parameters.
 *
 * @param size The calculated size of the resulting content used to created the Path.
 * @param cornerRadius The corner radius of the rectangle shape in px.
 * @param leaderWidth Width of the Callout leader in px.
 * @param leaderHeight Height of the Callout leader in px.
 * @since 200.5.0
 */
private fun calloutPath(
    size: Size,
    cornerRadius: Float,
    leaderWidth: Float,
    leaderHeight: Float
): Path {
    return Path().apply {
        reset()
        // Create a default rectangle using the given size
        val rect = Rect(left = 0f, top = 0f, right = size.width, bottom = size.height)
        // Move to the top-left corner of the shape
        moveTo(x = rect.left + cornerRadius, y = rect.top)
        // Draw a line from 0,0 to the top-right start of the corner
        lineTo(x = rect.right - cornerRadius, y = rect.top)
        // Create the top-right corner rectangle
        val topRightCorner = Rect(
            left = rect.right - 2 * cornerRadius,
            top = rect.top,
            right = rect.right,
            bottom = rect.top + 2 * cornerRadius
        )
        // Draw an arc representing the top-right corner
        arcTo(
            rect = topRightCorner,
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Draw a line from the end of the arc to the bottom-right start of the corner
        lineTo(
            x = rect.right,
            y = rect.bottom - cornerRadius
        )
        // Create the bottom-right corner rectangle
        val bottomRightCorner = Rect(
            left = rect.right - 2 * cornerRadius,
            top = rect.bottom - 2 * cornerRadius,
            right = rect.right,
            bottom = rect.bottom
        )
        // Draw an arc representing the bottom-right corner
        arcTo(
            rect = bottomRightCorner,
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Draw a line from the end of the arc to the start of the bottom leader
        lineTo(
            x = (size.width / 2) + (leaderWidth / 2),
            y = rect.bottom
        )
        // Draw a line from start of the leader bottom to the leader tip
        lineTo(
            x = (size.width / 2),
            y = rect.bottom + leaderHeight
        )
        // Draw a line from the leader tip to the bottom leader
        lineTo(
            x = (size.width / 2) - (leaderWidth / 2),
            y = rect.bottom
        )
        // Draw a line from the bottom leader to the start of the bottom-left corner
        lineTo(
            x = rect.left + cornerRadius,
            y = rect.bottom
        )
        // Create the bottom-left corner rectangle
        val bottomLeftCorner = Rect(
            left = rect.left,
            top = rect.bottom - 2 * cornerRadius,
            right = rect.left + 2 * cornerRadius,
            bottom = rect.bottom
        )
        // Draw an arc representing the bottom-left corner
        arcTo(
            rect = bottomLeftCorner,
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Draw a line from the end of the arc to the top-left start of the corner
        lineTo(
            x = rect.left,
            y = rect.top + cornerRadius
        )
        // Create the top-left corner rectangle
        val topLeftCorner = Rect(
            left = rect.left,
            top = rect.top,
            right = rect.left + 2 * cornerRadius,
            bottom = rect.top + 2 * cornerRadius
        )
        // Draw an arc representing the top-left corner
        arcTo(
            rect = topLeftCorner,
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Close the path to complete the shape
        close()
    }
}
