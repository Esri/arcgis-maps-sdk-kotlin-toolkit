/*
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.geocompose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The default instance of [MapViewInteractionOptions]
 */
public val MapViewInteractionOptionDefaults: MapViewInteractionOptions = MapViewInteractionOptions()

/**
 * A compose equivalent of the [MapView].
 *
 * @param modifier Modifier to be applied to the composable MapView
 * @param arcGISMap the [ArcGISMap] to be rendered by this composable
 * @param locationDisplay the [LocationDisplay] used by the composable [com.arcgismaps.toolkit.geocompose.MapView]
 * @param mapViewInteractionOptions the [MapViewInteractionOptions] used by this composable [com.arcgismaps.toolkit.geocompose.MapView]
 * @param onViewpointChanged lambda invoked when the viewpoint of the composable MapView has changed
 * @param onUp lambda invoked when the user removes all their pointers from the composable MapView
 * @param onDown lambda invoked when the user first presses on the composable MapView
 * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable MapView
 * @param onDoubleTap lambda invoked the user double taps on the composable MapView
 * @param onLongPress lambda invoked when a user holds a pointer on the composable MapView
 * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable MapView
 * @param onPan lambda invoked when a user drags a pointer or pointers across composable MapView
 * @param overlay the composable overlays to display on top of the composable MapView. Example, a compass, floorfilter etc.
 * @since 200.3.0
 */
@Composable
public fun MapView(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap? = null,
    locationDisplay: LocationDisplay = rememberLocationDisplay(),
    mapViewInteractionOptions: MapViewInteractionOptions = MapViewInteractionOptionDefaults,
    onViewpointChanged: (() -> Unit)? = null,
    onUp: ((UpEvent) -> Unit)? = null,
    onDown: ((DownEvent) -> Unit)? = null,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)? = null,
    onDoubleTap: ((DoubleTapEvent) -> Unit)? = null,
    onLongPress: ((LongPressEvent) -> Unit)? = null,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)? = null,
    onPan: ((PanChangeEvent) -> Unit)? = null,
    overlay: @Composable () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    Box(modifier = Modifier.semantics {
        contentDescription = "MapViewContainer"
    }) {
        AndroidView(modifier = modifier
            .semantics {
                contentDescription = "MapView"
            },
            factory = { mapView },
            update = {
                it.map = arcGISMap
                it.interactionOptions = mapViewInteractionOptions
                it.locationDisplay = locationDisplay
            })

        overlay()
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
            mapView.onDestroy(lifecycleOwner)
        }
    }

    val currentViewPointChanged by rememberUpdatedState(onViewpointChanged)
    LaunchedEffect(Unit) {
        launch {
            mapView.viewpointChanged.collect {
                currentViewPointChanged?.let {
                    it()
                }
            }
        }
        setupCallbacksForGestureEvents(
            mapView,
            this,
            onUp,
            onDown,
            onSingleTapConfirmed,
            onDoubleTap,
            onLongPress,
            onTwoPointerTap,
            onPan
        )
    }
}

/**
 * Create and [remember] a [LocationDisplay].
 * Checks that [ArcGISEnvironment.applicationContext] is set and if not, sets one.
 * [init] will be called when the [LocationDisplay] is first created to configure its
 * initial state.
 *
 * @param key invalidates the remembered LocationDisplay if different from the previous composition
 * @param init called when the [LocationDisplay] is created to configure its initial state
 * @since 200.3.0
 */
@Composable
public inline fun rememberLocationDisplay(
    key: Any? = null,
    crossinline init: LocationDisplay.() -> Unit = {}
): LocationDisplay {
    if (ArcGISEnvironment.applicationContext == null) {
        ArcGISEnvironment.applicationContext = LocalContext.current
    }
    return remember(key) {
        LocationDisplay().apply(init)
    }
}

/**
 * Sets up the callbacks for all the Gesture Events.
 *
 * @since 200.3.0
 */
private fun setupCallbacksForGestureEvents(
    mapView: MapView,
    composeScope: CoroutineScope,
    onUp: ((UpEvent) -> Unit)?,
    onDown: ((DownEvent) -> Unit)?,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)?,
    onDoubleTap: ((DoubleTapEvent) -> Unit)?,
    onLongPress: ((LongPressEvent) -> Unit)?,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)?,
    onPan: ((PanChangeEvent) -> Unit)?
) {
    with(composeScope) {
        launch(Dispatchers.Main.immediate) {
            mapView.onUp.collect { upEvent ->
                onUp?.let {
                    it(upEvent)
                }
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onDown.collect { downEvent ->
                onDown?.let {
                    it(downEvent)
                }
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onSingleTapConfirmed.collect { singleTapConfirmedEvent ->
                onSingleTapConfirmed?.let {
                    it(singleTapConfirmedEvent)
                }
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onDoubleTap.collect { doubleTapEvent ->
                onDoubleTap?.let {
                    it(doubleTapEvent)
                }
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onLongPress.collect { longPressEvent ->
                onLongPress?.let {
                    it(longPressEvent)
                }
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onTwoPointerTap.collect { twoPointerTapEvent ->
                onTwoPointerTap?.let {
                    it(twoPointerTapEvent)
                }
            }
        }
        launch(Dispatchers.Main.immediate) {
            mapView.onPan.collect { panChangeEvent ->
                onPan?.let {
                    it(panChangeEvent)
                }
            }
        }
    }
}

@Preview
@Composable
internal fun MapViewPreview() {
    MapView()
}
