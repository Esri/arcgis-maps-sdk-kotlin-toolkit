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

package com.arcgismaps.toolkit.offline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.OfflineMapMode
import com.arcgismaps.toolkit.offline.R
import com.arcgismaps.toolkit.offline.internal.utils.formatSize
import com.arcgismaps.toolkit.offline.internal.utils.htmlToPlainText
import com.arcgismaps.toolkit.offline.theme.ColorScheme
import com.arcgismaps.toolkit.offline.theme.OfflineMapAreasDefaults
import com.arcgismaps.toolkit.offline.theme.Typography
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheet
import com.arcgismaps.toolkit.offline.ui.material3.SheetState

@Composable
internal fun MapAreaDetailsBottomSheet(
    showSheet: Boolean,
    sheetState: SheetState,
    colorScheme: ColorScheme,
    typography: Typography,
    onDismiss: () -> Unit,
    offlineMapMode: OfflineMapMode,
    thumbnail: ImageBitmap?,
    title: String,
    description: String?,
    size: Int,
    isAvailableToDownload: Boolean,
    isDeletable: Boolean,
    onStartDownload: () -> Unit = {},
    onDeleteDownload: () -> Unit
) {
    if (showSheet) {
        // Launch expanded when shown
        LaunchedEffect(Unit) {
            sheetState.expand()
        }
        ModalBottomSheet(
            containerColor = colorScheme.offlineBackgroundColor,
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            MapAreaDetailsScreen(
                offlineMapMode = offlineMapMode,
                thumbnail = thumbnail,
                title = title,
                description = description,
                size = size,
                isAvailableToDownload = isAvailableToDownload,
                isDeletable = isDeletable,
                colorScheme = colorScheme,
                typography = typography,
                onStartDownload = onStartDownload,
                onDeleteDownload = onDeleteDownload
            )
        }
    }
}

@Composable
internal fun MapAreaDetailsScreen(
    offlineMapMode: OfflineMapMode,
    thumbnail: ImageBitmap?,
    title: String,
    description: String?,
    size: Int,
    isAvailableToDownload: Boolean,
    isDeletable: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    onStartDownload: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(12.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, colorScheme.offlineBackgroundColor, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
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
                        .align(Alignment.Center)
                        .size(50.dp), // 1/4th the size of the image Box
                    imageVector = Icons.Default.ImageNotSupported,
                    contentDescription = stringResource(id = R.string.no_image_available),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Title
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = typography.mapAreasDetailsTitle)

        // Size of the map area
        if (size != 0) {
            Text(
                text = stringResource(R.string.directory_size, formatSize(size)),
                style = typography.mapAreasDetailsSize
            )
        }

        if (description != null) {
            // Description label
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.description),
                style = typography.mapAreasDetailsDescriptionLabel,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 14.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(
                        colorScheme.offlineSurfaceContainerColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = htmlToPlainText(description),
                    style = typography.mapAreasDetailsDescription
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isAvailableToDownload) {
                Button(onClick = { onStartDownload() }, colors = colorScheme.offlineButtonsColor) {
                    Text(stringResource(id = R.string.download_map_area))
                }
            }
            if (isDeletable) {
                Button(
                    onClick = { onDeleteDownload() },
                    colors = colorScheme.offlineButtonsColor,
                    modifier = Modifier.testTag("RemoveDownloadButton")
                ) {
                    Text(
                        if (offlineMapMode == OfflineMapMode.Preplanned)
                            stringResource(id = R.string.remove_download)
                        else
                            stringResource(R.string.delete_download)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMapAreaDetailsScreen() {
    MaterialTheme {
        Surface {
            MapAreaDetailsScreen(
                offlineMapMode = OfflineMapMode.Preplanned,
                thumbnail = null,
                title = "City Hall Area",
                description = "A map that contains stormwater network within...",
                size = 40000,
                isAvailableToDownload = false,
                isDeletable = true,
                colorScheme = OfflineMapAreasDefaults.colorScheme(),
                typography = OfflineMapAreasDefaults.typography(),
                onStartDownload = { },
                onDeleteDownload = { }
            )
        }
    }
}
