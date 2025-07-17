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

package com.arcgismaps.toolkit.offline.preplanned

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
import com.arcgismaps.toolkit.offline.internal.utils.CancelDownloadButtonWithProgressIndicator
import com.arcgismaps.toolkit.offline.internal.utils.DownloadButton
import com.arcgismaps.toolkit.offline.internal.utils.OpenButton
import com.arcgismaps.toolkit.offline.internal.utils.htmlToPlainText
import com.arcgismaps.toolkit.offline.theme.ColorScheme
import com.arcgismaps.toolkit.offline.theme.Typography
import com.arcgismaps.toolkit.offline.ui.MapAreaDetailsBottomSheet
import com.arcgismaps.toolkit.offline.ui.material3.rememberModalBottomSheetState

/**
 * Displays a list of preplanned map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun PreplannedMapAreas(
    preplannedMapAreaStates: List<PreplannedMapAreaState>,
    isShowingOnlyOfflineModels: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    onDownloadDeleted: (PreplannedMapAreaState) -> Unit,
    modifier: Modifier
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val selectedPreplannedMapAreaState = selectedIndex.takeIf { it in preplannedMapAreaStates.indices }
        ?.let { preplannedMapAreaStates[it] }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var onHideSheet by rememberSaveable { mutableStateOf(false) }
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
    if (showSheet && selectedPreplannedMapAreaState != null) {
        MapAreaDetailsBottomSheet(
            showSheet = true,
            sheetState = sheetState,
            colorScheme = colorScheme,
            typography = typography,
            onDismiss = { onHideSheet = true },
            offlineMapMode = OfflineMapMode.Preplanned,
            thumbnail = selectedPreplannedMapAreaState.thumbnail?.asImageBitmap(),
            title = selectedPreplannedMapAreaState.title,
            description = selectedPreplannedMapAreaState.description,
            size = selectedPreplannedMapAreaState.directorySize,
            isAvailableToDownload = selectedPreplannedMapAreaState.status.allowsDownload,
            onStartDownload = {
                selectedPreplannedMapAreaState.downloadPreplannedMapArea()
                onHideSheet = true
            },
            isDeletable = selectedPreplannedMapAreaState.status.isDownloaded && !selectedPreplannedMapAreaState.isSelectedToOpen,
            onDeleteDownload = {
                selectedPreplannedMapAreaState.removeDownloadedMapArea { !preplannedMapAreaStates.any { it.status.isDownloaded } }
                // Dismiss the sheet and update the map areas list only in offline mode
                if (isShowingOnlyOfflineModels) {
                    onDownloadDeleted(selectedPreplannedMapAreaState)
                    onHideSheet = true
                }
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
            itemsIndexed(preplannedMapAreaStates) { index, state ->
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
                            style = typography.preplannedMapAreaTitle,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 1, // Restrict to one line
                            overflow = TextOverflow.Ellipsis // Add ellipses if the text overflows
                        )
                        // Display the description with a maximum of two lines
                        Text(
                            text = htmlToPlainText(state.description),
                            style = typography.preplannedMapAreaDescription,
                            maxLines = 2, // Restrict to two lines
                            overflow = TextOverflow.Ellipsis // Add ellipses if the text overflows
                        )
                        // Display the status string
                        val statusString = if (state.isSelectedToOpen) {
                            stringResource(R.string.currently_open)
                        } else {
                            getPreplannedMapAreaStatusString(
                                context = LocalContext.current,
                                status = state.status
                            )
                        }
                        Text(
                            text = statusString,
                            style = typography.preplannedMapAreaStatus,
                            maxLines = 1, // Restrict to one lines
                        )
                    }
                    // Display the action button based on the status
                    when {
                        state.status.allowsDownload -> {
                            DownloadButton(colorScheme) {
                                if (state.status.allowsDownload) {
                                    state.downloadPreplannedMapArea()
                                }
                            }
                        }

                        state.status == PreplannedStatus.Downloading -> {
                            CancelDownloadButtonWithProgressIndicator(colorScheme, state.downloadProgress.value) {
                                state.cancelDownload()
                            }
                        }

                        state.status.isDownloaded -> {
                            OpenButton(colorScheme, typography, !state.isSelectedToOpen) {
                                // Unselect all, then select this one
                                preplannedMapAreaStates.forEach { it.setSelectedToOpen(false) }
                                state.setSelectedToOpen(true)
                            }
                        }
                    }
                }
                if (state.preplannedMapArea != preplannedMapAreaStates.last().preplannedMapArea) {
                    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}

/**
 * Retrieves a user-friendly status string for a preplanned map area based on its current status.
 *
 * @param context The `Context` used to access application-specific resources like strings.
 * @param status  The current state of the preplanned map area, represented by an instance of `Status`.
 * @return A localized string corresponding to the given status.
 * @since 200.8.0
 */
private fun getPreplannedMapAreaStatusString(context: Context, status: PreplannedStatus): String {
    return when (status) {
        PreplannedStatus.NotLoaded, PreplannedStatus.Loading -> getString(context, R.string.loading)
        is PreplannedStatus.LoadFailure, is PreplannedStatus.MmpkLoadFailure -> getString(context, R.string.loading_failed)
        is PreplannedStatus.DownloadFailure -> getString(context, R.string.download_failed)
        PreplannedStatus.Downloaded -> getString(context, R.string.downloaded)
        PreplannedStatus.Downloading -> getString(context, R.string.downloading)
        PreplannedStatus.PackageFailure -> getString(context, R.string.packaging_failed)
        PreplannedStatus.Packaged -> getString(context, R.string.ready_to_download)
        PreplannedStatus.Packaging -> getString(context, R.string.packaging)
    }
}
