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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.mapping.view.WrapAroundMode
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * A compose equivalent of the [MapView].
 *
 * @param modifier Modifier to be applied to the composable MapView
 * @param arcGISMap the [ArcGISMap] to be rendered by this composable
 * @param locationDisplay the [LocationDisplay] used by the composable [com.arcgismaps.toolkit.geocompose.MapView]
 * @param geometryEditor the [GeometryEditor] used by the composable [com.arcgismaps.toolkit.geocompose.MapView] to create and edit geometries by user interaction.
 * @param mapViewInteractionOptions the [MapViewInteractionOptions] used by this composable [com.arcgismaps.toolkit.geocompose.MapView]
 * @param wrapAroundMode the [WrapAroundMode] to specify whether continuous panning across the international date line is enabled
 * @param selectionProperties the [SelectionProperties] used by the composable [com.arcgismaps.toolkit.geocompose.MapView].
 * @param onViewpointChanged lambda invoked when the viewpoint of the composable MapView has changed
 * @param overlay the composable overlays to display on top of the composable MapView. Example, a compass, floorfilter etc.
 * @since 200.3.0
 */
@Composable
public fun MapView(
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap? = null,
    locationDisplay: LocationDisplay = rememberLocationDisplay(),
    wrapAroundMode: WrapAroundMode = WrapAroundMode.EnabledWhenSupported,
    geometryEditor: GeometryEditor? = null,
    mapViewInteractionOptions: MapViewInteractionOptions = MapViewInteractionOptions(),
    selectionProperties: SelectionProperties = SelectionProperties(),
    onViewpointChanged: (() -> Unit)? = null,
    mapInsets: PaddingValues = PaddingValues(),
    overlay: @Composable () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val layoutDirection = LocalLayoutDirection.current

    Box(modifier = Modifier.semantics { contentDescription = "MapContainer" }) {
        AndroidView(
            modifier = modifier.semantics { contentDescription = "MapView" },
            factory = { mapView },
            update = {
                it.map = arcGISMap
                it.selectionProperties = selectionProperties
                it.interactionOptions = mapViewInteractionOptions
                it.locationDisplay = locationDisplay
                it.wrapAroundMode = wrapAroundMode
                it.geometryEditor = geometryEditor
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

    LaunchedEffect(mapInsets) {
        // When this call is made in the AndroidView's update callback, ViewInsets are not applied
        // on the mapview on initial load. So we set the ViewInsets here.
        mapView.setViewInsets(
            mapInsets.calculateStartPadding(layoutDirection).value.toDouble(),
            mapInsets.calculateEndPadding(layoutDirection).value.toDouble(),
            mapInsets.calculateTopPadding().value.toDouble(),
            mapInsets.calculateBottomPadding().value.toDouble()
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

@Preview
@Composable
internal fun MapViewPreview() {
    MapView()
}
