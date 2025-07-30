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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.offline.OfflineMapAreas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewModel: MapViewModel = hiltViewModel(), onBackPressed: () -> Unit = {}) {
    val options = listOf("Go Online", "Offline Maps")
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val state = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded
        )
    )
    var onHideSheet by remember { mutableStateOf(false) }
    var onShowSheet by remember { mutableStateOf(true) }

    LaunchedEffect(onHideSheet) {
        state.bottomSheetState.partialExpand()
        onHideSheet = false
    }
    LaunchedEffect(onShowSheet) {
        state.bottomSheetState.expand()
        onShowSheet = false
    }

    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = state,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetPeekHeight = 150.dp,
        sheetContent = {
            OfflineMapAreas(
                offlineMapState = mapViewModel.offlineMapState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .animateContentSize()
            )
        },
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
                                    isDropdownExpanded = false
                                    if (option == "Go Online") {
                                        mapViewModel.selectedMap.value = null
                                        mapViewModel.offlineMapState.resetSelectedMapArea()
                                        onShowSheet = true
                                    } else if (option == "Offline Maps") {
                                        onShowSheet = true
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
        Box(modifier = Modifier.padding(padding)) {
            // show the composable map using the mapViewModel
            MapView(
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.proxy,
                onDown = { onHideSheet = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

