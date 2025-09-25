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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.screens.UNAssociationsFilterResultScreen

internal fun NavGraphBuilder.associationsFilterResultDestination(
    onGroupSelected: (NavBackStackEntry, Int) -> Unit,
    onAddFromSourceClick: (NavBackStackEntry, Int) -> Unit,
    state: FeatureFormState,
) {
    composable<NavigationRoute.UNFilterView> { backStackEntry ->
        val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        UNAssociationsFilterResultScreen(
            formStateData = formData,
            route = route,
            onGroupSelected = { stateId ->
                onGroupSelected(backStackEntry, stateId)
            },
            onAddFromSourceClick = { stateId ->
                onAddFromSourceClick(backStackEntry, stateId)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal fun NavHostController.navigateToAssociationsFilterResultView(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.UNFilterView(stateId = stateId)
    // Navigate to the filter view
    navigateSafely(backStackEntry, newRoute)
}
