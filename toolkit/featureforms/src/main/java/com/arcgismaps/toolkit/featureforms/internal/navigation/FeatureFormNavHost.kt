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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navigation
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility

@Composable
internal fun FeatureFormNavHost(
    navController: NavHostController,
    state: FeatureFormState,
    isNavigationEnabled: Boolean,
    validationErrorVisibility: ValidationErrorVisibility,
    onSaveForm: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscardForm: suspend (Boolean) -> Unit,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onFeatureLocateRequest: (ArcGISFeature) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = NavigationRoute.Form,
        modifier = modifier,
        enterTransition = { slideInHorizontally { h -> h } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { h -> h } }
    ) {

        featureFormDestination(
            state = state,
            onBarcodeButtonClick = onBarcodeButtonClick,
            onUtilityFilterSelected = navController::navigateToUNAssociationsFilterResult,
            validationErrorVisibility = validationErrorVisibility
        )

        associationsFilterResultDestination(
            onGroupSelected =  navController::navigateToUNAssociationGroupResult,
            onAddFromSourceClick = navController::navigateToAddUNAssociationFromSource,
            state = state
        )

        associationGroupResultDestination(
            state = state,
            onSave = onSaveForm,
            onDiscard = onDiscardForm,
            isNavigationEnabled = isNavigationEnabled,
            onNavigateToFeature =  state::navigateTo,
            onBack =  navController::popBackStack,
        )

        navigation<NavigationRoute.AddUNAssociationFromSource>(
            startDestination = AddFromSourceNavRoute.SelectSource,
        ) {
            selectSourceDestination(
                onBackPressed = state::popBackStack,
                onGetParentEntry = {
                    navController.getBackStackEntry(it.destination.parent!!.id)
                },
                onSourceSelected = navController::navigateToSelectAssetType,
                state = state
            )

            selectAssetTypeDestination(
                onAssetTypeSelected = navController::navigateToSelectAssociatedFeature,
                onBackPressed = state::popBackStack,
                onGetParentEntry = {
                    navController.getBackStackEntry(it.destination.parent!!.id)
                }
            )

            selectFeatureDestination(
                onBackPressed = state::popBackStack,
                onFeatureCandidateSelected = { backStackEntry ->
                  navController.navigateToCreateAssociation(backStackEntry)
                },
                onFeatureCandidateLocateRequest = onFeatureLocateRequest,
                onGetParentEntry = {
                    navController.getBackStackEntry(it.destination.parent!!.id)
                }
            )

            createAssociationDestination(
                onAssociationCreated = {
                    navController.popBackStack<NavigationRoute.UNAssociationsFilterResult>(inclusive = false)
                },
                onBackPressed = state::popBackStack,
                onGetParentEntry = {
                    navController.getBackStackEntry(it.destination.parent!!.id)
                }
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
