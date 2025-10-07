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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.featureforms.internal.screens.FeatureFormScreen

internal fun NavGraphBuilder.featureFormDestination(
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityFilterSelected: (NavBackStackEntry, Int) -> Unit,
    state : FeatureFormState,
    validationErrorVisibility : ValidationErrorVisibility
) {
    composable<NavigationRoute.Form> { backStackEntry ->
        val formData = remember(backStackEntry) { state.getActiveFormStateData() }
        FeatureFormScreen(
            formStateData = formData,
            onBarcodeButtonClick = onBarcodeButtonClick,
            onUtilityFilterSelected = {
                onUtilityFilterSelected(backStackEntry, it.id)
            }
        )
        LaunchedEffect(formData) {
            // Update the active feature form if we navigate back to this screen from another form.
            state.updateActiveFeatureForm()
        }
        // launch a new side effect in a launched effect when validationErrorVisibility changes
        // for a given form
        LaunchedEffect(validationErrorVisibility, formData) {
            // if it set to always show errors validate all fields
            if (validationErrorVisibility == ValidationErrorVisibility.Visible) {
                state.validateAllFields()
            }
        }
    }
}
