/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.orientedimageryviewer.GeoViewInteractionStatus
import com.arcgismaps.toolkit.orientedimageryviewer.R

/** Selects the operation performed when the user taps the GeoView. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModeSelectionButton(
    interactionStatus: GeoViewInteractionStatus,
    isFindImagesSelected: Boolean,
    isDisplayImageSelected: Boolean,
    onFindImagesSelectedChange: (Boolean) -> Unit,
    onDisplayImageSelectedChange: (Boolean) -> Unit
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var lastClickedModeIcon by rememberSaveable { mutableStateOf(R.drawable.find_images_24) }

    FilterChip(
        selected = interactionStatus == GeoViewInteractionStatus.FindImages ||
                interactionStatus == GeoViewInteractionStatus.DisplayImage,
        onClick = { showSheet = true },
        label = { Text(stringResource(R.string.toolbar_mode)) },
        modifier = Modifier.padding(horizontal = 4.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(lastClickedModeIcon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mode_selection_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.toolbar_close)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeOption(
                        selected = isFindImagesSelected,
                        onClick = {
                            lastClickedModeIcon = R.drawable.find_images_24
                            onFindImagesSelectedChange(!isFindImagesSelected)
                        },
                        label = stringResource(R.string.mode_selection_find_images),
                        icon = R.drawable.find_images_24,
                        modifier = Modifier.weight(1f)
                    )
                    ModeOption(
                        selected = isDisplayImageSelected,
                        onClick = {
                            lastClickedModeIcon = R.drawable.display_image_24
                            onDisplayImageSelectedChange(!isDisplayImageSelected)
                        },
                        label = stringResource(R.string.mode_selection_display_image),
                        icon = R.drawable.display_image_24,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.heightIn(min = 56.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
