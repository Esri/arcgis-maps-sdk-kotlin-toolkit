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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import kotlinx.coroutines.flow.transformWhile

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

    // Convert the given location to a screen coordinate
    var calloutScreenCoordinate: ScreenCoordinate by remember {
        mutableStateOf(mapView.locationToScreen(location))
    }

    LaunchedEffect(location) {
        // Used to update screen coordinate when new location point is used
        calloutScreenCoordinate = mapView.locationToScreen(location)
        // Used to update screen coordinate when viewpoint is changed
        mapView.viewpointChanged.collect {
            calloutScreenCoordinate = mapView.locationToScreen(location)
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = calloutScreenCoordinate.x.toFloat()
                translationY = calloutScreenCoordinate.y.toFloat()
            }
            .wrapContentSize()
            .background(Color.White)
            .border(
                border = BorderStroke(2.dp, Color.LightGray),
                shape = MaterialTheme.shapes.medium
            )
    )
    {
        this.content()
    }
}
