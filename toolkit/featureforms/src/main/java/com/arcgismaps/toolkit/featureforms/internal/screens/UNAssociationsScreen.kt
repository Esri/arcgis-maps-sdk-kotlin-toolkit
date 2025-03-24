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
import com.arcgismaps.toolkit.featureforms.FormStateData
import com.arcgismaps.toolkit.featureforms.internal.components.dialogs.SaveEditsDialog
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociations
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationRoute
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import kotlinx.coroutines.launch

/**
 * Screen that displays the selected group of associations.
 *
 * @param formStateData The form state data.
 * @param route The [NavigationRoute.UNAssociationsView] route data of this screen.
 * @param onSave The callback to be invoked when the save button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation. The callback should
 * return a [Result] that indicates the success or failure of the save operation.
 * @param onDiscard The callback to be invoked when the discard button is clicked. The boolean parameter
 * indicates whether this action should be followed by a forward navigation.
 * @param onNavigateTo The callback to be invoked when the user selects an association to navigate to.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsScreen(
    formStateData: FormStateData,
    route : NavigationRoute.UNAssociationsView,
    onSave: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscard: suspend (Boolean) -> Unit,
    onNavigateTo: (ArcGISFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    val featureForm = formStateData.featureForm
    val states = formStateData.stateCollection
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
    val hasEdits by featureForm.hasEdits.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingNavigationAction: NavigationAction by rememberSaveable {
        mutableStateOf(NavigationAction.None)
    }
    val onNavigationAction: (NavigationAction) -> Unit = { action ->
        if (action is NavigationAction.NavigateToAssociation) {
            val selectedIndex = action.index
            groupResult.associationResults.getOrNull(selectedIndex)?.associatedFeature?.let { feature ->
                onNavigateTo(feature)
            }
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        UtilityAssociations(
            groupResult = groupResult,
            onItemClick = { index ->
                if (hasEdits) {
                    pendingNavigationAction = NavigationAction.NavigateToAssociation(index)
                } else {
                    val feature = groupResult.associationResults[index].associatedFeature
                    // Navigate to the next form if there are no edits.
                    onNavigateTo(feature)
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
    if (pendingNavigationAction != NavigationAction.None) {
        SaveEditsDialog(
            onDismissRequest = {
                pendingNavigationAction = NavigationAction.None
            },
            onSave = {
                scope.launch {
                    onSave(featureForm, true).onSuccess {
                        onNavigationAction(pendingNavigationAction)
                    }
                    pendingNavigationAction = NavigationAction.None
                }
            },
            onDiscard = {
                scope.launch {
                    onDiscard(true)
                    onNavigationAction(pendingNavigationAction)
                    pendingNavigationAction = NavigationAction.None
                }
            }
        )
    }
    FeatureFormDialog(states)
}
