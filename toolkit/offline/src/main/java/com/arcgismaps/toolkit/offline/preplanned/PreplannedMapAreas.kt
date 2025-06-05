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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.arcgismaps.toolkit.offline.R
import androidx.compose.ui.graphics.RectangleShape
import com.arcgismaps.toolkit.offline.DownloadDetailsBottomSheet
import com.arcgismaps.toolkit.offline.DownloadDetailsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Displays a list of preplanned map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun PreplannedMapAreas(
    preplannedMapAreaStates: List<PreplannedMapAreaState>,
    modifier: Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    var selectedState by remember { mutableStateOf<PreplannedMapAreaState?>(null) }

    // Show the modal bottom sheet if needed
    if (showSheet && selectedState != null) {
        DownloadDetailsBottomSheet(
            showSheet = showSheet,
            onDismiss = { showSheet = false },
            thumbnail = selectedState!!.preplannedMapArea.portalItem.thumbnail?.image?.bitmap?.asImageBitmap() /* your default image */,
            title = selectedState!!.preplannedMapArea.portalItem.title,
            description = selectedState!!.preplannedMapArea.portalItem.description,
//            size = "", // pass actual size if available
//            isAvailableToDownload = true, // set as needed
//            isDeletable = true, // set as needed
//            onStartDownload = { /* ... */ },
//            onDeleteDownload = { /* ... */ }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.map_areas),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier) {
            items(preplannedMapAreaStates) { state ->
                Row(
                    modifier = Modifier
                        .clickable {
                            selectedState = state
                            showSheet = true
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.preplannedMapArea.portalItem.thumbnail?.image?.bitmap?.asImageBitmap()?.let {
                        Image(
                            bitmap = it,
                            contentDescription = stringResource(R.string.thumbnail_description),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .size(64.dp) // Ensures the image is square
                                .clip(RoundedCornerShape(10.dp)), // Applies rounded corners
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        // Display the title with a maximum of one line
                        Text(
                            text = state.preplannedMapArea.portalItem.title,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 1, // Restrict to one line
                            overflow = TextOverflow.Ellipsis // Add ellipses if the text overflows
                        )
                        // Display the description with a maximum of two lines
                        Text(
                            text = state.preplannedMapArea.portalItem.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, // Restrict to two lines
                            overflow = TextOverflow.Ellipsis // Add ellipses if the text overflows
                        )
                        // Display the status string
                        val statusString = if (state.isSelected) {
                            stringResource(R.string.currently_open)
                        } else {
                            getPreplannedMapAreaStatusString(
                                context = LocalContext.current,
                                status = state.status
                            )
                        }
                        Text(
                            text = statusString,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1, // Restrict to one lines
                        )
                    }
                    // Display the action button based on the status
                    when {
                        state.status.allowsDownload -> {
                            DownloadButton {
                                if (state.status.allowsDownload) {
                                    state.downloadPreplannedMapArea()
                                }
                            }
                        }
                        state.status == Status.Downloading -> {
                            CancelDownloadButtonWithProgressIndicator(state.downloadProgress.value) {
                                state.cancelDownload()
                            }
                        }
                        state.status.isDownloaded -> {
                            OpenButton(!state.isSelected) {
                                // Unselect all, then select this one
                                preplannedMapAreaStates.forEach { it.setSelected(false) }
                                state.setSelected(true)
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
private fun getPreplannedMapAreaStatusString(context: Context, status: Status): String {
    return when (status) {
        Status.NotLoaded, Status.Loading -> getString(context, R.string.loading)
        is Status.LoadFailure, is Status.MmpkLoadFailure -> getString(context, R.string.loading_failed)
        is Status.DownloadFailure -> getString(context, R.string.download_failed)
        Status.Downloaded -> getString(context, R.string.downloaded)
        Status.Downloading -> getString(context, R.string.downloading)
        Status.PackageFailure -> getString(context, R.string.packaging_failed)
        Status.Packaged -> getString(context, R.string.ready_to_download)
        Status.Packaging -> getString(context, R.string.packaging)
    }
}

@Composable
private fun DownloadButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier.size(30.dp),
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = stringResource(R.string.download),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun CancelDownloadButtonWithProgressIndicator(progress: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clickable { onClick.invoke() }
    ) {
        // Circular Progress Indicator
        CircularProgressIndicator(
            progress = { progress / 100f },
        )
        // Square Button to cancel the download
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RectangleShape)
                .background(ButtonDefaults.buttonColors().containerColor),
        )
    }
}

@Composable
private fun OpenButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.widthIn(max = 80.dp), // restricts max width
        contentPadding = PaddingValues(horizontal = 10.dp),
        enabled = isEnabled,
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.open),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
