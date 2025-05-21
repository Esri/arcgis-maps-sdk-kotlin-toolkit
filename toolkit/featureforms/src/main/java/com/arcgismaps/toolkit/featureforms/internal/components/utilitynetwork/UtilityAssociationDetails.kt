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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType

/**
 * A composable that displays the details of a [UtilityAssociationResult].
 *
 * @param state The [UtilityAssociationsElementState] of the element.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociationDetails(
    state: UtilityAssociationsElementState,
    modifier: Modifier = Modifier
) {
    val associationResult = state.selectedAssociationResult ?: return
    val filter = state.selectedFilterResult?.filter ?: return
    val association = associationResult.association
    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.padding(24.dp)) {
            Column {
                PropertyRow(
                    title = stringResource(R.string.from_element),
                    value = "${association.fromElement.objectId}",
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                if (association.fromElement.terminal != null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    UtilityTerminalControl(
                        name = association.fromElement.terminal!!.name,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
        Card(modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Column {
                PropertyRow(
                    title = stringResource(R.string.to_element),
                    value = "${association.toElement.objectId}",
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                if (association.toElement.terminal != null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    UtilityTerminalControl(
                        name = association.toElement.terminal!!.name,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
        Card(modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            PropertyRow(
                title = stringResource(R.string.association_type),
                value = filter.filterType.toString(),
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            )
            if (association.associationType is UtilityAssociationType.Containment) {
                HorizontalDivider()
                ContentVisibleControl(
                    value = association.isContainmentVisible,
                    enabled = false,
                    onValueChange = {},
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .fillMaxWidth()
                )
            }
        }
        associationResult.getFractionAlongEdge()?.let { fraction ->
            FractionAlongEdgeControl(
                fraction = associationResult.getFractionAlongEdge()!!.toFloat(),
                enabled = false,
                onValueChanged = {},
                modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { showConfirmationDialog = true }) {
            Text(text = stringResource(R.string.remove_association))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.remove_association_tooltip),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (showConfirmationDialog) {
        RemoveAssociationConfirmationDialog(
            onDismiss = { showConfirmationDialog = false },
            onRemove = {
                showConfirmationDialog = false
                // Remove the association when the API is available.
            }
        )
    }
}

/**
 * A composable that displays a row with a title and value.
 */
@Composable
internal fun PropertyRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            )
        )
    }
}

/**
 * A composable that displays a confirmation dialog for removing an association.
 *
 * @param onDismiss A callback that is called when the dialog is dismissed.
 * @param onRemove A callback that is called when the remove button is clicked.
 */
@Composable
private fun RemoveAssociationConfirmationDialog(
    onDismiss: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "${stringResource(R.string.remove_association)}?")
        },
        text = {
            Text(text = stringResource(R.string.remove_association_tooltip))
        },
        confirmButton = {
            TextButton(onRemove) {
                Text(text = stringResource(R.string.remove))
            }
        },
        dismissButton = {
            TextButton(onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Extension function that returns the fraction along edge of the association result, if applicable.
 *
 * @return The fraction along edge of the association result, or null if not applicable.
 */
internal fun UtilityAssociationResult.getFractionAlongEdge(): Double? {
    val target = association.getTargetElement(associatedFeature)
    return when (this.association.associationType) {

        UtilityAssociationType.Connectivity -> {
            if (target.networkSource.sourceType == UtilityNetworkSourceType.Edge) {
                target.fractionAlongEdge
            } else {
                null
            }
        }

        UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan,
        UtilityAssociationType.JunctionEdgeObjectConnectivityToSide -> {
            association.fractionAlongEdge
        }

        else -> null
    }
}
