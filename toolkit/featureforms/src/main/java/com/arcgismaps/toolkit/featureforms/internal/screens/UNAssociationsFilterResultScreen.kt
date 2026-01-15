/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.material3.ModalBottomSheet
import com.arcgismaps.toolkit.featureforms.internal.components.material3.SheetState
import com.arcgismaps.toolkit.featureforms.internal.components.material3.SheetValue
import com.arcgismaps.toolkit.featureforms.internal.components.material3.rememberModalBottomSheetState
import com.arcgismaps.toolkit.featureforms.internal.components.material3.rememberSheetState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsFilterResult
import kotlinx.coroutines.launch

/**
 * Screen that displays the selected filter for a [UtilityAssociationsFormElement].
 *
 * @param state The [UtilityAssociationsElementState] that holds the state.
 * @param onGroupSelected The callback that is invoked when a group is selected.
 * @param onAddFromSourceClick The callback that is invoked when the user wants to add associations
 * from a source.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsFilterResultScreen(
    state: UtilityAssociationsElementState,
    onGroupSelected: (Int) -> Unit,
    onAddFromSourceClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterResult = state.selectedFilterResult ?: return
    val isEditable by state.isEditable.collectAsState()
    // Determine if we should show the add action as a sheet or a dropdown menu
    // based on the current window size class
    val showAsSheet = getWindowSize(LocalContext.current)
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        .not()
    var showAddAssociationAction by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UtilityAssociationsFilterResult(
            groupResults = filterResult.groupResults,
            onGroupClick = { groupResult ->
                state.setSelectedGroupResult(groupResult)
                onGroupSelected(state.id)
            },
            modifier = Modifier
                .padding(16.dp)
                .wrapContentSize()
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    showAddAssociationAction = true
                },
                enabled = isEditable
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Associations")
                Text(text = stringResource(R.string.add_associations), modifier = Modifier.padding(horizontal = 12.dp))
            }
            if (showAddAssociationAction && !showAsSheet) {
                // Show the dropdown menu below the button
                AddActionMenu(
                    onDismissRequest = { showAddAssociationAction = false },
                    onSelectFromMap = {
                        // Currently not supported
                    },
                    onSelectFromNetworkDataSource = {
                        onAddFromSourceClick(state.id)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(25.dp))
    }
    if (showAddAssociationAction && showAsSheet) {
        AddActionSheet(
            onDismissRequest = { showAddAssociationAction = false },
            sheetState = sheetState,
            onSelectFromMap = {
                // Currently not supported
            },
            onSelectFromNetworkDataSource = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showAddAssociationAction = false
                        onAddFromSourceClick(state.id)
                    }
                }
            }
        )
    }
}

@Composable
private fun AddActionSheet(
    onDismissRequest: () -> Unit,
    onSelectFromMap: () -> Unit,
    onSelectFromNetworkDataSource: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {
                        onSelectFromMap()
                    }
                    .padding(horizontal = 32.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = "On Map")
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        text = stringResource(R.string.on_map),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectFromNetworkDataSource()
                    }
                    .padding(horizontal = 32.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.database_24px),
                    contentDescription = "From Network Data Source"
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    stringResource(R.string.from_network_data_source),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun AddActionMenu(
    onDismissRequest: () -> Unit,
    onSelectFromMap: () -> Unit,
    onSelectFromNetworkDataSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = DpOffset(0.dp, (-15).dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.on_map),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(Icons.Outlined.Map, contentDescription = "On Map")
            },
            enabled = false,
            onClick = onSelectFromMap,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.from_network_data_source),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.database_24px),
                    contentDescription = "From Network Data Source"
                )
            },
            onClick = onSelectFromNetworkDataSource,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

internal fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.Companion.BREAKPOINTS_V1.computeWindowSizeClass(
        widthDp = width / density,
        heightDp = height / density
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewAddActionSheet() {
    val sheetState = rememberSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true },
        initialValue = SheetValue.Expanded,
    )
    AddActionSheet({}, {}, {}, sheetState = sheetState)
}
