/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.legend

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import com.arcgismaps.toolkit.legend.theme.LegendDefaults
import com.arcgismaps.toolkit.legend.theme.Typography
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.parcelize.Parcelize

/**
 * A composable that displays a legend for the given operational layers and basemap.
 * The legend will display the layer symbology and description so users can better
 * understand what is being viewed in the GeoView. The component provides the option to show all
 * layers or only those layers in the current scale range of the GeoView as well as the ability
 * to reverse the order of items in the legend. The legend will display information from layers,
 * sublayers and group layers.
 *
 * The required parameters are the operational layers, the basemap to display in the legend and
 * the current scale of the GeoView. The currentScale can be obtained from the Composable MapView's
 * onViewpointChangedForCenterAndScale callback.
 *
 * _Workflow example:_
 *
 *  ```
 * fun MainScreen(viewModel: LegendViewModel = viewModel()) {
 *
 *     val loadState by viewModel.arcGISMap.loadStatus.collectAsState()
 *     val basemap = viewModel.arcGISMap.basemap.collectAsState()
 *     var currentScale: Double by remember { mutableDoubleStateOf(Double.NaN) }
 *     ...
 *
 *     // show composable MapView with a legend in the bottom sheet
 *      BottomSheetScaffold(
 *         sheetContent = {
 *             AnimatedVisibility(
 *                 visible = loadState is LoadStatus.Loaded,
 *                 ...
 *             ) {
 *                 Legend(
 *                     operationalLayers = viewModel.arcGISMap.operationalLayers,
 *                     basemap = basemap.value,
 *                     currentScale = currentScale,
 *                     modifier = Modifier.fillMaxSize()
 *                 )
 *             }
 *         },
 *         modifier = Modifier.fillMaxSize(),
 *         scaffoldState = scaffoldState,
 *         sheetSwipeEnabled = true,
 *         topBar = null
 *     ) { padding ->
 *         MapView(
 *             modifier = Modifier
 *                 .padding(padding)
 *                 .fillMaxSize(),
 *             arcGISMap = viewModel.arcGISMap,
 *             onViewpointChangedForCenterAndScale = {
 *                 currentScale = it.targetScale
 *             }
 *         )
 *     }
 *  ```
 *
 * @param operationalLayers The operational layers to display in the legend.
 * @param basemap The basemap to display in the legend.
 * @param currentScale The current scale of the GeoView.
 * @param modifier Modifier to be applied to the legend.
 * @param reverseLayerOrder Whether to reverse the order of the layers in the legend.
 * @param respectScaleRange Whether to respect the scale range of the layers.
 * @param title The title of the legend. Defaults to "Legend". Empty string will not display the title.
 * @param typography The typography to use for the legend.
 *
 * @since 200.7.0
 */
