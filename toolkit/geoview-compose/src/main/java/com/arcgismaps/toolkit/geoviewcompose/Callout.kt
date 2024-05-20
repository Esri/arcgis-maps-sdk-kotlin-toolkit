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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SceneLocationVisibility
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.zero
import kotlinx.coroutines.flow.transformWhile
import kotlin.math.cos
import kotlin.math.sin

/**
 * The receiver class of the MapView content lambda.
 *
 * @since 200.5.0
 */
public class MapViewScope(internal val mapView: MapView)

/**
 * Creates a Callout at the specified geographical location on the MapView. The Callout is a composable
 * that can be used to display additional information about a location on the map. The additional information is
 * passed as a content composable that contains text and/or other content. It has a leader that points to
 * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
 * that contains the content lambda provided by the application. A thin border line is drawn around the entire Callout.
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

    val isMapViewReady = remember { mutableStateOf(false) }
    // We don't want to start drawing the Callout until the MapView is ready. We only collect
    // the drawStatus till the first time MapView is done drawing. the transformWhile operator
    // will stop collecting when isMapViewReady.value becomes false.
    LaunchedEffect(location) {
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

    val leaderScreenCoordinate = getLeaderScreenCoordinate(
        mapView,
        location,
        offset,
        rotateOffsetWithGeoView,
    )
    leaderScreenCoordinate?.let {

        val localDensity = LocalDensity.current
        // Get the default shape, color & size properties for Callout
        val properties = CalloutProperties()

        SubComposableLayout(
            screenCoordinate = leaderScreenCoordinate
        ) {
            Box(
                modifier = modifier
                    .drawCalloutContainer(
                        cornerRadius = with(localDensity) { properties.cornerRadius.toPx() },
                        strokeBorderWidth = with(localDensity) { properties.strokeBorderWidth.toPx() },
                        strokeColor = properties.strokeColor,
                        backgroundColor = properties.backgroundColor,
                        calloutContentPadding = properties.calloutContentPadding,
                        leaderWidth = with(localDensity) { properties.leaderSize.width.toPx() },
                        leaderHeight = with(localDensity) { properties.leaderSize.height.toPx() },
                        minSize = properties.minSize,
                        calloutScreenCoordinate = leaderScreenCoordinate,
                    )
            )
            {
                this.content()
            }
        }
    }
}

/**
 * Returns the ScreenCoordinate for the location [Point] on [GeoView].
 *
 * @param geoView the GeoView
 * @param location the location in geographical coordinates
 * @param offset the offset in screen coordinates from the geographical location at which to place the callout
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
private fun DoubleXY.rotate(rotateByAngle: Double, center: DoubleXY = DoubleXY.zero): DoubleXY {
    val x1 = x - center.x
    val y1 = y - center.y

    val x2 = x1 * cos(rotateByAngle) - y1 * sin(rotateByAngle)
    val y2 = x1 * sin(rotateByAngle) + y1 * cos(rotateByAngle)

    return DoubleXY(x2 + center.x, y2 + center.y)
}

/**
 * Extension function to draw the Callout container using the given parameters. It draws the shape, adds the content padding, adds padding for the leader height, restricting the min size and positions it on the screen.
 *
 * @param cornerRadius The corner radius of the Callout shape in px.
 * @param strokeBorderWidth Width of the Callout stroke in px.
 * @param strokeColor Color used to define the outline stroke.
 * @param backgroundColor Color used to define the fill color of the Callout shape.
 * @param calloutContentPadding PaddingValues for the content placed inside the Callout.
 * @param leaderWidth Width of the Callout leader in px.
 * @param leaderHeight Height of the Callout leader in px.
 * @param minSize Minimum size the of the Callout shape.
 * @param calloutScreenCoordinate Represents the x,y coordinate of the Callout leader.
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
    minSize: DpSize,
    calloutScreenCoordinate: ScreenCoordinate,
) = then(
    sizeIn(minWidth = minSize.width, minHeight = minSize.height)
        // Set bottom padding to ensure the leader is visible
        .padding(bottom = with(LocalDensity.current) { leaderHeight.toDp() })
        .graphicsLayer {
            translationX = calloutScreenCoordinate.x.toFloat()
            translationY = calloutScreenCoordinate.y.toFloat()
        }
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

@Composable
private fun SubComposableLayout(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Constraints.Infinity.dp,
    maxHeight: Dp = Constraints.Infinity.dp,
    screenCoordinate: ScreenCoordinate,
    content: @Composable () -> Unit
) {
    val configuration = LocalDensity.current
    val maxWidthInPx = with(configuration) {
        maxWidth.roundToPx()
    }
    val maxHeightInPx = with(configuration) {
        maxHeight.roundToPx()
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        // set the max width to the lesser of the available size or the maxWidth
        val layoutWidth = Integer.min(constraints.maxWidth, maxWidthInPx)
        // set the max height to the lesser of the available size or the maxHeight
        val layoutHeight = Integer.min(constraints.maxHeight, maxHeightInPx)
        // measure the content with the constraints
        val contentPlaceable = subcompose(0) {
            content()
        }[0].measure(
            constraints.copy(
                maxWidth = layoutWidth,
                maxHeight = layoutHeight
            )
        )
        // calculate the callout position
        val calloutOffsetX = screenCoordinate.x.toInt() - (contentPlaceable.width / 2)
        val calloutOffsetY = screenCoordinate.y.toInt() - contentPlaceable.height
        // place the callout in the layout
        layout(layoutWidth, layoutHeight) {
            contentPlaceable.place(calloutOffsetX, calloutOffsetY)
        }
    }
}

/**
 * Extension function to draw the Callout shape using the given shape options.
 *
 * [cornerRadius] The corner radius of the rectangle shape in px.
 * [anchorLeaderWidth] Width of the anchor leader in px.
 * [anchorLeaderHeight] Height of the anchor leader in px.
 * [strokeBorderWidth] Width of the Callout stroke in px.
 * [strokeColor] Color used to define the outline stroke.
 * [calloutBackgroundColor] Color used to define the fill color of the Callout shape.
 * [minSize] Minimum size the of the Callout shape.
 * @since 200.5.0
 */
