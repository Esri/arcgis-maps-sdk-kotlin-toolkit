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

package com.arcgismaps.toolkit.popup.internal.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.arcgismaps.toolkit.popup.PopupState
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationDetails
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.screens.PopupScreen
import com.arcgismaps.toolkit.popup.internal.screens.UNAssociationsFilterScreen
import com.arcgismaps.toolkit.popup.internal.screens.UNAssociationsScreen

@Composable
internal fun PopupNavHost(
    navController: NavHostController,
    state: PopupState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = NavigationRoute.PopupView,
        modifier = modifier,
        enterTransition = { slideInHorizontally { h -> h } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { h -> h } }
    ) {
        composable<NavigationRoute.PopupView> { backStackEntry ->
            val popupStateData = remember(backStackEntry) { state.getActivePopupStateData() }
            PopupScreen(
                state,
                popupStateData,
                popupStateData.initialEvaluation.value,
                -1,
                onUtilityFilterSelected = { state ->
                    val newRoute = NavigationRoute.UNFilterView(stateId = state.id)
                    // Navigate to the filter view
                    navController.navigateSafely(backStackEntry, newRoute)
                },
                modifier
            )
            LaunchedEffect(popupStateData) {
                // Update the active feature form if we navigate back to this screen from another form.
                state.updateActivePopup()
            }
        }

        composable<NavigationRoute.UNFilterView> { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
            val formData = remember(backStackEntry) { state.getActivePopupStateData() }
            UNAssociationsFilterScreen(
                popupStateData = formData,
                route = route,
                onGroupSelected = { stateId ->
                    val newRoute = NavigationRoute.UNAssociationsView(stateId = stateId)
                    navController.navigateSafely(backStackEntry, newRoute)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable<NavigationRoute.UNAssociationsView> { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
            val formData = remember(backStackEntry) { state.getActivePopupStateData() }
            UNAssociationsScreen(
                popupStateData = formData,
                route = route,
                onNavigateToFeature = { feature ->
                    // Request the state to navigate to the feature.
                    state.navigateTo(backStackEntry, feature)
                },
                onNavigateToAssociation = { stateId ->
                    val route = NavigationRoute.UNAssociationDetailView(stateId = stateId)
                    // Request the state to navigate to the association.
                    navController.navigateSafely(backStackEntry, route)
                },
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(formData) {
                // Update the active popup when we navigate back to this screen from another
                // popup.
                state.updateActivePopup()
            }
        }

        composable<NavigationRoute.UNAssociationDetailView> { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.UNAssociationDetailView>()
            val formData = remember(backStackEntry) { state.getActivePopupStateData() }
            // Get the selected UtilityAssociationsElementState from the state collection
            val utilityAssociationsElementState = formData.stateCollection[route.stateId]
                // guard against null value
                as? UtilityAssociationsElementState ?: return@composable
            // Display the association details
            UtilityAssociationDetails(
                state = utilityAssociationsElementState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
internal fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED

/**
 * Navigate to the given route only if the lifecycle of the [backStackEntry] is resumed.
 */
internal fun <T : Any> NavHostController.navigateSafely(
    backStackEntry: NavBackStackEntry,
    route: T
): Boolean {
    return if (backStackEntry.lifecycleIsResumed()) {
        this.navigate(route)
        true
    } else false
}
