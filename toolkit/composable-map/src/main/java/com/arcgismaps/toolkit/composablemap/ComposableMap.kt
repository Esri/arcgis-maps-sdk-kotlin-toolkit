package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
public fun ArcGISMap(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap,
    viewpoint: Viewpoint,
    insets: MapInsets = MapInsets(),
    onSingleTap: (mapPoint: Point?) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context)
    }
    val mapSafeAreaPadding by remember {
        mutableStateOf(insets)
    }

    DisposableEffect(key1 = context, key2 = lifecycle, key3 = mapView) {
        lifecycle.addObserver(mapView)
        onDispose {
            lifecycle.removeObserver(mapView)
        }
    }

    Box(modifier = modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = {
                it.map = arcGISMap
                it.setViewInsets(left = mapSafeAreaPadding.start,
                    right = mapSafeAreaPadding.end,
                    top = mapSafeAreaPadding.top,
                    bottom = mapSafeAreaPadding.bottom
                )
            })
        Box(modifier = Modifier
                .fillMaxSize()
                .padding(start = mapSafeAreaPadding.start.dp,
                    end = mapSafeAreaPadding.end.dp,
                    top = mapSafeAreaPadding.top.dp,
                    bottom = mapSafeAreaPadding.bottom.dp
                )
        ) {
            content()
        }
    }

    LaunchedEffect(true) {
        lifecycle.coroutineScope.launch {
            mapView.setViewpointAnimated(viewpoint)
            launch {
                mapView.onSingleTapConfirmed.collect {
                    onSingleTap(it.mapPoint)
                }
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
