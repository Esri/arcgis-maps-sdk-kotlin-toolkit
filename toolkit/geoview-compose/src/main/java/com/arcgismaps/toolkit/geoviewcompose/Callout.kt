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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate

/**
 * Receiver scope which is used by MapView and SceneView composable functions.
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
 * @param location the geographical location at which to display the fCallout
 * @param modifier Modifier to be applied to the composable Callout
 * @param content the content of the Callout
 * @since 200.5.0
 */
@Composable
public fun MapViewScope.Callout(
    location: Point,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val calloutScreenCoordinate: ScreenCoordinate = mapView.locationToScreen(location)

    // SHAPE PROPERTIES
    val cornerRadius = with(LocalDensity.current) { 10.dp.toPx() }
    val anchorLeaderWidth = with(LocalDensity.current) { 10.dp.toPx() }
    val anchorLeaderHeight = with(LocalDensity.current) { 12.dp.toPx() }
    val strokeBorderWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val stokeColor = Color.LightGray
    val calloutBackgroundColor = Color.White
    val anchorAlignment = AnchorAlignment.MIDDLE
    val minSize = Size(
        width = strokeBorderWidth + (2 * cornerRadius),
        height = strokeBorderWidth + (2 * cornerRadius)
    )

    Box(
        modifier = modifier
            .drawCalloutShape(
                cornerRadius = cornerRadius,
                anchorLeaderWidth = anchorLeaderWidth,
                anchorLeaderHeight = anchorLeaderHeight,
                strokeBorderWidth = strokeBorderWidth,
                minSize = minSize,
                strokeColor = stokeColor,
                calloutBackgroundColor = calloutBackgroundColor,
                anchorAlignment = anchorAlignment,
                calloutScreenCoordinate = calloutScreenCoordinate
            )
    )
    {
        this.content()
    }
}

/**
 * Extension function to draw the Callout shape using the given shape options.
 */
@Composable
private fun Modifier.drawCalloutShape(
    cornerRadius: Float,
    anchorLeaderWidth: Float,
    anchorLeaderHeight: Float,
    strokeBorderWidth: Float,
    strokeColor: Color,
    calloutBackgroundColor: Color,
    anchorAlignment: AnchorAlignment,
    minSize: Size,
    calloutScreenCoordinate: ScreenCoordinate
) = then(
    sizeIn(minWidth = minSize.width.dp, minHeight = minSize.height.dp)
        // Set bottom padding to ensure the leader is visible
        .padding(bottom = with(LocalDensity.current) { anchorLeaderHeight.toDp() })
        .graphicsLayer {
            translationX = calloutScreenCoordinate.x.toFloat()
            translationY = calloutScreenCoordinate.y.toFloat()
        }
        .drawBehind {
            val path = calloutPath(
                size, cornerRadius, anchorLeaderWidth, anchorLeaderHeight, anchorAlignment
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
)

/**
 * Create the Callout shape by returning the [Path] using the given parameters.
 */
private fun calloutPath(
    size: Size,
    cornerRadius: Float,
    anchorLeaderWidth: Float,
    anchorLeaderHeight: Float,
    anchorAlignment: AnchorAlignment
): Path {
    return Path().apply {
        reset()
        val rect = Rect(left = 0f, top = 0f, right = size.width, bottom = size.height)

        val anchorPosition = when (anchorAlignment) {
            AnchorAlignment.START -> cornerRadius + (anchorLeaderWidth / 2)
            AnchorAlignment.MIDDLE -> (size.width / 2)
            AnchorAlignment.END -> size.width - cornerRadius - (anchorLeaderWidth / 2)
        }

        // Move to the top-left corner of the shape
        moveTo(x = rect.left + cornerRadius, y = rect.top)
        // Draw a line to the top-right corner of the shape
        lineTo(x = rect.right - cornerRadius, y = rect.top)
        val topRightCorner = Rect(
            left = rect.right - 2 * cornerRadius,
            top = rect.top,
            right = rect.right,
            bottom = rect.top + 2 * cornerRadius
        )
        // Draw an arc from the top-right corner to the bottom-right corner
        arcTo(
            rect = topRightCorner,
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Draw a line to the bottom-right corner of the shape
        lineTo(
            x = rect.right,
            y = rect.bottom - cornerRadius
        )
        val bottomRightCorner = Rect(
            left = rect.right - 2 * cornerRadius,
            top = rect.bottom - 2 * cornerRadius,
            right = rect.right,
            bottom = rect.bottom
        )
        // Draw an arc from the bottom-right corner to the bottom-left corner
        arcTo(
            rect = bottomRightCorner,
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Draw a line from rect to the bottom anchor leader
        lineTo(
            x = (anchorPosition) + (anchorLeaderWidth / 2),
            y = rect.bottom
        )
        // Tip of the anchor leader
        lineTo(
            x = (anchorPosition),
            y = rect.bottom + anchorLeaderHeight
        )
        // Draw a line from the bottom of anchor leader to rect
        lineTo(
            x = (anchorPosition) - (anchorLeaderWidth / 2),
            y = rect.bottom
        )
        // Draw a line to the bottom left corner
        lineTo(
            x = rect.left + cornerRadius,
            y = rect.bottom
        )
        val bottomLeftCorner = Rect(
            left = rect.left,
            top = rect.bottom - 2 * cornerRadius,
            right = rect.left + 2 * cornerRadius,
            bottom = rect.bottom
        )
        // Draw an arc from the bottom-left corner to the top-left corner
        arcTo(
            rect = bottomLeftCorner,
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Draw a line to the top-left corner of the shape
        lineTo(
            x = rect.left,
            y = rect.top + cornerRadius
        )
        val topLeftCorner = Rect(
            left = rect.left,
            top = rect.top,
            right = rect.left + 2 * cornerRadius,
            bottom = rect.top + 2 * cornerRadius
        )
        // Draw an arc from the top-left corner to the top-right corner
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

private enum class AnchorAlignment { START, MIDDLE, END }
