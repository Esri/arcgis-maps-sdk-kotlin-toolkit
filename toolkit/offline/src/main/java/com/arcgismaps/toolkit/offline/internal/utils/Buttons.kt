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

package com.arcgismaps.toolkit.offline.internal.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.R
import com.arcgismaps.toolkit.offline.theme.ColorScheme
import com.arcgismaps.toolkit.offline.theme.OfflineMapAreasDefaults
import com.arcgismaps.toolkit.offline.theme.Typography

@Composable
internal fun DownloadButton(colorScheme: ColorScheme, onClick: () -> Unit) {
    IconButton(
        modifier = Modifier.size(30.dp),
        onClick = onClick
    ) {
        Icon(painter = painterResource(R.drawable.download_24px),
            contentDescription = stringResource(R.string.download),
            tint = colorScheme.offlineIconButtonsColor
        )
    }
}

@Composable
internal fun CancelDownloadButtonWithProgressIndicator(colorScheme: ColorScheme, progress: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clickable { onClick.invoke() }
    ) {
        // Circular Progress Indicator
        CircularProgressIndicator(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.Center),
            color = colorScheme.offlineIconButtonsColor,
            progress = { progress / 100f }
        )
        // Square Button to cancel the download
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RectangleShape)
                .background(colorScheme.offlineIconButtonsColor)
                .align(Alignment.Center),
        )
    }
}

@Composable
internal fun CancelButton(colorScheme: ColorScheme, onClick: () -> Unit) {
    IconButton(
        modifier = Modifier.size(30.dp),
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cancelled),
            tint = colorScheme.offlineIconButtonsColor
        )
    }
}

@Composable
internal fun OpenButton(
    colorScheme: ColorScheme,
    typography: Typography,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier.widthIn(max = 80.dp), // restricts max width
        contentPadding = PaddingValues(horizontal = 10.dp),
        enabled = isEnabled,
        onClick = onClick,
        colors = colorScheme.offlineButtonsColor
    ) {
        Text(
            text = stringResource(R.string.open),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.offlineOpenButtonText
        )
    }
}

@Composable
internal fun AddMapAreaButton(colorScheme: ColorScheme, typography: Typography, onAdd: () -> Unit) {
    Button(
        onClick = onAdd,
        colors = colorScheme.offlineButtonsColor
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Default.Add,
            contentDescription = "Icon to add map area"
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.add_map_area),
            style = typography.onDemandMapAreaAddMapAreaButtonText
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonsPreview() {
    MaterialTheme {
        Surface {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(12.dp),
                columns = GridCells.Fixed(2),
            ) {
                item {
                    DownloadButton(OfflineMapAreasDefaults.colorScheme()) { }
                }
                item {
                    CancelDownloadButtonWithProgressIndicator(OfflineMapAreasDefaults.colorScheme(), 55) { }
                }
                item {
                    OpenButton(OfflineMapAreasDefaults.colorScheme(), OfflineMapAreasDefaults.typography(), true) { }
                }
                item {
                    AddMapAreaButton(OfflineMapAreasDefaults.colorScheme(), OfflineMapAreasDefaults.typography()) { }
                }
                item {
                    CancelButton(OfflineMapAreasDefaults.colorScheme()) { }
                }
            }
        }
    }
}

