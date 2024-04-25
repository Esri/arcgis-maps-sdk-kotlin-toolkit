/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.popupappapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val portalItem = PortalItem(
    Portal.arcGISOnline(Portal.Connection.Anonymous),
    "9f3a674e998f461580006e626611f9ad"
)
private fun unselectFeature(feature: GeoElement?, layer: Layer?) {
    if (feature is Feature && layer is FeatureLayer) {
        layer.unselectFeature(feature)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISMap by remember {
        mutableStateOf(
            ArcGISMap(portalItem).apply {
                initialViewpoint = Viewpoint(
                    latitude = 39.8,
                    longitude = -98.6,
                    scale = 10e7
                )
            }
        )
    }
    val loadState by arcGISMap.loadStatus.collectAsState()
    val title by remember(loadState) { mutableStateOf(portalItem.title) }
    val proxy by remember { mutableStateOf(MapViewProxy()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var popup: Popup? by remember { mutableStateOf(null) }
    var layer: Layer? by remember { mutableStateOf(null) }
    var geoElement: GeoElement? by remember { mutableStateOf(null) }
    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            confirmValueChange = { it != SheetValue.Hidden },
            skipHiddenState = false
        )
    )
    BottomSheetScaffold(
        sheetContent = {
            com.arcgismaps.toolkit.popup.Popup(
                popup!!,
                modifier = Modifier.fillMaxSize()
            )
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = bottomSheetState,
        topBar = {
            TopFormBar(title = title) {
                //TODO: go back to webmap gallery by calling viewModel.onBackPressed() here
            }
        }
    ) { padding ->
        // show the composable map using the mapViewModel
        MapView(
            arcGISMap = arcGISMap,
            mapViewProxy = proxy,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onSingleTapConfirmed = {
                scope.launch {
                    proxy.identifyLayers(
                        screenCoordinate = it.screenCoordinate,
                        tolerance = 22.dp,
                        returnPopupsOnly = true
                    ).onSuccess { results ->
                        if (results.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "No Layers with Popups identified",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            try {
                                results.forEach { result ->
                                    result.popups.firstOrNull()?.let {
                                        val newLayer = result.layerContent
                                        val newGeoElement = result.geoElements.firstOrNull()

                                        if (newGeoElement == geoElement) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "the same GeoElement was selected",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            unselectFeature(geoElement, layer)

                                            when (newLayer) {
                                                is FeatureLayer -> {
                                                    // the Popup exists on a FeatureLayer
                                                    if (newGeoElement is ArcGISFeature) {
                                                        // the tap was on a Feature
                                                        // unselect any previously selected Feature
                                                        newLayer.selectFeature(newGeoElement)
                                                    }
                                                    // otherwise the tap was on some non-Feature GeoElement
                                                    layer = newLayer
                                                    geoElement = newGeoElement
                                                }

                                                is Layer -> {
                                                    // the popup exists on a non-FeatureLayer
                                                    layer = newLayer
                                                    geoElement = newGeoElement
                                                }

                                                else -> {
                                                    // the popup exists on a sublayer, which is a complication
                                                    // that doesn't offer any testing value for the Popup
                                                    // toolkit component.
                                                    throw IllegalStateException("popups on sublayers are not supported by the PopupApp")
                                                }
                                            }
                                            popup = it
                                        }
                                    } ?: run {
                                        unselectFeature(geoElement, layer)
                                        popup = null
                                        layer = null
                                        geoElement = null

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "tap was not on a GeoElement",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "failed to create a Popup for the feature",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        )
    }
//        AnimatedVisibility(
//            visible = popup != null,
//            enter = slideInVertically { h -> h },
//            exit = slideOutVertically { h -> h },
//            label = "feature form"
//        ) {
//            val rememberedForm: Popup = remember(this) {
//                popup!!
//            }
//            val bottomSheetState = rememberStandardBottomSheetState(
//                initialValue = SheetValue.PartiallyExpanded,
//                confirmValueChange = { it != SheetValue.Hidden },
//                skipHiddenState = false
//            )
//            SheetLayout(
//                windowSizeClass = windowSize,
//                sheetOffsetY = { bottomSheetState.requireOffset() },
//                modifier = Modifier.padding(padding),
//                maxWidth = BottomSheetMaxWidth,
//            ) { layoutWidth, layoutHeight ->
//                StandardBottomSheet(
//                    state = bottomSheetState,
//                    peekHeight = 40.dp,
//                    expansionHeight = SheetExpansionHeight(0.5f),
//                    sheetSwipeEnabled = true,
//                    shape = RoundedCornerShape(5.dp),
//                    layoutHeight = layoutHeight.toFloat(),
//                    sheetWidth = with(LocalDensity.current) { layoutWidth.toDp() }
//                ) {
//                    // set bottom sheet content to the FeatureForm
//                    Popup(
//                        popup!!,
//                        modifier = Modifier.fillMaxSize()
//                    )
//                }
//            }
//        }
//    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopFormBar(
    title: String,
    onBackPressed: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Preview
@Composable
fun TopFormBarPreview() {
    TopFormBar("Map")
}
