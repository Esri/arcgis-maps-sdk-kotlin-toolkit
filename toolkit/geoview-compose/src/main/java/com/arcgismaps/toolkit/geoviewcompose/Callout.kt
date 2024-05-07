package com.arcgismaps.toolkit.geoviewcompose

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
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.MapView


public class MapViewScope(internal val mapView: MapView)

@Composable
public fun MapViewScope.Callout(location: Point,
                   content: @Composable () -> Unit) {
//    if (mapView!!.spatialReference.collectAsState().value == null) return
    if (mapView.drawStatus?.collectAsState()?.value == DrawStatus.InProgress) return
    Log.d("location->", "outer")
        Log.d("location->", mapView.locationToScreen(location).x.toString())

        val calloutScreenCoordinate: ScreenCoordinate = mapView.locationToScreen(location)
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