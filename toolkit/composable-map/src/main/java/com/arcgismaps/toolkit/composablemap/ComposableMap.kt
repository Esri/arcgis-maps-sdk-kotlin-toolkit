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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
public fun ComposableMap(
    mapInterface: MapInterface,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val flowProducer = rememberFlowProducer()

    val map by mapInterface.map.collectAsState()
    val insets by mapInterface.insets.collectAsState()

    val mapView = remember {
        MapView(context).also { view ->
            with(view) {
                with(coroutineScope) {
                    launch {
                        view.onDown.collect {
                            mapInterface.onDown(it)
                        }
                    }
                    launch {
                        view.onUp.collect {
                            mapInterface.onUp(it)
                        }
                    }
                    launch {
                        view.onSingleTapConfirmed.collect {
                            mapInterface.onSingleTapConfirmed(it)
                        }
                    }
                    launch {
                        view.onDoubleTap.collect {
                            mapInterface.onDoubleTap(it)
                        }
                    }
                    launch {
                        view.onLongPress.collect {
                            mapInterface.onLongPress(it)
                        }
                    }
                    launch {
                        view.onTwoPointerTap.collect {
                            mapInterface.onTwoPointerTap(it)
                        }
                    }
                    launch {
                        view.onPan.collect {
                            mapInterface.onPan(it)
                        }
                    }
                    launch {
                        view.mapRotation.collect {
                            mapInterface.onMapRotationChanged(it)
                        }
                    }
                    launch {
                        view.viewpointChanged.collect {
                            view.getCurrentViewpoint(ViewpointType.CenterAndScale)?.let {
                                mapInterface.onMapViewpointChanged(it)
                            }
                        }
                    }
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

    LaunchedEffect(Unit) {
        launch {
            mapInterface.mapRotation.collect {
                mapView.setViewpointRotation(it)
            }
        }
        launch {
            mapInterface.viewpoint.collect {
                it?.let {
                        mapView.setViewpoint(it)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }

    Box(modifier = modifier.semantics {
        contentDescription = "MapContainer"
    }) {
        AndroidView(modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "MapView"
            }, factory = { mapView })
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.start.dp,
                end = insets.end.dp,
                top = insets.top.dp,
                bottom = insets.bottom.dp
            )
            .semantics {
                contentDescription = "Content"
            }) {
            content()
        }
    }
}

@Composable
public fun rememberFlowProducer() : UUID = remember { UUID.randomUUID() }
