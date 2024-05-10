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

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SceneLocationVisibility
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.zero
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
 * @since 200.5.0
 */
@Composable
public fun MapViewScope.Callout(location: Point,
                                modifier: Modifier = Modifier,
                                offset: DoubleXY = DoubleXY.zero,
//                                offset: DpOffset = DpOffset(0.dp, 0.dp),
                                rotateOffsetWithGeoView: Boolean = false,
                                content: @Composable BoxScope.() -> Unit) {

    if (mapView.map?.loadStatus?.collectAsState()?.value == LoadStatus.Loaded) {

//        val mapViewRotation = mapView.mapRotation.collectAsState().value
//        Log.d("***Callout test", "mapViewRotation: $mapViewRotation")

        val leaderLocation = getLeaderScreenCoordinate(
            mapView,
            location,
            offset,
            rotateOffsetWithGeoView,
//            mapViewRotation
        )
        Log.d("***Callout test", "leaderLocation: $leaderLocation")
//        val calloutScreenCoordinate: ScreenCoordinate = mapView.locationToScreen(location)
        leaderLocation?.let {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = leaderLocation.x.toFloat()
                        translationY = leaderLocation.y.toFloat()
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
    }
}


    /**
     * Returns the ScreenCoordinate for the location [Point] on [GeoView].
     *
     * @param geoView the GeoView
     * @return A [ScreenCoordinate] for the screen in pixels or null if the location is not visible
     * @since 200.2.0
     */
    private fun getLeaderScreenCoordinate(
        geoView: GeoView,
        location: Point,
        offset: DoubleXY = DoubleXY.zero,
        rotateOffsetWithGeoView: Boolean = false,
//        geoViewRotation: Double = 0.0
    ): ScreenCoordinate? {
        val geoViewRotation = geoView.rotation()
        val locationToScreen = if (geoView is MapView) {
             geoView.locationToScreen(location)
        } else {
            // geoView is a SceneView
            val locationToScreenResult = (geoView as SceneView).locationToScreen(location)
            if (locationToScreenResult?.visibility == SceneLocationVisibility.Visible) {
                return locationToScreenResult.screenPoint
            }
            null
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

private fun GeoView.rotation(): Double {
    return if (this is SceneView) {
        getCurrentViewpoint(ViewpointType.CenterAndScale)?.rotation ?: 0.0
    } else {
        val mapViewRotation = (this as MapView).mapRotation.value
        Log.d("***Callout test", "mapViewRotation: $mapViewRotation")
        mapViewRotation
//        (this as MapView).mapRotation.value
    }
}

/**
 * Returns a [DoubleXY] which is the result of offsetting this [DoubleXY] by the specified value.
 *
 * @param offset the offset to he applied
 * @return the new [DoubleXY] offset point
 * @since 200.2.0
 */
private fun DoubleXY.offset(offset: DoubleXY): DoubleXY {
    return this + offset
}

/**
 * Returns a [DoubleXY] which is the result of rotating this [DoubleXY] by an angle around a center.
 *
 * @param rotateByAngle angle in Radians where a positive value is counter clockwise
 * @param center the center around which the resulting point will be rotated
 * @return the resulting [DoubleXY] that has been rotated
 * @since 200.2.0
 */
private fun DoubleXY.rotate(rotateByAngle: Double, center: DoubleXY = DoubleXY.zero): DoubleXY {
    val x1 = x - center.x
    val y1 = y - center.y

    val x2 = x1 * cos(rotateByAngle) - y1 * sin(rotateByAngle)
    val y2 = x1 * sin(rotateByAngle) + y1 * cos(rotateByAngle)

    return DoubleXY(x2 + center.x, y2 + center.y)
}
