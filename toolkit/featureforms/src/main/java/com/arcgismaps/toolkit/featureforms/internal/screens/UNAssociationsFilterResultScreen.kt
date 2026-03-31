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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.material3.ModalBottomSheet
import com.arcgismaps.toolkit.featureforms.internal.components.material3.SheetState
import com.arcgismaps.toolkit.featureforms.internal.components.material3.SheetValue
import com.arcgismaps.toolkit.featureforms.internal.components.material3.rememberModalBottomSheetState
import com.arcgismaps.toolkit.featureforms.internal.components.material3.rememberSheetState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsFilterResult

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
                    onAddFromSourceClick(state.id)
                },
                enabled = isEditable
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Associations")
                Text(text = stringResource(R.string.add_associations), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        Spacer(modifier = Modifier.height(25.dp))
    }
}

@Composable
private fun AddActionSheet(
    onDismissRequest: () -> Unit,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewAddActionSheet() {
    val sheetState = rememberSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true },
        initialValue = SheetValue.Expanded,
    )
    AddActionSheet({}, {}, sheetState = sheetState)
}