@Composable
private fun Modifier.drawCalloutShape(
    cornerRadius: Float,
    anchorLeaderWidth: Float,
    anchorLeaderHeight: Float,
    strokeBorderWidth: Float,
    strokeColor: Color,
    calloutBackgroundColor: Color,
    minSize: DpSize,
    calloutContentPadding: PaddingValues,
): Modifier {

    val localDensity = LocalDensity.current
    return this.then(
        sizeIn(minWidth = minSize.width, minHeight = minSize.height)
            .padding(bottom = with(localDensity) { anchorLeaderHeight.toDp() })
            .drawWithCache {
                onDrawBehind {
                    // Define the Path of the callout
                    val path = calloutPath(
                        size = size,
                        cornerRadius = cornerRadius,
                        anchorLeaderWidth = anchorLeaderWidth,
                        anchorLeaderHeight = anchorLeaderHeight
                    )
                    // Fill the path's shape with the Callout's background color
                    drawPath(
                        path = path,
                        color = calloutBackgroundColor,
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
}

/**
 * Create the Callout shape by returning the [Path] using the given parameters.
 *
 * [size] The calculated size of the resulting content used to created the Path.
 * [cornerRadius] The corner radius of the rectangle shape in px.
 * [anchorLeaderWidth] Width of the anchor leader in px.
 * [anchorLeaderHeight] Height of the anchor leader in px.
 * @since 200.5.0
 */
private fun calloutPath(
    size: Size,
    cornerRadius: Float,
    anchorLeaderWidth: Float,
    anchorLeaderHeight: Float
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
            x = (size.width / 2) + (anchorLeaderWidth / 2),
            y = rect.bottom
        )
        // Draw a line from start of the leader bottom to the leader tip
        lineTo(
            x = (size.width / 2),
            y = rect.bottom + anchorLeaderHeight
        )
        // Draw a line from the leader tip to the bottom leader
        lineTo(
            x = (size.width / 2) - (anchorLeaderWidth / 2),
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
