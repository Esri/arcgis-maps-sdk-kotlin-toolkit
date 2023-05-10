package com.arcgismaps.toolkit.exampleapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.coroutineScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

// could also do slot based layout for stuff like widgets on top of map
// see https://developer.android.com/jetpack/compose/layouts/basics#slot-based-layouts
@Composable
fun ArcGISMapView(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap,
    viewpoint: Viewpoint,
    onSingleTap: (mapPoint: Point?) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    Box(modifier = modifier) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    lifecycle.addObserver(this)
                    lifecycle.coroutineScope.launch {
                        onSingleTapConfirmed.collect {
                            onSingleTap(it.mapPoint)
                        }
                    }
                }
            },
            update = { mapView ->
                mapView.map = arcGISMap
                lifecycle.coroutineScope.launch {
                    mapView.setViewpointAnimated(viewpoint)
                }
            })
        content()
    }
}

@Composable
fun ArcGISMapScaffoldView(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap,
    viewpoint: Viewpoint,
    onSingleTap: (mapPoint: Point?) -> Unit = {},
    bottomRightActions: @Composable () -> Unit = {},
    bottomLeftActions: @Composable () -> Unit = {},
    topRightActions: @Composable () -> Unit = {},
    topLeftActions: @Composable () -> Unit = {},
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    Box(modifier = modifier) {
        AndroidView(modifier = modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    lifecycle.addObserver(this)
                    lifecycle.coroutineScope.launch {
                        onSingleTapConfirmed.collect {
                            onSingleTap(it.mapPoint)
                        }
                    }
                }
            },
            update = { mapView ->
                mapView.map = arcGISMap
                lifecycle.coroutineScope.launch {
                    mapView.setViewpointAnimated(viewpoint)
                }
            })
        Row(modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            Column() {
                topLeftActions()
            }
            Column() {
                topRightActions()
            }
        }
        Row(modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom) {
            Column() {
                bottomLeftActions()
            }
            Column() {
                bottomRightActions()
            }
        }
    }
}