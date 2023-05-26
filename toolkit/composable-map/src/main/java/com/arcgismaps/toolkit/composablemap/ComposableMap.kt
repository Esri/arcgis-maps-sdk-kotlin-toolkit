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
    val mapView = MapView(context).also {
        with(it) {
            with(coroutineScope) {
                
                // call any MapView methods that do not affect the screen in this function.
                // e.g. identifyLayers, setBookmark, etc.
                mapInterface.viewLogic()
            }
        }
    }
    
    val mapState by mapInterface.mapData.collectAsState()
    
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
}
