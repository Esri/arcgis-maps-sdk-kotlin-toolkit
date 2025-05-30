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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.offline.OfflineMapAreas
import com.arcgismaps.toolkit.offline.OfflineMapState
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetState
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.offlinemapareasapp.screens.bottomsheet.rememberStandardBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewModel: MapViewModel = hiltViewModel(), onBackPressed: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val options = listOf("Go Online", "Offline Maps")
    var selectedOption by remember { mutableStateOf(options[0]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = { it != SheetValue.Hidden },
        skipHiddenState = false
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mapViewModel.portalItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isDropdownExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedOption = option
                                    isDropdownExpanded = false
                                    if (option == "Go Online") {
                                        mapViewModel.selectedMap.value = null
                                        mapViewModel.offlineMapState.resetSelectedMapArea()
                                        scope.launch { bottomSheetState.partialExpand() }
                                    } else if (option == "Offline Maps") {
                                        scope.launch { bottomSheetState.expand() }
                                    }
                                },
                                enabled = option == "Offline Maps" || mapViewModel.selectedMap.value != null
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box {
            // show the composable map using the mapViewModel
            MapView(
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.proxy,
                onDown = {
                    if (bottomSheetState.isVisible) {
                        scope.launch { bottomSheetState.hide() }
                    }
                },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "Offline map areas sheet"
            ) {
                OfflineMapAreasSheet(
                    modifier = Modifier.padding(padding),
                    offlineMapState = mapViewModel.offlineMapState,
                    bottomSheetState = bottomSheetState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapAreasSheet(
    modifier: Modifier = Modifier,
    offlineMapState: OfflineMapState,
    bottomSheetState: SheetState,
) {
    val windowSize = getWindowSize(LocalContext.current)

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
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}
