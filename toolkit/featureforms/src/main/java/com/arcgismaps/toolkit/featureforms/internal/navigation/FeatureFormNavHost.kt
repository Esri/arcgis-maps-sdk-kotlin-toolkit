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
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.screens.FeatureFormScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsFilterScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsScreen

@Composable
internal fun FeatureFormNavHost(
    navController: NavHostController,
    state: FeatureFormState,
    onSaveForm: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscardForm: suspend (Boolean) -> Unit,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = NavigationRoute.FormView,
        modifier = modifier,
        enterTransition = { slideInHorizontally { h -> h } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { h -> h } }
    ) {
        composable<NavigationRoute.FormView> { backStackEntry ->
            val formData = remember(backStackEntry) { state.getActiveFormStateData() }
            FeatureFormScreen(
                formStateData = formData,
                onBarcodeButtonClick = onBarcodeButtonClick,
                onUtilityFilterSelected = { state ->
                    val route = NavigationRoute.UNFilterView(
                        stateId = state.id
                    )
                    // Navigate to the filter view
                    navController.navigateSafely(backStackEntry, route)
                }
            )
            LaunchedEffect(formData) {
                // Update the active feature form if we navigate back to this screen from another form.
                state.updateActiveFeatureForm()
            }
        }

        composable<NavigationRoute.UNFilterView> { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
            val formData = remember(backStackEntry) { state.getActiveFormStateData() }
            UNAssociationsFilterScreen(
                formStateData = formData,
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
            val formData = remember(backStackEntry) { state.getActiveFormStateData() }
            UNAssociationsScreen(
                formStateData = formData,
                route = route,
                onSave = onSaveForm,
                onDiscard = onDiscardForm,
                onNavigateTo = { feature ->
                    state.navigateTo(feature, backStackEntry)
                },
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(formData) {
                // Update the active feature form when we navigate back to this screen from another
                // form.
                state.updateActiveFeatureForm()
            }
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
