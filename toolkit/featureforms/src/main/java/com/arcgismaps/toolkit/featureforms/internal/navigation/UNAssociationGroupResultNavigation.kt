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

package com.arcgismaps.toolkit.featureforms.internal.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormNavigationRoute
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationGroupResultScreen

internal fun NavGraphBuilder.associationGroupResultDestination(
    state: FeatureFormState,
    onSave: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscard: suspend (Boolean) -> Unit,
    onNavigateToAssociation : (NavBackStackEntry, Int) -> Unit,
    onNavigateToFeature: (NavBackStackEntry, ArcGISFeature) -> Unit,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit,
    onAssociatedFeatureLocateRequest: (ArcGISFeature) -> Unit,
    onBack: (NavBackStackEntry) -> Unit,
    isNavigationEnabled: Boolean,
) {
    composable<NavigationRoute.UNAssociationGroupResult> { backStackEntry ->
        val route = backStackEntry.toRoute<NavigationRoute.UNAssociationGroupResult>()
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        val states = formData.stateCollection
        // Get the selected UtilityAssociationsElementState from the state collection
        val utilityAssociationsElementState =
            states[route.stateId] as? UtilityAssociationsElementState
        // Get the selected group from the filter
        val groupResult = utilityAssociationsElementState?.selectedGroupResult
        if (groupResult != null) {
            UNAssociationGroupResultScreen(
                state = utilityAssociationsElementState,
                featureForm = formData.featureForm,
                isNavigationEnabled = isNavigationEnabled,
                onSave = onSave,
                onDiscard = onDiscard,
                onNavigateToAssociation = {
                    onNavigateToAssociation(backStackEntry, utilityAssociationsElementState.id)
                },
                onNavigateToFeature = { feature ->
                    onNavigateToFeature(backStackEntry, feature)
                },
                onAssociatedFeatureLocateRequest = onAssociatedFeatureLocateRequest,
                onBack = {
                    onBack(backStackEntry)
                },
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(formData) {
                // Update the active feature form when we navigate back to this screen from another
                // form.
                state.updateActiveFeatureForm()
            }
            LaunchedEffect(groupResult) {
                val eventData = FeatureFormNavigationRoute.AssociationGroupResult(
                    element = utilityAssociationsElementState.element,
                    filter = utilityAssociationsElementState.selectedFilterResult!!.filter,
                    groupResult = groupResult.groupResult
                )
                onNavigationEvent(eventData)
            }
        } else {
            // If we don't have a valid state or group, navigate back to the previous screen.
            // This could happen if the group was deleted.
            state.popBackStack(backStackEntry)
        }
    }
}

internal fun NavHostController.navigateToUNAssociationGroupResult(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.UNAssociationGroupResult(stateId = stateId)
    navigateSafely(backStackEntry, newRoute)
}
