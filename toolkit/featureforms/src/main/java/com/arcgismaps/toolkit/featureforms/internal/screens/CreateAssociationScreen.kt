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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.ContentVisibleControl
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.FractionAlongEdgeControl
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.PropertyRow
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityTerminalControl
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.getTerminalById
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterType
import kotlinx.coroutines.launch

@Composable
internal fun CreateAssociationScreen(
    viewModel: AddAssociationFromSourceViewModel,
    onAssociationCreated: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val associationOptions = viewModel.newAssociationOptions
    var selectedFormFeatureTerminalId by rememberSaveable(associationOptions) {
        mutableStateOf<Int?>(null)
    }
    var selectedCandidateFeatureTerminalId by rememberSaveable(associationOptions) {
        mutableStateOf<Int?>(null)
    }
    var isContainmentVisible by rememberSaveable(associationOptions) {
        mutableStateOf(false)
    }
    var fractionAlongEdge by rememberSaveable(associationOptions) {
        val initialValue = if (associationOptions?.options?.isFractionAlongEdgeValid == true) {
            0f
        } else {
            null
        }
        mutableStateOf(initialValue)
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddWorkflowTopBar(
                title = stringResource(R.string.new_association),
                subTitle = "",
                onBackPressed = onBackPressed,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    scope.launch {
                        viewModel.addAssociation(
                            isContainmentVisible = isContainmentVisible,
                            fromTerminalId = selectedFormFeatureTerminalId,
                            toTerminalId = selectedCandidateFeatureTerminalId,
                            fractionAlongEdge = fractionAlongEdge
                        ).onSuccess {
                            onAssociationCreated()
                        }.onFailure {
                            snackbarHostState.showSnackbar(
                                message = "Failed to create association. ${it.localizedMessage}",
                                duration = SnackbarDuration.Long,
                                actionLabel = "OK"
                            )
                        }
                    }
                },
                enabled = associationOptions != null,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(text = "Add")
            }
        }
        associationOptions?.let {
            val candidate = it.candidate
            val options = it.options
            val filterType = it.type
            // First show the association type
            // If containment/content show boolean control

            // Association Type
            Card(
                modifier = Modifier.padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                )
            ) {
                PropertyRow(
                    title = stringResource(R.string.association_type),
                    value = filterType.toString(),
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                if (filterType is UtilityAssociationsFilterType.Container
                    || filterType is UtilityAssociationsFilterType.Content
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    ContentVisibleControl(
                        value = isContainmentVisible,
                        enabled = true,
                        onValueChange = { value ->
                            isContainmentVisible = value
                        },
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .fillMaxWidth()
                    )
                }
            }

            // -- Space --

            // Show From Element
            // if terminal config is available show terminal selection
            Card(
                modifier = Modifier.padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                )
            ) {
                PropertyRow(
                    title = stringResource(R.string.from_element),
                    value = viewModel.featureForm.title.collectAsState().value,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                options.formFeatureTerminalConfiguration?.let { terminalConfig ->
                    UtilityTerminalControl(
                        selected = selectedFormFeatureTerminalId?.let { id ->
                            terminalConfig.getTerminalById(id)
                        },
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        options = terminalConfig.terminals,
                        onTerminalSelected = { selected ->
                            selectedFormFeatureTerminalId = selected.terminalId
                        },
                        enabled = true,
                    )
                }
            }

            // Show To Element
            // if terminal config is available show terminal selection
            Card(
                modifier = Modifier.padding(24.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                )
            ) {
                PropertyRow(
                    title = stringResource(R.string.to_element),
                    value = candidate.title,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
                options.candidateFeatureTerminalConfiguration?.let { terminalConfig ->
                    UtilityTerminalControl(
                        selected = selectedCandidateFeatureTerminalId?.let { id ->
                            terminalConfig.getTerminalById(id)
                        },
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        options = terminalConfig.terminals,
                        onTerminalSelected = { selected ->
                            selectedCandidateFeatureTerminalId = selected.terminalId
                        },
                        enabled = true,
                    )
                }
            }

            // If isFractionAlongEdgeValid and no spatial feature show
            // fraction along edge control

            if (options.isFractionAlongEdgeValid) {
                Card(modifier = Modifier.padding(24.dp)) {
                    FractionAlongEdgeControl(
                        fraction = fractionAlongEdge ?: 0f,
                        enabled = true,
                        onValueChanged = { fraction ->
                            fractionAlongEdge = fraction
                        }
                    )
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
