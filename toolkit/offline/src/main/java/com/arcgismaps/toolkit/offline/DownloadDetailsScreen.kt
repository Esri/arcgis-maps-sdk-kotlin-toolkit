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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.text.Html
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadDetailsBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    thumbnail: ImageBitmap?,
    title: String,
    description: String,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
//        confirmValueChange = { it != SheetValue.Hidden }
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
            DownloadDetailsScreen(
                thumbnail = thumbnail,
                title = title,
                description = description,
            )
        }
    }
}

@Composable
internal fun DownloadDetailsScreen(
    thumbnail: ImageBitmap?,
    title: String,
    description: String,
//   size: String,
//   isAvailableToDownload: Boolean,
//   isDeletable: Boolean,
//   onStartDownload: (PreplannedMapAreaState) -> Unit,
//   onDeleteDownload: (PreplannedMapAreaState) -> Unit
) {
    val descriptionText = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY).toString()
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        thumbnail?.let {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = descriptionText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
//        Text(text = "Size: $size", style = MaterialTheme.typography.bodySmall)
//        Spacer(modifier = Modifier.height(16.dp))
//        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            if (isAvailableToDownload) {
//                Button(onClick = { /* TODO: Pass the correct PreplannedMapAreaState */ }) {
//                    Text("Download")
//                }
//            }
//            if (isDeletable) {
//                Button(onClick = { /* TODO: Pass the correct PreplannedMapAreaState */ }) {
//                    Text("Delete")
//                }
//            }
//        }
    }
}