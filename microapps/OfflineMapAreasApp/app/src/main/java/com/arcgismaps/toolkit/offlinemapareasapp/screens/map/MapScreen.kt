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

package com.arcgismaps.toolkit.offlinemapareasapp.screens.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.layers.ArcGISSublayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.offline.OfflineMapAreas
import com.arcgismaps.toolkit.offline.OfflineMapState
import com.arcgismaps.toolkit.offlinemapareasapp.R
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.rememberBottomSheetScaffoldState
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.rememberStandardBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewModel: MapViewModel = hiltViewModel(), onBackPressed: () -> Unit = {}) {
    val scope = rememberCoroutineScope()

    val coroutineScope = rememberCoroutineScope()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )

    // Radio options
    val options = listOf("Go Online", "Offline Maps")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                // show the top bar which changes available actions based on if the FeatureForm is
                // being shown and is in edit mode
                TopFormBar(
                    title = mapViewModel.portalItem.title,
                    editingMode = false,
                    onClose = {
                    },
                    onSave = {
                    },
                    onBackPressed = onBackPressed
                )
            }
        }
    ) { padding ->
        Box {
            // show the composable map using the mapViewModel
            MapView(
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.proxy,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                onSingleTapConfirmed = {

                }
            )
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                // show the add feature button when the user is not editing
                FloatingActionButton(
                    onClick = {
                    },
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Feature"
                    )
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "feature form"
            ) {
                FeatureFormSheet(
                    modifier = Modifier.padding(padding),
                    offlineMapState = mapViewModel.offlineMapState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureFormSheet(
    modifier: Modifier = Modifier,
    offlineMapState: OfflineMapState,
) {
    val windowSize = getWindowSize(LocalContext.current)
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = { it != SheetValue.Hidden },
        skipHiddenState = false
    )
    SheetLayout(
        windowSizeClass = windowSize,
        sheetOffsetY = { bottomSheetState.requireOffset() },
        modifier = modifier,
        maxWidth = BottomSheetMaxWidth,
    ) { layoutWidth, layoutHeight ->
        StandardBottomSheet(
            state = bottomSheetState,
            peekHeight = 40.dp,
            expansionHeight = SheetExpansionHeight(0.5f),
            sheetSwipeEnabled = true,
            shape = RoundedCornerShape(5.dp),
            layoutHeight = layoutHeight.toFloat(),
            sheetWidth = with(LocalDensity.current) { layoutWidth.toDp() }
        ) {
            OfflineMapAreas(
                offlineMapState = offlineMapState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DiscardEditsDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.discard))
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.discard_edits))
        },
        text = {
            Text(text = stringResource(R.string.all_changes_will_be_lost))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopFormBar(
    title: String,
    editingMode: Boolean,
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = if (editingMode) stringResource(R.string.edit_feature) else title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (editingMode) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Feature Editor"
                    )
                }
            } else {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (editingMode) {
                IconButton(onClick = onSave) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save Feature")
                }
            }
        }
    )
}


fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}

/**
 * Returns the label of the feature. If the feature has a name attribute, it will return that.
 * If the feature has an objectid attribute, it will return "Object ID : <objectid>". If neither
 * of these attributes are present, it will return "Unnamed Feature".
 */
val ArcGISFeature.label: String
    get() {
        return if (attributes["name"] != null) {
            attributes["name"] as String
        } else if (attributes["objectid"] != null) {
            "Object ID : ${attributes["objectid"]}"
        } else {
            "Unnamed Feature"
        }
    }

/**
 * Returns the symbol of the feature as a bitmap. If the feature's layer is a subtype feature layer,
 * it will return the symbol of the sublayer that the feature belongs to. If the feature's layer is
 * a feature layer, it will return the symbol of the feature layer.
 *
 * If the symbol cannot be created, it will return null.
 */
suspend fun ArcGISFeature.getSymbol(resources: Resources): Bitmap? {
    val renderer = when (featureTable?.layer) {
        is SubtypeFeatureLayer -> sublayer?.renderer
        is FeatureLayer -> (featureTable?.layer as? FeatureLayer)?.renderer
        else -> null
    }
    val symbol = renderer?.getSymbol(this) ?: return null
    return symbol.createSwatch(resources.displayMetrics.density).getOrNull()?.bitmap
}

/**
 * Returns the sublayer that the feature belongs to. If the feature's layer is not a subtype feature
 * layer, it will return null.
 */
val ArcGISFeature.sublayer: ArcGISSublayer?
    get() {
        val subtypeFeatureLayer = featureTable?.layer as? SubtypeFeatureLayer ?: return null
        val code = getFeatureSubtype()?.code ?: return null
        return subtypeFeatureLayer.getSublayerWithSubtypeCode(code)
    }

@Preview
@Composable
private fun TopFormBarPreview() {
    TopFormBar("Map", false)
}
