package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.coroutineScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

@Composable
public fun ArcGISMap(modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap,
    viewpoint: Viewpoint,
    onSingleTap: (mapPoint: Point?) -> Unit = {},
    content: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            map = arcGISMap
            lifecycle.addObserver(this)
            lifecycle.coroutineScope.launch {
                setViewpointAnimated(viewpoint)
                onSingleTapConfirmed.collect {
                    onSingleTap(it.mapPoint)
                }
            }
        }
    }
    Box(modifier = modifier.padding(bottom = 25.dp)) {
        AndroidView(modifier = modifier.fillMaxSize(),
            factory = { mapView })
        content()
    }
}
