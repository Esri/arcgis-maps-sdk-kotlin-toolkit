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
import com.arcgismaps.toolkit.offline.ui.EmptyOnDemandOfflineAreas
import com.arcgismaps.toolkit.offline.ui.EmptyPreplannedOfflineAreas
import com.arcgismaps.toolkit.offline.ui.NoInternetNoAreas
import com.arcgismaps.toolkit.offline.ui.OfflineDisabled
import com.arcgismaps.toolkit.offline.ui.OfflineMapAreasError

/**
 * Take a web map offline by downloading map areas.
 *
 * @since 200.8.0
 */
@Composable
public fun OfflineMapAreas(
    offlineMapState: OfflineMapState,
    modifier: Modifier = Modifier
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
                                onRefresh = { isRefreshEnabled = true }
                            )
                        }
                        // If not preplanned state & map has offline mode enabled, display the on demand areas
                        OfflineMapMode.OnDemand, OfflineMapMode.Unknown -> {
                            OnDemandLayoutContainer(
                                modifier = modifier,
                                onDemandMapAreaStates = offlineMapState.onDemandMapAreaStates,
                                isShowingOnlyOfflineModels = offlineMapState.isShowingOnlyOfflineModels,
                                localMap = offlineMapState.localMap,
                                onRefresh = { isRefreshEnabled = true },
                                onDownloadMapAreaSelected = { onDemandConfig ->
                                    // Create the on-demand state and start the download
                                    offlineMapState.makeOnDemandMapAreaState(
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
    onRefresh: () -> Unit
) {
    Column {
        PreplannedMapAreas(
            preplannedMapAreaStates = preplannedMapAreaStates,
            modifier = modifier
        )
        if (isShowingOnlyOfflineModels) {
            NoInternetNoAreas(
                onlyFooterVisible = preplannedMapAreaStates.isNotEmpty(),
                onRefresh = onRefresh
            )
        } else if (preplannedMapAreaStates.isEmpty()) {
            EmptyPreplannedOfflineAreas(onRefresh = onRefresh)
        }
    }
}

@Composable
internal fun OnDemandLayoutContainer(
    modifier: Modifier,
    onDemandMapAreaStates: List<OnDemandMapAreasState>,
    isShowingOnlyOfflineModels: Boolean,
    localMap: ArcGISMap,
    onRefresh: () -> Unit,
    onDownloadMapAreaSelected: (OnDemandMapAreaConfiguration) -> Unit
) {
    var isOnDemandMapAreaSelectorVisible by rememberSaveable { mutableStateOf(false) }
    var isProposedTitleChangeUnique by rememberSaveable { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (onDemandMapAreaStates.isNotEmpty()) {
            OnDemandMapAreas(
                onDemandMapAreasStates = onDemandMapAreaStates,
                modifier = modifier
            )
            if (!isShowingOnlyOfflineModels) {
                AddMapAreaButton { isOnDemandMapAreaSelectorVisible = true }
            } else {
                NoInternetNoAreas(
                    onlyFooterVisible = true,
                    onRefresh = onRefresh
                )
            }
        }
        if (isShowingOnlyOfflineModels && onDemandMapAreaStates.isEmpty()) {
            NoInternetNoAreas(
                onlyFooterVisible = false,
                onRefresh = onRefresh
            )
        } else if (!isShowingOnlyOfflineModels && onDemandMapAreaStates.isEmpty()) {
            EmptyOnDemandOfflineAreas(
                onAdd = { isOnDemandMapAreaSelectorVisible = true }
            )
        }
    }
    OnDemandMapAreaSelector(
        localMap = localMap,
        showBottomSheet = isOnDemandMapAreaSelectorVisible,
        uniqueMapAreaTitle = getDefaultMapAreaTitle(onDemandMapAreaStates),
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