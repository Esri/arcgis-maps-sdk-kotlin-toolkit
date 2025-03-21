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

import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.arcgismaps.toolkit.featureforms.NavigationRoute
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.objectId
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FeatureFormNavHost(
    navController: NavHostController,
    state: FeatureFormState,
    showFormActions: Boolean,
    showCloseIcon: Boolean,
    onSaveForm: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscardForm: (Boolean) -> Unit,
    onDismiss: (() -> Unit)?,
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
            val hasBackStack = remember(formData, navController) {
                navController.previousBackStackEntry != null
            }
            FeatureFormScreen(
                formStateData = formData,
                hasBackStack = hasBackStack,
                isEvaluatingExpressions = state.isEvaluatingExpressions,
                onClose = {
                    onDismiss?.invoke()
                },
                onSave = { willNavigate ->
                    onSaveForm(formData.featureForm, willNavigate).onSuccess {
                        withContext(Dispatchers.Main) {
                            if (willNavigate && hasBackStack && backStackEntry.lifecycleIsResumed()) {
                                // Navigate back to the previous form after saving the edits.
                                state.popBackStack(backStackEntry)
                            }
                        }
                    }
                },
                onDiscard = { willNavigate ->
                    onDiscardForm(willNavigate)
                    if (willNavigate && hasBackStack && backStackEntry.lifecycleIsResumed()) {
                        // Navigate back to the previous form after discarding the edits.
                        state.popBackStack(backStackEntry)
                    }
                },
                onNavigateBack = {
                    // Navigate back to the previous form
                    Log.e(
                        "TAG",
                        "FeatureFormNavHost: back : ${state.popBackStack(backStackEntry)} "
                    )
                },
                onBarcodeButtonClick = onBarcodeButtonClick,
                onUtilityFilterSelected = { state ->
                    val route = NavigationRoute.UNFilterView(
                        stateId = state.id
                    )
                    // Navigate to the filter view
                    if (backStackEntry.lifecycleIsResumed()) {
                        navController.navigate(route)
                    }
                }
            )
            LaunchedEffect(formData) {
                Log.e("TAG", "FormView: ${formData.featureForm.feature.objectId}")
                // Update the active feature form if we navigate back to this screen from another form.
                state.updateActiveFeatureForm()
            }
        }

        composable<NavigationRoute.UNFilterView> { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
            val formData = remember(backStackEntry) { state.getActiveFormStateData() }
            val featureForm = formData.featureForm
            val states = formData.stateCollection
            // Get the selected UtilityAssociationsElementState from the state collection
            val utilityAssociationsElementState = states[route.stateId]
                // guard against null value
                as? UtilityAssociationsElementState ?: return@composable
            // Get the selected filter from the UtilityAssociationsElementState
            val filterResult = utilityAssociationsElementState.selectedFilterResult
            // guard against null value
                ?: return@composable
            UNAssociationsFilterScreen(
                filterResult = filterResult,
                onClose = {
                    onDismiss?.invoke()
                },
                onSave = {
                    onSaveForm(featureForm, false)
                },
                onDiscard = {
                    onDiscardForm(false)
                },
                onNavigateBack = {
                    state.popBackStack(backStackEntry)
                },
                onGroupSelected = { groupResult ->
                    utilityAssociationsElementState.setSelectedGroupResult(groupResult)
                    val newRoute = NavigationRoute.UNAssociationsView(
                        stateId = utilityAssociationsElementState.id
                    )
                    if (backStackEntry.lifecycleIsResumed()) {
                        navController.navigate(newRoute)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(featureForm) {
                Log.e("TAG", "FeatureFormNavHost: FiltersView")
            }
        }

        composable<NavigationRoute.UNAssociationsView> { backStackEntry ->
            val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
            val formData = remember(backStackEntry) { state.getActiveFormStateData() }
            val featureForm = formData.featureForm
            val states = formData.stateCollection
            // Get the selected UtilityAssociationsElementState from the state collection
            val utilityAssociationsElementState = states[route.stateId] as?
                UtilityAssociationsElementState ?: return@composable
            // Get the selected filter from the UtilityAssociationsElementState
            val filterResult = utilityAssociationsElementState.selectedFilterResult
            // Get the selected group from the filter
            val groupResult = utilityAssociationsElementState.selectedGroupResult
            if (filterResult == null || groupResult == null) {
                // guard against null values
                return@composable
            }
            val hasEdits by featureForm.hasEdits.collectAsState()
            UNAssociationsScreen(
                groupResult = groupResult,
                subTitle = filterResult.filter.title,
                showFormActions = showFormActions,
                showCloseIcon = showCloseIcon,
                hasEdits = hasEdits,
                onClose = {
                    onDismiss?.invoke()
                },
                onSave = { willNavigate ->
                    onSaveForm(featureForm, willNavigate)
                },
                onDiscard = { willNavigate ->
                    onDiscardForm(willNavigate)
                },
                onNavigateBack = {
                    state.popBackStack(backStackEntry)
                },
                onNavigateTo = { feature ->
                    state.navigateTo(feature, backStackEntry)
                },
                modifier = Modifier.fillMaxSize()
            )
            FeatureFormDialog(states)
            LaunchedEffect(featureForm) {
                Log.e("TAG", "Associations: ${featureForm.feature.objectId}")
                // Update the active feature form if we navigate back to this screen from another form.
                state.updateActiveFeatureForm()
            }
        }
    }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            Log.e("TAG", "FeatureFormNavHost: ${it.destination.route}")
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
