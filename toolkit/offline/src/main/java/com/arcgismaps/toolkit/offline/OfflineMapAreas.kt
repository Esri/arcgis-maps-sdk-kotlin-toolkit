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

package com.arcgismaps.toolkit.offline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.offline.internal.utils.AddMapAreaButton
import com.arcgismaps.toolkit.offline.internal.utils.getDefaultMapAreaTitle
import com.arcgismaps.toolkit.offline.internal.utils.isValidMapAreaTitle
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreaConfiguration
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreaSelector
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreas
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreasState
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreas
import com.arcgismaps.toolkit.offline.theme.ColorScheme
import com.arcgismaps.toolkit.offline.theme.OfflineMapAreasDefaults
import com.arcgismaps.toolkit.offline.theme.Typography
import com.arcgismaps.toolkit.offline.ui.EmptyOnDemandOfflineAreas
import com.arcgismaps.toolkit.offline.ui.EmptyPreplannedOfflineAreas
import com.arcgismaps.toolkit.offline.ui.NoInternetNoAreas
import com.arcgismaps.toolkit.offline.ui.OfflineDisabled
import com.arcgismaps.toolkit.offline.ui.OfflineMapAreasError

/**
 * Take a web map offline by downloading map areas.
 *
 * #### Features
 *
 * Supports both ahead-of-time (preplanned) and on-demand map areas for an offline enabled web map. This [OfflineMapAreas] composable:
 *
 * - Displays a list of map areas.
 *
 * - Shows download progress and status for map areas.
 *
 * - Opens a map area for viewing when selected.
 *
 * - Provides options to view details about downloaded map areas.
 *
 * - Supports removing downloaded offline map areas files from the device.
 *
 * For preplanned workflows, this composable:
 *
 * - Displays a list of available preplanned map areas from an offline-enabled web map that contains preplanned map areas when the network is connected.
 *
 * - Downloads preplanned map areas in the list.
 *
 * - Displays a list of downloaded preplanned map areas on the device when the network is disconnected.
 *
 * For on-demand workflows, this composable:
 *
 * - Allows users to add and download on-demand map areas to the device by specifying an area of interest and level of detail.
 *
 * - Displays a list of on-demand map areas available on the device that are tied to a specific web map.
 *
 * _Workflow example:_
 *
 * ```
 * val selectedMap = mutableStateOf<ArcGISMap?>(null)
 *
 * val displayedMap get() = selectedMap.value ?: onlineMap
 *
 * val offlineMapState = OfflineMapState(
 *     arcGISMap = displayedMap,
 *     onSelectionChanged = { offlineMap ->
 *         selectedMap.value = offlineMap
 *     }
 * )
 *
 * @Composable
 * fun SheetContent() {
 *     OfflineMapAreas(
 *         offlineMapState = offlineMapState,
 *         modifier = Modifier
 *             .padding(horizontal = 16.dp)
 *             .animateContentSize()
 *     )
 * }
 *
 *```
 *
 * @since 200.8.0
 */
