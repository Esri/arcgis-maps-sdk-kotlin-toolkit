package com.arcgismaps.toolkit.composablemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    println("sroth HIII")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val map by mapInterface.map.collectAsState()
    val insets by mapInterface.insets.collectAsState()
    val currentViewpoint by mapInterface.currentViewpoint.collectAsState()

    val mapView = remember {
        MapView(context).also {
            with(it) {
                with(coroutineScope) {
                    mapInterface.viewLogic()
                }
            }
        }
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
    
    Box(modifier = modifier.semantics {
        contentDescription = "MapContainer"
    }) {
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
}
