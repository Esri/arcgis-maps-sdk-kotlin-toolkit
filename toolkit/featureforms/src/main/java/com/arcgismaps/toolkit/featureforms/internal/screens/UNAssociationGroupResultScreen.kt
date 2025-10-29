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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.internal.components.dialogs.SaveEditsDialog
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationGroupResult
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction
import kotlinx.coroutines.launch

/**
 * Screen that displays the selected group of associations.
 *
 * @param state The state of the utility associations element.
 * @param featureForm The feature form associated with the current feature.
 * @param isNavigationEnabled Flag indicating whether navigation to associated features is enabled.
 * @param onSave The callback to be invoked when the save button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation. The callback should
 * return a [Result] that indicates the success or failure of the save operation.
 * @param onDiscard The callback to be invoked when the discard button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation.
 * @param onNavigateToFeature The callback to be invoked when the user selects a feature to navigate to.
 * @param onBack The callback to invoke when the back action is triggered.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationGroupResultScreen(
    state: UtilityAssociationsElementState,
    featureForm: FeatureForm,
    isNavigationEnabled: Boolean,
    onSave: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscard: suspend (Boolean) -> Unit,
    onNavigateToAssociation : () -> Unit,
    onNavigateToFeature: (ArcGISFeature) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupResult = state.selectedGroupResult ?: return
    val hasEdits by featureForm.hasEdits.collectAsState()
    val isEditable by state.isEditable.collectAsState()
    val scope = rememberCoroutineScope()
    // State to hold the pending navigation action when the form has unsaved edits
    var pendingNavigationAction: NavigationAction by rememberSaveable {
        mutableStateOf(NavigationAction.None)
    }
    // Handler for navigating to a selected associated feature
    val navigateToFeature: (NavigationAction) -> Unit = { action ->
        if (action is NavigationAction.NavigateToFeature) {
            val selectedIndex = action.index
            groupResult.associationResults.getOrNull(selectedIndex)?.associatedFeature?.let { feature ->
                onNavigateToFeature(feature)
            }
        }
    }
    UtilityAssociationGroupResult(
        groupResult = groupResult,
        isEditable = isEditable,
        isNavigationEnabled = isNavigationEnabled,
        onItemClick = { index ->
            if (hasEdits) {
                pendingNavigationAction = NavigationAction.NavigateToFeature(index)
            } else {
                val feature = groupResult.associationResults[index].associatedFeature
                // Navigate to the next form if there are no edits.
                onNavigateToFeature(feature)
            }
        },
        onDetailsClick = { index ->
            val association = groupResult.associationResults[index]
            state.setSelectedAssociationResult(association)
            // show the details sheet
            // showDetails = true
            onNavigateToAssociation()
        },
        onDelete = { isGroupEmpty ->
            if (isGroupEmpty) {
                // If the group is empty after deletion, navigate back to the filter view
                onBack()
            }
        },
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    )
    if (pendingNavigationAction != NavigationAction.None) {
        SaveEditsDialog(
            onDismissRequest = {
                // Clear the pending navigation action when the dialog is dismissed
                pendingNavigationAction = NavigationAction.None
            },
            onSave = {
                scope.launch {
                    onSave(featureForm, true).onSuccess {
                        // If the save is successful, navigate to the association
                        navigateToFeature(pendingNavigationAction)
                    }
                    pendingNavigationAction = NavigationAction.None
                }
            },
            onDiscard = {
                scope.launch {
                    onDiscard(true)
                    // Navigate to the association after discarding changes
                    navigateToFeature(pendingNavigationAction)
                    pendingNavigationAction = NavigationAction.None
                }
            }
        )
    }
}
