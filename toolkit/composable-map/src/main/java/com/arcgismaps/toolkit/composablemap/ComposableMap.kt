package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.view.MapView

@Composable
public fun ComposableMap(
    modifier: Modifier = Modifier,
    mapInterface: MapInterface,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = MapView(context)
    val mapState by mapInterface.mapData.collectAsState()

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }

    Box(modifier = modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { mapView ->
                mapView.map = mapState.map
                mapView.setViewInsets(
                    left = mapState.insets.start,
                    right = mapState.insets.end,
                    top = mapState.insets.top,
                    bottom = mapState.insets.bottom
                )
                mapState.viewPoint?.let {
                    mapView.setViewpoint(it)
                }
            })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = mapState.insets.start.dp,
                    end = mapState.insets.end.dp,
                    top = mapState.insets.top.dp,
                    bottom = mapState.insets.bottom.dp
                )
        ) {
            content()
        }
    }

    LaunchedEffect(Unit) {
        with (mapView) {
            with (this) {
                // call any MapView methods that do not affect the screen in this function.
                // e.g. identifyLayers, setBookmark, etc.
                mapInterface.viewLogic()
            }
        }
    }
}
