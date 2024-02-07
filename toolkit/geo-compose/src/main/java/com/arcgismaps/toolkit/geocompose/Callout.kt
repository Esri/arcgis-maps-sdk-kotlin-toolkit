package com.arcgismaps.toolkit.geocompose

import android.util.Log
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.zero
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.MapView

/*
 * COPYRIGHT 1995-2024 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

//public data class Callout(
//    public val location: Point,
//    public val offset: DoubleXY = DoubleXY.zero,
//    public val rotateOffsetWithGeoView: Boolean = false,
//    public val animated: Boolean = false,
//    public var content: (@Composable () -> Unit)? = null,
//)

@Composable
public fun Callout(
    location: Point,
    content: @Composable () -> Unit
) {
    if (mapView!!.spatialReference.collectAsState().value == null) return
    Log.d("location->", "outer")
    CompositionLocalProvider(LocalMapView provides mapView) {

        val localMapView = LocalMapView.current
        Log.d("location->", localMapView?.locationToScreen(location)?.x.toString())

        val calloutScreenCoordinate: ScreenCoordinate = localMapView!!.locationToScreen(location)
        Box(
//        modifier = Modifier.offset(x = calloutScreenCoordinate.x.dp, y = calloutScreenCoordinate.y.dp)
//        modifier = Modifier.offset(x = 186.dp, y = 50.dp)
            modifier = Modifier.offset(
                x = with(LocalDensity.current) { calloutScreenCoordinate.x.toFloat().toDp() },
                y = with(LocalDensity.current) { calloutScreenCoordinate.y.toFloat().toDp() })
                .wrapContentSize()
//          .padding(30.dp)
                .background(Color.White)
                .border(
                    border = BorderStroke(2.dp, Color.LightGray),
                    shape = MaterialTheme.shapes.medium
                )
        )
        {
            content.invoke()
        }
    }
}
