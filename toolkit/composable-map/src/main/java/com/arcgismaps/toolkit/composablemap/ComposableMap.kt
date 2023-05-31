package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

@Composable
public fun ComposableMap(
    modifier: Modifier = Modifier,
    mapInterface: MapInterface,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val map by mapInterface.map.collectAsState()
    val insets by mapInterface.insets.collectAsState()
    val currentViewpoint by mapInterface.currentViewpoint.collectAsState()

    val mapView = remember {
        MapView(context)
    }
    mapView.map = map
    mapView.setViewInsets(
        left = insets.start,
        right = insets.end,
        top = insets.top,
        bottom = insets.bottom
    )
    currentViewpoint?.let { mapView.setViewpoint(it) }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }

    Box(modifier = modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(),
            factory = { mapView })
        Box(
            modifier = Modifier
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
            mapView.onSingleTapConfirmed.collect {
                mapInterface.onSingleTapConfirmed(it)
            }
        }
    }
}
