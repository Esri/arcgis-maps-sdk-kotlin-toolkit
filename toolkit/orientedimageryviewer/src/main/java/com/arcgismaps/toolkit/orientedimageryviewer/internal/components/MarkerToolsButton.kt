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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.arcgismaps.toolkit.orientedimageryviewer.R

@Composable
internal fun MarkerToolsButton(
    isMarkingLocation: Boolean,
    onAddMarker: () -> Unit,
    onSelectMarker: () -> Unit,
    onClearAllMarkers: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Keep the chip tappable while the menu is open. Handle Back explicitly.
    BackHandler(enabled = menuExpanded) { menuExpanded = false }

    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
        FilterChip(
            selected = isMarkingLocation,
            onClick = { menuExpanded = true },
            label = { Text(stringResource(R.string.toolbar_markers)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.location_image_24),
                    contentDescription = null
                )
            },
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.marker_tools_mark_location)) },
                trailingIcon = {
                    Switch(
                        checked = isMarkingLocation,
                        onCheckedChange = {
                            menuExpanded = false
                            onAddMarker()
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.location_image_24),
                        contentDescription = null
                    )
                },
                onClick = {
                    menuExpanded = false
                    onAddMarker()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.marker_tools_select_marker)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    menuExpanded = false
                    onSelectMarker()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.marker_tools_clear_all_markers)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                },
                onClick = {
                    menuExpanded = false
                    onClearAllMarkers()
                }
            )
        }
    }
}

