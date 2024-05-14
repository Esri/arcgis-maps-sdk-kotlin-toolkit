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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val isMapViewReady = remember { mutableStateOf(false) }
    LaunchedEffect(location) {
        launch {
            mapView.drawStatus.transformWhile { drawStatus ->
                emit(drawStatus)
                !isMapViewReady.value
            }.collect {
                if (it == DrawStatus.Completed) {
                    isMapViewReady.value = true
                }
            }
        }
    }

    if (isMapViewReady.value) {

        val localDensity = LocalDensity.current

        // SHAPE PROPERTIES
        val cornerRadius = 10.dp
        val anchorLeaderWidth = 10.dp
        val anchorLeaderHeight = 12.dp
        val strokeBorderWidth = 2.dp
        val stokeColor = Color.LightGray
        val calloutBackgroundColor = Color.White
        val calloutContentPadding = PaddingValues(cornerRadius + (strokeBorderWidth / 2))
        val minSize = DpSize(
            width = strokeBorderWidth + (2 * cornerRadius),
            height = strokeBorderWidth + (2 * cornerRadius)
        )

        // Convert the given location to a screen coordinate
        var calloutScreenCoordinate: ScreenCoordinate by remember {
            mutableStateOf(mapView.locationToScreen(location))
        }

        LaunchedEffect(location) {
            // Used to update screen coordinate when viewpoint is changed
            mapView.viewpointChanged.collect {
                calloutScreenCoordinate = mapView.locationToScreen(location)
            }
        }

        // Create a provider to calculate the offset for the current callout screen coordinate
        val popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                if (calloutScreenCoordinate.x.isNaN() || calloutScreenCoordinate.y.isNaN())
                    return IntOffset(
                        -9999,
                        -9999
                    ) // TODO: This is not ideal, find a way to only display this content when given Map/Scene has loaded.

                return IntOffset(
                    x = calloutScreenCoordinate.x.roundToInt() - (popupContentSize.width / 2),
                    y = calloutScreenCoordinate.y.roundToInt() - (popupContentSize.height / 2)
                            - (with(localDensity) { anchorLeaderHeight.toPx() }).roundToInt()
                )
            }
        }

        Popup(
            popupPositionProvider = popupPositionProvider,
            properties = PopupProperties(clippingEnabled = false),
            onDismissRequest = {
                // TODO: Callout function probably would need a dismiss param.
            }
        ) {
            Box(
                modifier = modifier
                    .drawCalloutShape(
                        cornerRadius = with(localDensity) { cornerRadius.toPx() },
                        anchorLeaderWidth = with(localDensity) { anchorLeaderWidth.toPx() },
                        anchorLeaderHeight = with(localDensity) { anchorLeaderHeight.toPx() },
                        strokeBorderWidth = with(localDensity) { strokeBorderWidth.toPx() },
                        minSize = minSize,
                        strokeColor = stokeColor,
                        calloutBackgroundColor = calloutBackgroundColor,
                        calloutContentPadding = calloutContentPadding,
                    )
            )
            {
                this.content()
            }
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
    calloutContentPadding: PaddingValues
) = then(
    sizeIn(minWidth = minSize.width, minHeight = minSize.height)
        .drawWithCache {
            onDrawBehind {
                // Define the Path of the callout
                val path = calloutPath(size, cornerRadius, anchorLeaderWidth, anchorLeaderHeight)
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

@Preview(showBackground = true)
@Composable
public fun CalloutPreview() {
    MaterialTheme {
        val localDensity = LocalDensity.current
        // SHAPE PROPERTIES
        val cornerRadius = 10.dp
        val anchorLeaderWidth = 10.dp
        val anchorLeaderHeight = 12.dp
        val strokeBorderWidth = 2.dp
        val stokeColor = Color.LightGray
        val calloutBackgroundColor = Color.White
        val calloutContentPadding = PaddingValues(cornerRadius + (strokeBorderWidth / 2))
        val minSize = DpSize(
            width = strokeBorderWidth + (2 * cornerRadius),
            height = strokeBorderWidth + (2 * cornerRadius)
        )

        Box(
            modifier = Modifier
                .drawCalloutShape(
                    cornerRadius = with(localDensity) { cornerRadius.toPx() },
                    anchorLeaderWidth = with(localDensity) { anchorLeaderWidth.toPx() },
                    anchorLeaderHeight = with(localDensity) { anchorLeaderHeight.toPx() },
                    strokeBorderWidth = with(localDensity) { strokeBorderWidth.toPx() },
                    minSize = minSize,
                    strokeColor = stokeColor,
                    calloutBackgroundColor = calloutBackgroundColor,
                    calloutContentPadding = calloutContentPadding,
                )
        )
        {
            Text(text = "Hello World")
        }
    }
}
