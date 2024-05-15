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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate

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
 * @since 200.5.0
 */
@Composable
public fun MapViewScope.Callout(
    location: Point,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    val localDensity = LocalDensity.current

    if (mapView.map?.loadStatus?.collectAsState()?.value == LoadStatus.Loaded) {
        val calloutScreenCoordinate: ScreenCoordinate = mapView.locationToScreen(location)

        // Get the default shape, color & size properties for Callout
        val properties = CalloutProperties()

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
                    calloutScreenCoordinate = calloutScreenCoordinate,
                )
        )
        {
            this.content()
        }
    }
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

/**
 * UI default properties for the [Callout] component.
 */
internal data class CalloutProperties(
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
