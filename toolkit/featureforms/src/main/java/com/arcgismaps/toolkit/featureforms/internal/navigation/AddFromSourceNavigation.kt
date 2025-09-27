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
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.screens.SelectAssociatedFeatureScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.SelectNetworkSourceScreen

internal fun NavGraphBuilder.selectSourceDestination(
    onSourceSelected: (NavBackStackEntry) -> Unit,
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry,
    onBackPressed: (NavBackStackEntry) -> Unit,
    state: FeatureFormState
) {
    composable<AddFromSourceNavRoute.SelectSource>(
        enterTransition = { slideInVertically { h -> h } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutVertically { h -> h } }
    ) { backStackEntry ->
        val parent = remember(backStackEntry) {
            onGetParentEntry(backStackEntry)
        }
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        val stateId = backStackEntry.arguments?.getInt("stateId") ?: -1
        val state = formData.stateCollection[stateId] as? UtilityAssociationsElementState
        if (state != null) {
            val viewModel: AddAssociationFromSourceViewModel = viewModel(
                viewModelStoreOwner = parent,
                factory = AddAssociationFromSourceViewModel.Factory(
                    element = state.element,
                    filter = state.selectedFilterResult!!.filter,
                    onAssociationAdded = {
                    }
                )
            )
            SelectNetworkSourceScreen(
                viewModel = viewModel,
                onNetworkSourceSelected = {
                    onSourceSelected(backStackEntry)
                },
                onBackPressed = {
                    onBackPressed(backStackEntry)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

internal fun NavGraphBuilder.selectAssetTypeDestination(
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry
) {
    composable<AddFromSourceNavRoute.SelectAssetType> { backStackEntry ->
        val parent = remember(backStackEntry) {
            onGetParentEntry(backStackEntry)
        }
        val viewModel: AddAssociationFromSourceViewModel = viewModel(parent)
        Text(text = backStackEntry.destination.route.toString())
    }
}

internal fun NavGraphBuilder.selectFeatureDestination(
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry,
    onBackPressed: (NavBackStackEntry) -> Unit
) {
    composable<AddFromSourceNavRoute.SelectAssociatedFeature> { backStackEntry ->
        val parent = remember(backStackEntry) {
            onGetParentEntry(backStackEntry)
        }
        SelectAssociatedFeatureScreen(
            viewModel = viewModel(parent),
            onBackPressed = {
                onBackPressed(backStackEntry)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal fun NavHostController.navigateToAddUNAssociationFromSourceView(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.AddUNAssociationFromSourceView(stateId = stateId)
    // Navigate to the add from source view
    navigateSafely(backStackEntry, newRoute)
}

internal fun NavHostController.navigateToSelectAssetType(
    backStackEntry: NavBackStackEntry
) {
    val newRoute = AddFromSourceNavRoute.SelectAssetType
    navigateSafely(backStackEntry, newRoute)
}

internal fun NavHostController.navigateToSelectAssociatedFeature(
    backStackEntry: NavBackStackEntry
) {
    val newRoute = AddFromSourceNavRoute.SelectAssociatedFeature
    navigateSafely(backStackEntry, newRoute)
}
