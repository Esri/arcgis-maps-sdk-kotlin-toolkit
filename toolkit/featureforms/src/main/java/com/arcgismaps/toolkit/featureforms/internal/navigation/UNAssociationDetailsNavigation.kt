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

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.arcgismaps.toolkit.featureforms.internal.screens.UtilityAssociationDetailsScreen

internal fun NavGraphBuilder.associationDetailsDestination(
    onDeleteAssociation: (isGroupEmpty: Boolean) -> Unit,
    onClose: (NavBackStackEntry) -> Unit,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit,
    state: FeatureFormState,
) {
    composable<NavigationRoute.UNAssociationDetails>(
        enterTransition = { slideInVertically { h -> h } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutVertically { h -> h } }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<NavigationRoute.UNAssociationGroupResult>()
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        val states = formData.stateCollection
        // Get the selected UtilityAssociationsElementState from the state collection
        val utilityAssociationsElementState =
            states[route.stateId] as? UtilityAssociationsElementState
        if (utilityAssociationsElementState != null) {
            UtilityAssociationDetailsScreen(
                onDelete = onDeleteAssociation,
                onClose = {
                    onClose(backStackEntry)
                },
                state = utilityAssociationsElementState,
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(utilityAssociationsElementState.selectedAssociationResult) {
                utilityAssociationsElementState.selectedAssociationResult?.let { result ->
                    val eventData = FeatureFormNavigationRoute.AssociationResult(
                        element = utilityAssociationsElementState.element,
                        associationResult = result
                    )
                    onNavigationEvent(eventData)
                }
            }
        }
    }
}

internal fun NavHostController.navigateToUNAssociationDetails(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.UNAssociationDetails(stateId = stateId)
    navigateSafely(backStackEntry, newRoute)
}
