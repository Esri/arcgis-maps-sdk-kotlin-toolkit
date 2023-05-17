package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

@Composable
public fun ComposableMap(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap,
    initialViewPoint: Viewpoint?,
    insets: MapInsets = MapInsets(),
    onSingleTap: (mapPoint: Point?) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = MapView(context)

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }

    Box(modifier = modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = {
                it.map = arcGISMap
                it.setViewInsets(
                    left = insets.start,
                    right = insets.end,
                    top = insets.top,
                    bottom = insets.bottom
                )
            })
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.start.dp,
                end = insets.end.dp,
                top = insets.top.dp,
                bottom = insets.bottom.dp
            )
        ) {
            content()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            initialViewPoint?.let {
                mapView.setViewpointAnimated(it)
            }
            mapView.onSingleTapConfirmed.collect {
                onSingleTap(it.mapPoint)
            }
        }
    }
}

public data class MapInsets(
    var start: Double = 0.0,
    var end: Double = 0.0,
    var top: Double = 0.0,
    var bottom: Double = 0.0
)
