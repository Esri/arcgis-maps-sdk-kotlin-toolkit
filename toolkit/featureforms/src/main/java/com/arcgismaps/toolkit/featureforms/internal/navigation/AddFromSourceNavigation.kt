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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.FeatureFormNavigationRoute
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.screens.CreateAssociationScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.SelectAssetTypeScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.SelectAssociatedFeatureScreen
import com.arcgismaps.toolkit.featureforms.internal.screens.SelectNetworkSourceScreen

internal fun NavGraphBuilder.selectSourceDestination(
    onBackPressed: (NavBackStackEntry) -> Unit,
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry,
    onSourceSelected: (NavBackStackEntry) -> Unit,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit,
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
                    featureForm = formData.featureForm,
                    filter = state.selectedFilterResult!!.filterResult.filter,
                    onAssociationAdded = {
                        state.refreshResults()
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
            LaunchedEffect(state) {
                val eventData = FeatureFormNavigationRoute.SelectAssociationFeatureSource(
                    element = state.element
                )
                onNavigationEvent(eventData)
            }
        }
    }
}

internal fun NavGraphBuilder.selectAssetTypeDestination(
    onAssetTypeSelected: (NavBackStackEntry) -> Unit,
    onBackPressed: (NavBackStackEntry) -> Unit,
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit
) {
    composable<AddFromSourceNavRoute.SelectAssetType> { backStackEntry ->
        val parent = remember(backStackEntry) {
            onGetParentEntry(backStackEntry)
        }
        val viewModel: AddAssociationFromSourceViewModel = viewModel(parent)
        SelectAssetTypeScreen(
            viewModel = viewModel,
            onBackPressed = {
                onBackPressed(backStackEntry)
            },
            onAssetTypeSelected = {
                onAssetTypeSelected(backStackEntry)
            },
            modifier = Modifier.fillMaxSize()
        )
        LaunchedEffect(viewModel.selectedSource) {
            viewModel.selectedSource?.let { source ->
                // only send event if we have a selected source
                val eventData = FeatureFormNavigationRoute.SelectUtilityAssetType(
                    element = viewModel.element,
                    featureSource = source
                )
                onNavigationEvent(eventData)
            }
        }
    }
}

internal fun NavGraphBuilder.selectFeatureDestination(
    onBackPressed: (NavBackStackEntry) -> Unit,
    onFeatureCandidateSelected: (NavBackStackEntry) -> Unit,
    onFeatureCandidateLocateRequest: (ArcGISFeature) -> Unit,
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit
) {
    composable<AddFromSourceNavRoute.SelectAssociatedFeature> { backStackEntry ->
        val parent = remember(backStackEntry) {
            onGetParentEntry(backStackEntry)
        }
        val viewModel: AddAssociationFromSourceViewModel = viewModel(parent)
        SelectAssociatedFeatureScreen(
            viewModel = viewModel,
            onBackPressed = {
                onBackPressed(backStackEntry)
            },
            onFeatureCandidateSelected = {
                onFeatureCandidateSelected(backStackEntry)
            },
            onFeatureCandidateLocateRequest = onFeatureCandidateLocateRequest,
            modifier = Modifier.fillMaxSize()
        )
        LaunchedEffect(viewModel.selectedAssetType) {
            viewModel.selectedSource?.let { source ->
                viewModel.selectedAssetType?.let { assetType ->
                    val eventData =
                        FeatureFormNavigationRoute.SelectAssociationFeatureCandidate(
                            element = viewModel.element,
                            featureSource = source,
                            assetType = assetType
                        )
                    onNavigationEvent(eventData)
                }
            }
        }
    }
}

internal fun NavGraphBuilder.createAssociationDestination(
    onAssociationCreated: (NavBackStackEntry) -> Unit,
    onBackPressed: (NavBackStackEntry) -> Unit,
    onGetParentEntry: (NavBackStackEntry) -> NavBackStackEntry,
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit
) {
    composable<AddFromSourceNavRoute.CreateAssociation>(
        enterTransition = { slideInVertically { h -> h } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutVertically { h -> h } }
    ) { backStackEntry ->
        val parent = remember(backStackEntry) {
            onGetParentEntry(backStackEntry)
        }
        val viewModel: AddAssociationFromSourceViewModel = viewModel(parent)
        CreateAssociationScreen(
            viewModel = viewModel,
            onAssociationCreated = {
                onAssociationCreated(backStackEntry)
            },
            onBackPressed = {
                onBackPressed(backStackEntry)
            },
            modifier = Modifier.fillMaxSize()
        )
        LaunchedEffect(viewModel.newAssociationOptions) {
            viewModel.selectedSource?.let { source ->
                viewModel.newAssociationOptions?.let { options ->
                    val eventData = FeatureFormNavigationRoute.CreateAssociation(
                        element = viewModel.element,
                        featureSource = source,
                        candidate = options.candidate
                    )
                    onNavigationEvent(eventData)
                }
            }
        }
    }
}

internal fun NavHostController.navigateToAddUNAssociationFromSource(
    backStackEntry: NavBackStackEntry,
    stateId: Int
) {
    val newRoute = NavigationRoute.AddUNAssociationFromSource(stateId = stateId)
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

internal fun NavHostController.navigateToCreateAssociation(
    backStackEntry: NavBackStackEntry
) {
    val newRoute = AddFromSourceNavRoute.CreateAssociation
    navigateSafely(backStackEntry, newRoute)
}