@Composable
public fun OfflineMapAreas(
    offlineMapState: OfflineMapState,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme = OfflineMapAreasDefaults.colorScheme(),
    typography: Typography = OfflineMapAreasDefaults.typography()
) {
    val context = LocalContext.current
    val initializationStatus by offlineMapState.initializationStatus
    var isRefreshEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(offlineMapState, isRefreshEnabled) {
        if (isRefreshEnabled) {
            offlineMapState.resetInitialize()
        }
        offlineMapState.initialize(context)
        isRefreshEnabled = false
    }

    Surface(modifier = modifier) {
        when (initializationStatus) {
            is InitializationStatus.NotInitialized, InitializationStatus.Initializing -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is InitializationStatus.FailedToInitialize -> {
                OfflineMapAreasError(
                    onRefresh = { isRefreshEnabled = true },
                    error = (initializationStatus as InitializationStatus.FailedToInitialize).error
                )
            }

            is InitializationStatus.Initialized -> {
                // Check if the map has offline mode disabled
                if (offlineMapState.mapIsOfflineDisabled) {
                    OfflineDisabled(onRefresh = { isRefreshEnabled = true })
                } else {
                    // If offline mode is enabled, display the offline modes
                    when (offlineMapState.mode) {
                        // For preplanned, display online & offline map areas.
                        OfflineMapMode.Preplanned -> {
                            PreplannedLayoutContainer(
                                modifier = modifier,
                                preplannedMapAreaStates = offlineMapState.preplannedMapAreaStates,
                                isShowingOnlyOfflineModels = offlineMapState.isShowingOnlyOfflineModels,
                                colorScheme = colorScheme,
                                typography = typography,
                                onDownloadDeleted = offlineMapState::removePreplannedMapArea,
                                onRefresh = { isRefreshEnabled = true }
                            )
                        }
                        // If not preplanned state & map has offline mode enabled, display the on demand areas
                        OfflineMapMode.OnDemand, OfflineMapMode.Unknown -> {
                            OnDemandLayoutContainer(
                                modifier = modifier,
                                onDemandMapAreaStates = offlineMapState.onDemandMapAreaStates,
                                isShowingOnlyOfflineModels = offlineMapState.isShowingOnlyOfflineModels,
                                colorScheme = colorScheme,
                                typography = typography,
                                localMap = offlineMapState.localMap,
                                onRefresh = { isRefreshEnabled = true },
                                onDownloadDeleted = offlineMapState::removeOnDemandMapArea,
                                onDownloadMapAreaSelected = { onDemandConfig ->
                                    // Create the on-demand state and start the download
                                    offlineMapState.createOnDemandMapAreaState(
                                        context = context,
                                        configuration = onDemandConfig
                                    ).downloadOnDemandMapArea()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PreplannedLayoutContainer(
    modifier: Modifier,
    preplannedMapAreaStates: List<PreplannedMapAreaState>,
    isShowingOnlyOfflineModels: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    onDownloadDeleted: (PreplannedMapAreaState) -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        // Show preplanned map areas if available
        if (preplannedMapAreaStates.isNotEmpty()) {
            PreplannedMapAreas(
                preplannedMapAreaStates = preplannedMapAreaStates,
                isShowingOnlyOfflineModels = isShowingOnlyOfflineModels,
                colorScheme = colorScheme,
                typography = typography,
                onDownloadDeleted = onDownloadDeleted,
                modifier = modifier
            )
        }
        // Show "No Internet" message if offline models are displayed
        if (isShowingOnlyOfflineModels) {
            NoInternetNoAreas(
                onlyFooterVisible = preplannedMapAreaStates.isNotEmpty(),
                onRefresh = onRefresh
            )
        }
        // Show empty state message if no preplanned areas and online mode
        else if (preplannedMapAreaStates.isEmpty()) {
            EmptyPreplannedOfflineAreas(onRefresh = onRefresh)
        }
    }
}

@Composable
internal fun OnDemandLayoutContainer(
    modifier: Modifier,
    onDemandMapAreaStates: List<OnDemandMapAreasState>,
    isShowingOnlyOfflineModels: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    localMap: ArcGISMap,
    onRefresh: () -> Unit,
    onDownloadDeleted: (OnDemandMapAreasState) -> Unit,
    onDownloadMapAreaSelected: (OnDemandMapAreaConfiguration) -> Unit
) {
    // Track visibility of the map area selector
    var isOnDemandMapAreaSelectorVisible by rememberSaveable { mutableStateOf(false) }
    // Track if the proposed map area title is unique
    var isProposedTitleChangeUnique by rememberSaveable { mutableStateOf(true) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Show on-demand map areas if available
        if (onDemandMapAreaStates.isNotEmpty()) {
            OnDemandMapAreas(
                onDemandMapAreasStates = onDemandMapAreaStates,
                onDownloadDeleted = onDownloadDeleted,
                colorScheme = colorScheme,
                typography = typography,
                modifier = modifier
            )
            // Show "Add Map Area" button if not in offline-only mode
            if (!isShowingOnlyOfflineModels) {
                AddMapAreaButton(typography) { isOnDemandMapAreaSelectorVisible = true }
            }
        }
        // Show "No Internet" message if offline models are displayed
        if (isShowingOnlyOfflineModels) {
            NoInternetNoAreas(
                onlyFooterVisible = onDemandMapAreaStates.isNotEmpty(),
                onRefresh = onRefresh
            )
        }
        // Show empty state message if no on-demand areas and online mode
        else if (onDemandMapAreaStates.isEmpty()) {
            EmptyOnDemandOfflineAreas(typography) { isOnDemandMapAreaSelectorVisible = true }
        }
    }
    // Map area selection bottom sheet
    OnDemandMapAreaSelector(
        localMap = localMap,
        showSheet = isOnDemandMapAreaSelectorVisible,
        uniqueMapAreaTitle = getDefaultMapAreaTitle(onDemandMapAreaStates),
        colorScheme = colorScheme,
        typography = typography,
        isProposedTitleChangeUnique = isProposedTitleChangeUnique,
        onDismiss = { isOnDemandMapAreaSelectorVisible = false },
        onProposedTitleChange = { mapAreaTitle ->
            isProposedTitleChangeUnique = isValidMapAreaTitle(
                mapAreaTitle = mapAreaTitle,
                onDemandMapAreaStates = onDemandMapAreaStates
            )
        },
        onDownloadMapAreaSelected = onDownloadMapAreaSelected
    )
}
