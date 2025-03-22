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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.internal.components.dialogs.SaveEditsDialog
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociations
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import kotlinx.coroutines.launch

/**
 * Composable function that displays the given [groupResult] and its associations.
 *
 * @param groupResult The group result.
 * @param subTitle The subtitle to display.
 * @param showFormActions A boolean value that indicates whether to show the form actions.
 * @param showCloseIcon A boolean value that indicates whether to show the close icon.
 * @param hasEdits A boolean value that indicates whether there are edits.
 * @param onClose The callback to be invoked when the close icon is clicked. This is only invoked
 * when there are no edits in the form. If there are edits, this callback is invoked after a successful
 * save or discard operation.
 * @param onSave The callback to be invoked when the save button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation. The callback should
 * return a [Result] that indicates the success or failure of the save operation.
 * @param onDiscard The callback to be invoked when the discard button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation.
 * @param onNavigateBack The callback to be invoked when the back navigation is requested.
 * @param onNavigateTo The callback to be invoked when the user selects an association to navigate to.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsScreen(
    groupResult: UtilityAssociationGroupResult,
    subTitle: String,
    showFormActions: Boolean,
    showCloseIcon: Boolean,
    hasEdits: Boolean,
    onClose: () -> Unit,
    onSave: suspend (Boolean) -> Result<Unit>,
    onDiscard: suspend (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateTo: (ArcGISFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showDiscardEditsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingCloseAction by rememberSaveable {
        mutableStateOf(false)
    }
    var selectedAssociationIndex by rememberSaveable {
        mutableStateOf<Int?>(null)
    }
//    FeatureFormLayout(
//        modifier = modifier,
//        title = {
//            FeatureFormTitle(
//                title = groupResult.name,
//                subTitle = subTitle,
//                hasEdits = if (showFormActions) hasEdits else false,
//                showCloseIcon = showCloseIcon,
//                modifier = Modifier
//                    .padding(
//                        vertical = 8.dp,
//                        horizontal = 8.dp
//                    )
//                    .fillMaxWidth(),
//                onBackPressed = onNavigateBack,
//                onClose = {
//                    if (hasEdits) {
//                        pendingCloseAction = true
//                        showDiscardEditsDialog = true
//                    } else {
//                        onClose()
//                    }
//                },
//                onSave = {
//                    scope.launch {
//                        onSave(false)
//                    }
//                },
//                onDiscard = {
//                    onDiscard(false)
//                }
//            )
//        },
//        content = {
//        }
//    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        UtilityAssociations(
            groupResult = groupResult,
            onItemClick = { index ->
                if (hasEdits) {
                    selectedAssociationIndex = index
                    showDiscardEditsDialog = true
                } else {
                    val feature = groupResult.associationResults[index].associatedFeature
                    // Navigate to the next form if there are no edits.
                    onNavigateTo(feature)
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
    if (showDiscardEditsDialog) {
        SaveEditsDialog(
            onDismissRequest = {
                showDiscardEditsDialog = false
                pendingCloseAction = false
            },
            onSave = {
                scope.launch {
                    val willNavigate = !pendingCloseAction
                    onSave(willNavigate).onSuccess {
                        // If this action is followed by a close action, close the form.
                        if (pendingCloseAction) {
                            onClose()
                            pendingCloseAction = false
                        } else {
                            // If this action is followed by a forward navigation, navigate to the next form.
                            selectedAssociationIndex?.let { index ->
                                groupResult.associationResults.getOrNull(index)?.associatedFeature?.let { feature ->
                                    onNavigateTo(feature)
                                }
                            }
                        }
                    }
                }
                showDiscardEditsDialog = false
            },
            onDiscard = {
                val willNavigate = !pendingCloseAction
                scope.launch {
                    onDiscard(willNavigate)
                }
                // If this action is followed by a close action, close the form.
                if (pendingCloseAction) {
                    onClose()
                    pendingCloseAction = false
                } else {
                    // If this action is followed by a forward navigation, navigate to the next form.
                    selectedAssociationIndex?.let { index ->
                        groupResult.associationResults.getOrNull(index)?.associatedFeature?.let { feature ->
                            onNavigateTo(feature)
                        }
                    }
                }
                showDiscardEditsDialog = false
            }
        )
    }
}
