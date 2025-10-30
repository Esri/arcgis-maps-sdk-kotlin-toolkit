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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityTerminal
import kotlinx.coroutines.launch

/**
 * A composable that displays the details of a [UtilityAssociationResult].
 *
 * @param state The [UtilityAssociationsElementState] of the element.
 * @param onDelete A callback that is called after the association is deleted.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociationDetails(
    state: UtilityAssociationsElementState,
    onDelete: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val associationResult = state.selectedAssociationResult ?: return
    val filter = state.selectedFilterResult?.filter ?: return
    val association = associationResult.association
    val (fromElement, fromTerminal) = associationResult.getFromElement(state.featureForm)
    val (toElement, toTerminal) = associationResult.getToElement(state.featureForm)
    val isEditable by state.isEditable.collectAsState()
    val scrollState = rememberScrollState()
    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright
    )
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Association Type
        Card(
            modifier = Modifier.padding(24.dp),
            colors = cardColors
        ) {
            PropertyRow(
                title = stringResource(R.string.association_type),
                value = filter.filterType.toString(),
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            )
            if (association.associationType is UtilityAssociationType.Containment) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                )
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
        // From Element
        Card(
            modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            colors = cardColors
        ) {
            Column {
                PropertyRow(
                    title = stringResource(R.string.from_element),
                    value = fromElement,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                fromTerminal?.let { terminal ->
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    UtilityTerminalControl(
                        selected = terminal,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        enabled = false,
                    )
                }
            }
        }
        // To Element
        Card(
            modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            colors = cardColors
        ) {
            Column {
                PropertyRow(
                    title = stringResource(R.string.to_element),
                    value = toElement,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                toTerminal?.let { terminal ->
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    UtilityTerminalControl(
                        selected = terminal,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        enabled = false,
                    )
                }
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
        Button(
            onClick = { showConfirmationDialog = true },
            enabled = isEditable
        ) {
            Text(text = stringResource(R.string.remove_association))
        }
    }
    if (showConfirmationDialog) {
        RemoveAssociationConfirmationDialog(
            onDismiss = { showConfirmationDialog = false },
            onRemove = {
                scope.launch {
                    state.selectedGroupResult?.let { group ->
                        val isGroupEmpty = group.delete(association)
                        onDelete(isGroupEmpty)
                    }
                    showConfirmationDialog = false
                }
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
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            ),
            textAlign = TextAlign.Right
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
internal fun RemoveAssociationConfirmationDialog(
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
    return when (association.associationType) {
        UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan,
        UtilityAssociationType.JunctionEdgeObjectConnectivityToSide -> {
            association.fractionAlongEdge
        }

        else -> null
    }
}

/**
 * Extension function that returns the from element and terminal of the association result based on
 * the provided feature form.
 *
 * @param featureForm The feature form to compare against.
 * @return A pair containing the element name and terminal.
 */
internal fun UtilityAssociationResult.getFromElement(featureForm: FeatureForm): Pair<String, UtilityTerminal?> {
    val feature = featureForm.feature
    return if (feature.globalId == association.fromElement.globalId) {
        Pair(featureForm.title.value, association.fromElement.terminal)
    } else {
        Pair(this.title, association.toElement.terminal)
    }
}

/**
 * Extension function that returns the to element and terminal of the association result based on
 * the provided feature form.
 *
 * @param featureForm The feature form to compare against.
 * @return A pair containing the element name and terminal.
 */
internal fun UtilityAssociationResult.getToElement(featureForm: FeatureForm): Pair<String, UtilityTerminal?> {
    val feature = featureForm.feature
    return if (feature.globalId == association.toElement.globalId) {
        Pair(featureForm.title.value, association.toElement.terminal)
    } else {
        Pair(this.title, association.fromElement.terminal)
    }
}
