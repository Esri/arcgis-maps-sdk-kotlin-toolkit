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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.orientedimageryviewer.R
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3.ModalBottomSheet
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3.rememberModalBottomSheetState

/** Selects which footprint and camera overlays are visible. */
@Composable
internal fun OverlaySelectionButton(
    hasActiveImage: Boolean,
    hasAdditionalImages: Boolean,
    isActiveFootprintVisible: Boolean,
    isAdditionalFootprintsVisible: Boolean,
    isAdditionalCameraLocationsVisible: Boolean,
    onCurrentFootprint: () -> Unit,
    onAdditionalFootprints: () -> Unit,
    onAdditionalCameraLocations: () -> Unit
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }

    FilterChip(
        selected = isActiveFootprintVisible || isAdditionalFootprintsVisible ||
                isAdditionalCameraLocationsVisible,
        onClick = { showSheet = true },
        label = { Text(stringResource(R.string.toolbar_overlays)) },
        modifier = Modifier.padding(horizontal = 4.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.overlays_24),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.overlay_selection_title),
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
                OverlayOption(
                    checked = isActiveFootprintVisible,
                    enabled = hasActiveImage,
                    onToggle = onCurrentFootprint,
                    label = stringResource(R.string.overlay_selection_current_footprint),
                    icon = R.drawable.current_footprint_24
                )
                HorizontalDivider()
                OverlayOption(
                    checked = isAdditionalFootprintsVisible,
                    enabled = hasAdditionalImages,
                    onToggle = onAdditionalFootprints,
                    label = stringResource(R.string.overlay_selection_additional_footprints),
                    icon = R.drawable.additional_footprints_24
                )
                HorizontalDivider()
                OverlayOption(
                    checked = isAdditionalCameraLocationsVisible,
                    enabled = hasAdditionalImages,
                    onToggle = onAdditionalCameraLocations,
                    label = stringResource(R.string.overlay_selection_camera_locations),
                    icon = R.drawable.camera_locations_24
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun OverlayOption(
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    label: String,
    @DrawableRes icon: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { onToggle() }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        // The entire row owns the toggle semantics, including taps on the switch.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
