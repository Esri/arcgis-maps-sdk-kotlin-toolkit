package com.arcgismaps.toolkit.geoviewcompose

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ReusableComposeNode

import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.ScreenCoordinate

internal class CalloutNode(
    val compositionContext: CompositionContext,
) : MapNode {
    override fun onAttached() {

    }

    override fun onRemoved() {
    }

    override fun onCleared() {
    }
}

@Composable
public fun Callout(location: Point,
                   content: @Composable () -> Unit) {
//    val mapView = (currentComposer.applier as? MapApplier)?.mapView
    val mapView = (currentComposer.applier as MapApplier).mapView
//    val location = Point(-117.9190, 33.8121, SpatialReference.wgs84())
    Log.d("location-> mapView", mapView.toString())
    Log.d("location->", mapView?.locationToScreen(location)?.x.toString())

    val calloutScreenCoordinate: ScreenCoordinate = mapView.locationToScreen(location)
    CalloutImpl(location = calloutScreenCoordinate, content = content)

}

@Composable
private fun CalloutImpl(location: ScreenCoordinate,
                        content: @Composable () -> Unit) {
    val mapView = (currentComposer.applier as MapApplier).mapView
    val compositionContext = rememberCompositionContext()
    ComposeNode<CalloutNode, MapApplier>(
        factory = { CalloutNode(compositionContext = compositionContext) },
        update = {  },
    ) {
        addCallout(calloutScreenCoordinate = location, content = content)
    }

//    ReusableComposeNode<CalloutNode, MapApplier>(
//         factory = { CalloutNode(compositionContext = compositionContext) },
//         update = {},
//        { addCallout(calloutScreenCoordinate = location, content = content) }
//    )
//    {
//        addCallout(calloutScreenCoordinate = location, content = content)
//    }
}

@Composable
private fun addCallout(calloutScreenCoordinate: ScreenCoordinate, content: @Composable () -> Unit) {
    Box(
//        modifier = Modifier.offset(x = calloutScreenCoordinate.x.dp, y = calloutScreenCoordinate.y.dp)
//        modifier = Modifier.offset(x = 186.dp, y = 50.dp)
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) {
                    calloutScreenCoordinate.x
                        .toFloat()
                        .toDp()
                },
                y = with(LocalDensity.current) {
                    calloutScreenCoordinate.y
                        .toFloat()
                        .toDp()
                })
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

