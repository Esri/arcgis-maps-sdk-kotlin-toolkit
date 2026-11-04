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

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.arcgismaps.toolkit.orientedimageryviewer.R
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3.BottomSheetDefaults
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3.ModalBottomSheet
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3.ModalBottomSheetProperties
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3.rememberModalBottomSheetState

/**
 * A button that opens a bottom sheet with marker tools.
 *
 * @param isMarkingLocation Whether the user is currently marking a location.
 * @param onAddMarker Callback when the user selects the "Add Marker" option.
 * @param onSelectMarker Callback when the user selects the "Select Marker" option.
 * @param onClearAllMarkers Callback when the user selects the "Clear All Markers" option.
 * @param isMarkerSelectionVisible Whether the marker selection sheet is currently visible.
 * @param selectedOptionId The currently selected marker option ID, if any.
 * @param onSelectOption Callback when the user selects a marker option from the selection sheet.
 * @param onDismissSelection Callback when the marker selection sheet is dismissed.
 */
@Composable
internal fun MarkerToolsButton(
    isMarkingLocation: Boolean,
    onAddMarker: () -> Unit,
    onSelectMarker: () -> Unit,
    onClearAllMarkers: () -> Unit,
    isMarkerSelectionVisible: Boolean = false,
    selectedOptionId: MarkerOptionId? = null,
    onSelectOption: (MarkerOptionId) -> Unit = {},
    onDismissSelection: () -> Unit = {},
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    // Also support selection opened through OrientedImageryViewerState.selectMarker().
    LaunchedEffect(isMarkerSelectionVisible) {
        if (isMarkerSelectionVisible) showSheet = true
    }
    val dismiss: () -> Unit = {
        showSheet = false
        onDismissSelection()
    }

    FilterChip(
        selected = isMarkingLocation,
        onClick = { showSheet = true },
        label = {
            Text(
                text = stringResource(R.string.toolbar_markers),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
            )
        },
        modifier = Modifier.padding(horizontal = 2.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.location_image_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )

    if (showSheet || isMarkerSelectionVisible) {
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetGesturesEnabled = false,
            dragHandle = null,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
        ) {
            BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            // Use the dialog's dispatcher AND lifecycle, not the inherited activity owners.
            val dialogWindow = (LocalView.current.parent as DialogWindowProvider).window
            val dialogOwner = checkNotNull(dialogWindow.decorView.findViewTreeOnBackPressedDispatcherOwner())
            CompositionLocalProvider(
                LocalOnBackPressedDispatcherOwner provides dialogOwner,
                LocalLifecycleOwner provides dialogOwner,
                LocalNavigationEventDispatcherOwner provides (dialogOwner as NavigationEventDispatcherOwner)
            ) {
                BackHandler {
                    if (isMarkerSelectionVisible) onDismissSelection()
                }
            }
            if (isMarkerSelectionVisible) {
                MarkerSelection(
                    selectedOptionId = selectedOptionId,
                    onSelectOption = {
                        onSelectOption(it)
                        onDismissSelection()
                    },
                    onDismiss = dismiss,
                    onBack = onDismissSelection
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.marker_tools_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = dismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.toolbar_close))
                        }
                    }
                    MarkerToolRow(
                        label = stringResource(R.string.marker_tools_mark_location),
                        onClick = onAddMarker,
                        checked = isMarkingLocation,
                        leadingIcon = {
                            Icon(painterResource(R.drawable.location_image_24), null, Modifier.size(24.dp))
                        },
                        trailingContent = {
                            Switch(checked = isMarkingLocation, onCheckedChange = null)
                        }
                    )
                    HorizontalDivider()
                    MarkerToolRow(
                        label = stringResource(R.string.marker_tools_select_marker),
                        enabled = isMarkingLocation,
                        onClick = onSelectMarker,
                        leadingIcon = {
                            val option = MarkerSymbols.options.firstOrNull { it.id == selectedOptionId }
                                ?: MarkerSymbols.options.first()
                            val description = stringResource(option.descriptionResId)
                            Canvas(Modifier.size(24.dp).semantics { contentDescription = description }) {
                                drawMarkerSymbol(option.symbol, center, 24.dp.toPx())
                            }
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    )
                    HorizontalDivider()
                    MarkerToolRow(
                        label = stringResource(R.string.marker_tools_clear_all_markers),
                        onClick = onClearAllMarkers,
                        leadingIcon = {
                            Icon(painterResource(R.drawable.clear_markers_24), null, Modifier.size(24.dp))
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MarkerToolRow(
    label: String,
    onClick: () -> Unit,
    leadingIcon: @Composable () -> Unit,
    enabled: Boolean = true,
    checked: Boolean? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (checked != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = { onClick() }
                    )
                } else {
                    Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                }
            )
            .alpha(if (enabled) 1f else 0.38f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            leadingIcon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        )
        trailingContent()
    }
}
