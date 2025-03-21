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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.FeatureFormLayout
import com.arcgismaps.toolkit.featureforms.FeatureFormTitle
import com.arcgismaps.toolkit.featureforms.internal.components.dialogs.SaveEditsDialog
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationFilter
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult
import kotlinx.coroutines.launch

/**
 * Composable function that displays the given [filterResult].
 *
 * @param filterResult The filter result.
 * @param subTitle The subtitle to display.
 * @param showFormActions A boolean value that indicates whether to show the form actions.
 * @param showCloseIcon A boolean value that indicates whether to show the close icon.
 * @param hasEdits A boolean value that indicates whether there are edits.
 * @param onClose The callback to be invoked when the close icon is clicked. This is only invoked
 * when there are no edits in the form. If there are edits, this callback is invoked after a successful
 * save or discard operation.
 * @param onSave The callback to be invoked when the save button is clicked. The callback should
 * return a [Result] that indicates the success or failure of the save operation.
 * @param onDiscard The callback to be invoked when the discard button is clicked.
 * @param onNavigateBack The callback to be invoked when the back navigation is requested.
 * @param onGroupSelected The callback that is invoked when a group is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsFilterScreen(
    filterResult: UtilityAssociationsFilterResult,
    onClose: () -> Unit,
    onSave: suspend () -> Result<Unit>,
    onDiscard: () -> Unit,
    onNavigateBack: () -> Unit,
    onGroupSelected: (UtilityAssociationGroupResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showDiscardEditsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingCloseAction by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        UtilityAssociationFilter(
            groupResults = filterResult.groupResults,
            onGroupClick = onGroupSelected,
            onBackPressed = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
            .wrapContentSize()
        )
        if (showDiscardEditsDialog) {
            SaveEditsDialog(
                onDismissRequest = {
                    showDiscardEditsDialog = false
                },
                onSave = {
                    scope.launch {
                        onSave().onSuccess {
                            // Run the pending close action if there is one
                            if (pendingCloseAction) {
                                onClose()
                                pendingCloseAction = false
                            }
                        }
                    }
                    showDiscardEditsDialog = false
                },
                onDiscard = {
                    onDiscard()
                    // Run the pending close action if there is one
                    if (pendingCloseAction) {
                        onClose()
                        pendingCloseAction = false
                    }
                    showDiscardEditsDialog = false
                }
            )
        }
    }
//    FeatureFormLayout(
//        modifier = modifier,
//        title = {
//            FeatureFormTitle(
//                title = filterResult.filter.title,
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
//                        onSave()
//                    }
//                },
//                onDiscard = {
//                    onDiscard()
//                }
//            )
//        },
//        content = {
//
//        }
//    )
}
