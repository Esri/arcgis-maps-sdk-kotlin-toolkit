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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import kotlinx.coroutines.flow.transformWhile

public class MapViewScope(internal val mapView: MapView) {

    @Composable
    public fun Callout(
        point: Point,
        content: @Composable MapViewScope.() -> Unit
    ) {

        val isMapViewReady = remember { mutableStateOf(false) }
        // We don't want to start drawing the Callout until the MapView is ready. We only collect
        // the drawStatus till the first time MapView is done drawing. the transformWhile operator
        // will stop collecting when isMapViewReady.value becomes false.
        LaunchedEffect(point) {
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

        var leaderScreenCoordinate: ScreenCoordinate by remember {
            mutableStateOf(mapView.locationToScreen(point))
        }

        LaunchedEffect(point) {
            leaderScreenCoordinate = mapView.locationToScreen(point)
            // Used to update screen coordinate when new location point is used
            // Used to update screen coordinate when viewpoint is changed
            mapView.viewpointChanged.collect {
                leaderScreenCoordinate = mapView.locationToScreen(point)
            }
        }

        val currentCoordinates =
            IntOffset(leaderScreenCoordinate.x.toInt(), leaderScreenCoordinate.y.toInt())
        var showPopup by remember {
            mutableStateOf(true)
        }

        var contentSize by remember { mutableStateOf(IntSize.Zero) }
        val popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                contentSize = popupContentSize
                return currentCoordinates.copy(
                    x = currentCoordinates.x - popupContentSize.width / 2,
                    y = currentCoordinates.y + anchorBounds.top - popupContentSize.height - 20
                )
            }
        }

        LaunchedEffect(currentCoordinates) {
                showPopup =
                    currentCoordinates.y > 20 && currentCoordinates.y < (mapView.height + 20 + contentSize.height)
        }

        if (showPopup) {
            val shouldClipBottom = currentCoordinates.y - mapView.height - 20 > 0
            val shouldClipTop = currentCoordinates.y < contentSize.height + 20

            val clipShape = object: Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline =
                    if (shouldClipBottom) {
                        val clipBottom = currentCoordinates.y - mapView.height - 20
                        val clipBottomSize = contentSize.height - clipBottom
                        val rectSize = IntSize(contentSize.width, clipBottomSize)
                        Outline.Rectangle(
                            Rect(
                                Offset.Zero,
                                rectSize.toSize()
                            )
                        )
                    } else if (shouldClipTop) {
                        val clipTopSize = currentCoordinates.y - 20
                        val clipTopOffset = (contentSize.height - currentCoordinates.y + 20).toFloat()
                        val rectSize = IntSize(
                            contentSize.width,
                            clipTopSize
                        )
                        Outline.Rectangle(
                            Rect(
                                Offset(0.0f, clipTopOffset),
                                rectSize.toSize()
                            )
                        )
                    } else {
                        Outline.Rectangle(
                            Rect(
                                Offset.Zero,
                                contentSize.toSize()
                            )
                        )
                    }
            }

            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent)
            ) {
                Popup(
                    popupPositionProvider,
                    properties = PopupProperties(clippingEnabled = false)
                ) {
                    PopupContent(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .clip(clipShape)

                    ) {
                        content()
                    }

                }
            }
        }
    }
}

@Composable
private fun PopupContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Max),
            content = content
        )
    }
}


