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

package com.arcgismaps.toolkit.popup.internal.screens

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
import com.arcgismaps.toolkit.popup.PopupStateData
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociations
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationAction
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationRoute

/**
 * Screen that displays the selected group of associations.
 *
 * @param popupStateData The form state data.
 * @param route The [NavigationRoute.UNAssociationsView] route data of this screen.
 * @param onSave The callback to be invoked when the save button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation. The callback should
 * return a [Result] that indicates the success or failure of the save operation.
 * @param onDiscard The callback to be invoked when the discard button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation.
 * @param onNavigateToFeature The callback to be invoked when the user selects a feature to navigate to.
 * @param onNavigateToAssociation The callback to be invoked when the user selects an association to navigate to.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsScreen(
    popupStateData: PopupStateData,
    route: NavigationRoute.UNAssociationsView,
//    isNavigationEnabled: Boolean,
//    onSave: suspend (FeatureForm, Boolean) -> Result<Unit>,
//    onDiscard: suspend (Boolean) -> Unit,
    onNavigateToFeature: (ArcGISFeature) -> Unit,
    onNavigateToAssociation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val popup = popupStateData.popup
    val states = popupStateData.stateCollection
    // Get the selected UtilityAssociationsElementState from the state collection
    val utilityAssociationsElementState = states[route.stateId] as?
        UtilityAssociationsElementState ?: return
    // Get the selected filter from the UtilityAssociationsElementState
    val filterResult = utilityAssociationsElementState.selectedFilterResult
    // Get the selected group from the filter
    val groupResult = utilityAssociationsElementState.selectedGroupResult
    if (filterResult == null || groupResult == null) {
        // guard against null values
        return
    }
//    val hasEdits by popup.hasEdits.collectAsState()
//    val scope = rememberCoroutineScope()
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
    UtilityAssociations(
        groupResult = groupResult,
        isNavigationEnabled = true,
        onItemClick = { index ->
//            if (hasEdits) {
//                pendingNavigationAction = NavigationAction.NavigateToFeature(index)
//            } else {
                val feature = groupResult.associationResults[index].associatedFeature
                // Navigate to the next form if there are no edits.
                onNavigateToFeature(feature)
//            }
        },
        onDetailsClick = { index ->
            val association = groupResult.associationResults[index]
            utilityAssociationsElementState.setSelectedAssociationResult(association)
            onNavigateToAssociation(utilityAssociationsElementState.id)
        },
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    )
//    if (pendingNavigationAction != NavigationAction.None) {
//        SaveEditsDialog(
//            onDismissRequest = {
//                // Clear the pending navigation action when the dialog is dismissed
//                pendingNavigationAction = NavigationAction.None
//            },
//            onSave = {
//                scope.launch {
//                    onSave(featureForm, true).onSuccess {
//                        // If the save is successful, navigate to the association
//                        navigateToFeature(pendingNavigationAction)
//                    }
//                    pendingNavigationAction = NavigationAction.None
//                }
//            },
//            onDiscard = {
//                scope.launch {
//                    onDiscard(true)
//                    // Navigate to the association after discarding changes
//                    navigateToFeature(pendingNavigationAction)
//                    pendingNavigationAction = NavigationAction.None
//                }
//            }
//        )
//    }
//    FeatureFormDialog(states)
}
