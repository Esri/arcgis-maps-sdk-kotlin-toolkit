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
import com.arcgismaps.toolkit.featureforms.FeatureFormNavigationRoute
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsFilterResultScreen

internal fun NavGraphBuilder.associationsFilterResultDestination(
    onGroupSelected: (NavBackStackEntry, Int) -> Unit,
    onAddFromSourceClick: (NavBackStackEntry, Int) -> Unit,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit,
    state: FeatureFormState,
) {
    composable<NavigationRoute.UNAssociationsFilterResult> { backStackEntry ->
        val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsFilterResult>()
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        val states = formData.stateCollection
        // Get the selected UtilityAssociationsElementState from the state collection
        val utilityAssociationsElementState = states[route.stateId]
            // guard against null value
            as? UtilityAssociationsElementState
        // Get the selected filter from the UtilityAssociationsElementState
        val filterResult = utilityAssociationsElementState?.selectedFilterResult
        // guard against null value
        if (filterResult != null) {
            UNAssociationsFilterResultScreen(
                state = utilityAssociationsElementState,
                onGroupSelected = { stateId ->
                    onGroupSelected(backStackEntry, stateId)
                },
                onAddFromSourceClick = { stateId ->
                    onAddFromSourceClick(backStackEntry, stateId)
                },
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(filterResult) {
                val eventData = FeatureFormNavigationRoute.FilterResult(
                    element = utilityAssociationsElementState.element,
                    utilityAssociationsFilterResult = filterResult.filterResult
                )
                onNavigationEvent(eventData)
            }
        }
    }
}

internal fun NavHostController.navigateToUNAssociationsFilterResult(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.UNAssociationsFilterResult(stateId = stateId)
    // Navigate to the filter view
    navigateSafely(backStackEntry, newRoute)
}