@Composable
public fun Legend(
    operationalLayers: List<LayerContent>,
    basemap: Basemap?,
    currentScale: Double,
    modifier: Modifier = Modifier,
    reverseLayerOrder: Boolean = false,
    respectScaleRange: Boolean = true,
    title: String = stringResource(R.string.title),
    typography: Typography = LegendDefaults.typography()
) {
    val density = LocalContext.current.resources.displayMetrics.density
    var initialized: Boolean by rememberSaveable(operationalLayers, basemap) { mutableStateOf(false) }
    val layerContentData =
        rememberSaveable(operationalLayers, basemap, saver = snapshotStateListSaver()) {
            mutableStateListOf<LayerContentData>()
        }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (!initialized) {
        LaunchedEffect(Unit) {
            // initialize legend content once
            operationalLayers.filterIsInstance<Layer>().forEach {
                it.load().onFailure { error ->
                    errorMessage = error.message ?: "Unknown error"
                    showErrorDialog = true
                }
            }

            basemap?.load()?.onFailure { error ->
                errorMessage = error.message ?: "Unknown error"
                showErrorDialog = true
            }

            basemap?.baseLayers?.forEach {
                it.load().onFailure { error ->
                    errorMessage = error.message ?: "Unknown error"
                    showErrorDialog = true
                }
            }
            basemap?.referenceLayers?.forEach {
                it.load().onFailure { error ->
                    errorMessage = error.message ?: "Unknown error"
                    showErrorDialog = true
                }
            }
            legendContent(
                layerContentData,
                operationalLayers,
                basemap?.baseLayers ?: emptyList(),
                basemap?.referenceLayers ?: emptyList(),
                reverseLayerOrder,
                density
            )
            initialized = true
        }
    } else {
        // Get the operational layers' and basemaps' sublayer contents or, if there are none,
        // the basemaps' reference layers sublayer contents. If no sublayer content is available
        // just get an empty list
        LaunchedEffect(Unit) {
            // Establish the sublayerContents observation after initialization and after a config change.
            val sublayerContent = operationalLayers.map {
                it.subLayerContents
            } + (basemap?.baseLayers?.map {
                it.subLayerContents
            } ?: emptyList()) + (basemap?.referenceLayers?.map {
                it.subLayerContents
            } ?: emptyList())

            // Drop the first values -- the initial sublayer contents are already in the Legend.
            val dropList = sublayerContent.map {
                it.drop(1)
            }
            val sublayerChangedFlow: Flow<List<LayerContent>> =
                merge(*dropList.toTypedArray()).shareIn(this, SharingStarted.Lazily)

            sublayerChangedFlow
                .collect {
                    legendContent(
                        layerContentData,
                        operationalLayers,
                        basemap?.baseLayers ?: emptyList(),
                        basemap?.referenceLayers ?: emptyList(),
                        reverseLayerOrder,
                        density
                    )
                }
        }
    }


    val validScale = (currentScale != 0.0 && !currentScale.isNaN())
    if (validScale && initialized) {
        Legend(modifier, layerContentData, currentScale, respectScaleRange, title, typography)
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text(text = stringResource(R.string.error)) },
            text = { Text(text = errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Provides the row data for LayerContent and their sublayer content.
 *
 * @param layerContentData the output list. It is a SnapshotStateList which when altered will cause recomposition.
 * @param layers the operational layers of the map
 * @param baseLayers the baseLayers of the Basemap
 * @param referenceLayers the referenceLayers of the Basemap
 * @param reverseOrder build the list in reverse order when true
 * @param density the screen metrics used to create symbol swatches for the Symbols returned by
 * LayerContent.fetchLegendInfos()
 *
 * @since 200.7.0
 */
private suspend fun legendContent(
    layerContentData: MutableList<LayerContentData>,
    layers: List<LayerContent>,
    baseLayers: List<LayerContent>,
    referenceLayers: List<LayerContent>,
    reverseOrder: Boolean,
    density: Float
) {
    layerContentData.clear()
    // Add the layers to the layer content data
    // Add the operational layers first
    addLayersAndSubLayersDataToLayerContentData(
        layers,
        reverseOrder,
        density,
        layerContentData
    )
    // Add the basemap layers
    addLayersAndSubLayersDataToLayerContentData(
        baseLayers,
        reverseOrder,
        density,
        layerContentData,
        addAtIndexZero = reverseOrder
    )
    // Add the reference layers
    addLayersAndSubLayersDataToLayerContentData(
        referenceLayers,
        reverseOrder,
        density,
        layerContentData,
        addAtIndexZero = !reverseOrder
    )
}

/**
 * Provides LayerContentData for a list of LayerContent. Descends recursively into any sublayer content
 *
 * @param layers the LayerContents to evaluate
 * @param reverseLayerOrder build the LegendContent in reverse order when trye
 * @param density the screen metrics to use to create bitmaps from Symbol swatches
 * @param layerContentData the output list of LayerContentData
 * @param addAtIndexZero used in reverse ordering to build the overall list for operational, base,
 * and reference layers
 *
 * @since 200.7.0
 */
private suspend fun addLayersAndSubLayersDataToLayerContentData(
    layers: List<LayerContent>?,
    reverseLayerOrder: Boolean,
    density: Float,
    layerContentData: MutableList<LayerContentData>,
    addAtIndexZero: Boolean = false
) {
    layers?.let { layerList ->
        // The order of the layers is reversed to match the order in the legend
        val orderedLayers = if (reverseLayerOrder) layerList else layerList.reversed()
        val filteredLayers = orderedLayers.filter { it.isVisible && it.showInLegend }
        val layersAndSubLayersLayerContentData =
            filteredLayers.flatMap { getLayerContentData(it, density) }
        if (addAtIndexZero) {
            layerContentData.addAll(0, layersAndSubLayersLayerContentData)
        } else {
            layerContentData.addAll(layersAndSubLayersLayerContentData)
        }
    }
}

/**
 * Gets the LayerContentData for a single LayerContent. Descends recursively into any sublayerContents
 *
 * @param layerContent the LayerContent to evaluate
 * @param density the screen metrics density to use to create bitmaps from Symbol swatches
 * @return a list of LayerContentData representing the LayerContent in addition to any
 * LayerContent.sublayerContents
 * @since 200.7.0
 */
private suspend fun getLayerContentData(
    layerContent: LayerContent,
    density: Float
): List<LayerContentData> {
    val data = LayerContentData(layerContent.name, layerContent is Layer) { scale ->
        layerContent.isVisibleAtScale(scale)
    }
    layerContent.fetchLegendInfos().onSuccess {
        it.map { info ->
            val bitmap = info.symbol?.createSwatch(density)?.getOrNull()?.bitmap
            LegendItem(info.name, bitmap)
        }.also { items ->
            data.legendItems.addAll(items)
        }
    }
    return if (layerContent.subLayerContents.value.isEmpty()) {
        listOf(data)
    } else {
        listOf(data) + layerContent.subLayerContents.value.flatMap { sublayer ->
            getLayerContentData(sublayer, density)
        }
    }
}

@Composable
private fun Legend(
    modifier: Modifier,
    legendItems: List<LayerContentData>,
    currentScale: Double,
    respectScaleRange: Boolean,
    title: String,
    typography: Typography
) {
    val localContext = LocalContext.current
    Column(modifier = modifier
        .semantics { contentDescription = localContext.getString(R.string.legend_component) }) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = typography.title
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn {
            itemsIndexed(legendItems) { index, item ->
                if (!respectScaleRange || item.isVisible(currentScale)) {
                    if (index == legendItems.size - 1 || item.name != legendItems[index + 1].name) {
                        if (item.isLayer && index < legendItems.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                        }
                        Row(
                            modifier = Modifier.then(
                                if (item.isLayer) Modifier.padding(
                                    start = 8.dp, top = 8.dp
                                ) else Modifier.padding(
                                    start = 16.dp, top = 8.dp
                                )
                            )
                        ) {
                            Text(
                                text = item.name,
                                style = if (item.isLayer) typography.layerName else typography.subLayerName
                            )
                        }
                    }
                    if (item.legendItems.isNotEmpty()) {
                        item.legendItems.forEach { legendInfo ->
                            LegendInfoRow(
                                legendInfo = legendInfo,
                                typography = typography,
                                modifier = if (item.isLayer) Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp
                                ) else Modifier.padding(
                                    start = 24.dp,
                                    top = 8.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendInfoRow(
    legendInfo: LegendItem,
    typography: Typography,
    modifier: Modifier
) {
    Row(modifier = modifier) {
        legendInfo.bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.symbol_description)
            )
        }
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(text = legendInfo.name, style = typography.legendInfoName)
    }
}

@Parcelize
private data class LegendItem(
    val name: String,
    val bitmap: Bitmap?
): Parcelable

@Parcelize
private data class LayerContentData(
    val name: String,
    val isLayer: Boolean,
    val legendItems: MutableList<LegendItem> = mutableListOf(),
    val isVisible: (Double) -> Boolean
) : Parcelable


/**
 * Preserves a SnapshotStateList across compositions
 *
 * @since 200.7.0
 */
private fun <T : Parcelable> snapshotStateListSaver(): Saver<SnapshotStateList<T>, Any> = listSaver(
    {
        it.toList()
    },
    {
        mutableStateListOf<T>().apply {
            addAll(it)
        }
    }
)
