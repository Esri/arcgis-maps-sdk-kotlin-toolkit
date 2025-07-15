/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offline.ondemand

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import com.arcgismaps.toolkit.offline.OfflineMapMode
import com.arcgismaps.toolkit.offline.R
import com.arcgismaps.toolkit.offline.internal.utils.CancelButton
import com.arcgismaps.toolkit.offline.internal.utils.CancelDownloadButtonWithProgressIndicator
import com.arcgismaps.toolkit.offline.internal.utils.DownloadButton
import com.arcgismaps.toolkit.offline.internal.utils.OpenButton
import com.arcgismaps.toolkit.offline.theme.ColorScheme
import com.arcgismaps.toolkit.offline.theme.Typography
import com.arcgismaps.toolkit.offline.ui.MapAreaDetailsBottomSheet
import com.arcgismaps.toolkit.offline.ui.material3.rememberModalBottomSheetState

/**
 * Displays a list of on-demand map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun OnDemandMapAreas(
    onDemandMapAreasStates: List<OnDemandMapAreasState>,
    onDownloadDeleted: (OnDemandMapAreasState) -> Unit,
    colorScheme: ColorScheme,
    typography: Typography,
    modifier: Modifier
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var onHideSheet by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val selectedOnDemandMapAreaState = selectedIndex.takeIf { it in onDemandMapAreasStates.indices }
        ?.let { onDemandMapAreasStates[it] }

    LaunchedEffect(onHideSheet, sheetState.isVisible) {
        if (onHideSheet) {
            sheetState.hide()
            onHideSheet = false
        }
        if (!sheetState.isVisible) {
            showSheet = false
        }
    }

    // Show the modal bottom sheet if needed
    if (showSheet && selectedOnDemandMapAreaState != null) {
        MapAreaDetailsBottomSheet(
            showSheet = true,
            sheetState = sheetState,
            colorScheme = colorScheme,
            typography = typography,
            onDismiss = { showSheet = false },
            offlineMapMode = OfflineMapMode.OnDemand,
            thumbnail = selectedOnDemandMapAreaState.thumbnail?.asImageBitmap(),
            title = selectedOnDemandMapAreaState.title,
            description = null,
            size = selectedOnDemandMapAreaState.directorySize,
            isAvailableToDownload = selectedOnDemandMapAreaState.status.allowsDownload,
            isDeletable = selectedOnDemandMapAreaState.status.isDownloaded && !selectedOnDemandMapAreaState.isSelectedToOpen,
            onDeleteDownload = {
                selectedOnDemandMapAreaState.removeDownloadedMapArea { !onDemandMapAreasStates.any { it.status.isDownloaded } }
                onDownloadDeleted(selectedOnDemandMapAreaState)
                onHideSheet = true
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.map_areas),
            style = typography.offlineMapAreasTitle,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier) {
            itemsIndexed(onDemandMapAreasStates) { index, state ->
                Row(
                    modifier = Modifier
                        .clickable {
                            selectedIndex = index
                            showSheet = true
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display the thumbnail image if available, otherwise show a placeholder icon
                    val thumbnail = state.thumbnail?.asImageBitmap()
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .size(64.dp) // Ensures the image is square
                            .clip(RoundedCornerShape(10.dp)), // Applies rounded corners
                    ) {
                        if (thumbnail != null) {
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                bitmap = thumbnail,
                                contentDescription = stringResource(R.string.thumbnail_description),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                modifier = Modifier
                                    .size(32.dp) // half the size of the image Box
                                    .align(Alignment.Center),
                                imageVector = Icons.Default.ImageNotSupported,
                                contentDescription = stringResource(id = R.string.no_image_available),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        // Display the title with a maximum of one line
                        Text(
                            text = state.title,
                            style = typography.onDemandMapAreasTitle,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 1, // Restrict to one line
                            overflow = TextOverflow.Ellipsis // Add ellipses if the text overflows
                        )
                        // Display the status string
                        val statusString = if (state.isSelectedToOpen) {
                            stringResource(R.string.currently_open)
                        } else {
                            getOnDemandMapAreaStatusString(
                                context = LocalContext.current,
                                status = state.status
                            )
                        }
                        Text(
                            text = statusString,
                            style = typography.onDemandMapAreaStatus,
                            maxLines = 1, // Restrict to one lines
                        )
                    }
                    // Display the action button based on the status
                    when {
                        state.status.allowsDownload -> {
                            DownloadButton(colorScheme = colorScheme) {
                                if (state.status.allowsDownload) {
                                    state.downloadOnDemandMapArea()
                                }
                            }
                        }

                        state.status == OnDemandStatus.Downloading -> {
                            CancelDownloadButtonWithProgressIndicator(colorScheme, state.downloadProgress.value) {
                                state.cancelDownload()
                            }
                        }

                        state.status == OnDemandStatus.DownloadCancelled || state.status is OnDemandStatus.DownloadFailure -> {
                            CancelButton(colorScheme = colorScheme){
                                state.removeCancelledMapArea { !onDemandMapAreasStates.any { it.status.isDownloaded } }
                                onDownloadDeleted(state)
                            }
                        }

                        state.status.isDownloaded -> {
                            OpenButton(!state.isSelectedToOpen) {
                                // Unselect all, then select this one
                                onDemandMapAreasStates.forEach { it.setSelectedToOpen(false) }
                                state.setSelectedToOpen(true)
                            }
                        }
                    }
                }
                if (state != onDemandMapAreasStates.last()) {
                    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}

/**
 * Retrieves a user-friendly status string for a on demand map area based on its current status.
 *
 * @param context The `Context` used to access application-specific resources like strings.
 * @param status  The current state of the on demand map area, represented by an instance of `Status`.
 * @return A localized string corresponding to the given status.
 * @since 200.8.0
 */
private fun getOnDemandMapAreaStatusString(context: Context, status: OnDemandStatus): String {
    return when (status) {
        OnDemandStatus.NotLoaded, OnDemandStatus.Loading -> getString(context, R.string.loading)
        is OnDemandStatus.LoadFailure, is OnDemandStatus.MmpkLoadFailure -> getString(context, R.string.loading_failed)
        is OnDemandStatus.DownloadFailure -> getString(context, R.string.download_failed)
        OnDemandStatus.Downloaded -> getString(context, R.string.downloaded)
        OnDemandStatus.Downloading -> getString(context, R.string.downloading)
        OnDemandStatus.DownloadCancelled -> getString(context, R.string.cancelled)
    }
}
