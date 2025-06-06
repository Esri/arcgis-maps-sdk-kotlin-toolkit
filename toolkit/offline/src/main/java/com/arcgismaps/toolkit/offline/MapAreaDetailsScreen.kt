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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.internal.utils.formatSize
import com.arcgismaps.toolkit.offline.internal.utils.htmlToPlainText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapAreaDetailsBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    thumbnail: ImageBitmap?,
    title: String,
    description: String,
    size: Int,
    isAvailableToDownload: Boolean,
    isDeletable: Boolean,
    onStartDownload: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    if (showSheet) {
        // Launch expand when shown
        LaunchedEffect(Unit) {
            scope.launch { sheetState.expand() }
        }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            MapAreaDetailsScreen(
                thumbnail = thumbnail,
                title = title,
                description = description,
                size = size,
                isAvailableToDownload = isAvailableToDownload,
                isDeletable = isDeletable,
                onStartDownload = onStartDownload,
                onDeleteDownload = onDeleteDownload
            )
        }
    }
}

@Composable
internal fun MapAreaDetailsScreen(
    thumbnail: ImageBitmap?,
    title: String,
    description: String,
    size: Int,
    isAvailableToDownload: Boolean,
    isDeletable: Boolean,
    onStartDownload: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = stringResource(id = R.string.no_image_available),
                modifier = Modifier.size(300.dp)
            )
        }
        // Title
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)

        // Size of the map area
        if (size != 0) {
            Text(text = "Size: ${formatSize(size)}", style = MaterialTheme.typography.bodyMedium)
        }
        // Description label
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.description),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 14.dp)
        )
        // Description
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(
                text = htmlToPlainText(description),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isAvailableToDownload) {
                Button(onClick = { onStartDownload() }) {
                    Text(stringResource(id = R.string.download_map_area))
                }
            }
            if (isDeletable) {
                Button(onClick = { onDeleteDownload() }) {
                    Text(stringResource(id = R.string.delete_map_area))
                }
            }
        }
    }
}
