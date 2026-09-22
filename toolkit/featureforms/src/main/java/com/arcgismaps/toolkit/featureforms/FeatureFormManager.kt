/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormManagerActionBar
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormToolbar
import com.arcgismaps.toolkit.featureforms.internal.editor.ManagerOverview
import com.arcgismaps.toolkit.featureforms.internal.editor.ModalSheet
import com.arcgismaps.toolkit.featureforms.internal.screens.shouldEnableTopBar
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.launch

@Composable
public fun FeatureFormManager(
    state: FeatureFormManagerState,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onDismiss: () -> Unit = {},
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {},
) {
    val featureFormState by rememberUpdatedState(state.featureFormState)
    val navController = rememberNavController(featureFormState)
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val dialogRequester = LocalDialogRequester.current
    val scope = rememberCoroutineScope()
    val hasBackStack = currentBackStackEntry != null && navController.previousBackStackEntry != null
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FeatureFormManagerActionBar(
                isVisible = currentBackStackEntry?.shouldEnableTopBar() == true,
                state = state,
                hasBackStack = hasBackStack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onBack = {
                    currentBackStackEntry?.let {
                        featureFormState.popBackStack(it)
                    }
                },
                onSave = {
                    if (state.formsWithErrors.value > 0) {
                        val errorCount = state.formsWithErrors.value
                        val errorDialog = DialogType.ValidationErrorsDialog(
                            onDismiss = state::validateAllForms,
                            onAction = {
                                state.showOverview()
                            },
                            title = "There are validation errors",
                            body = "There are $errorCount forms with validation errors. Please correct the errors before saving.",
                            actionText = "View Errors",
                        )
                        dialogRequester.requestDialog(errorDialog)
                    } else {
                        Log.e("TAG", "FeatureFormManager: saving form", )
                        scope.launch {
                            state.saveForm().onFailure {

                            }
                        }
                    }
                },
                onDiscard = {
                    scope.launch {
                        state.discardEdits()
                    }
                },
                onDismiss = onDismiss
            )
            FeatureForm(
                featureFormState = featureFormState,
                navController = navController,
                modifier = Modifier.weight(1f),
                showCloseIcon = false,
                showBackAction = false,
                showFormActions = false,
                showTopBar = false,
                allowNavigationWithEdits = true,
                onShowOnMapRequest = onShowOnMapRequest,
                onDismiss = onDismiss,
                onNavigationEvent = state::setCurrentFeatureFormRoute,
                validationErrorVisibility = validationErrorVisibility
            )
            if (showToolbar) {
                FeatureFormToolbar(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        ModalSheet(
            visible = state.showOverview,
            onDismiss = state::hideOverview
        ) {
            val forms by state.featureForms.collectAsState()
            ManagerOverview(
                forms = forms,
                errorCount = state.formsWithErrors.collectAsState().value,
                modifier = Modifier.fillMaxSize(),
                onShowOnMapRequest = onShowOnMapRequest,
                onDismiss = state::hideOverview,
                onNavigateToForm = { featureForm ->
                    state.navigateToForm(featureForm)
                    state.hideOverview()
                },
                onRemoveForm = { featureForm -> }
            )
        }
    }

    // only enable back navigation if there is a previous route
    BackHandler(hasBackStack) {
        Log.e("TAG", "FeatureFormManagerActionBar: backhandler", )
        currentBackStackEntry?.let {
            featureFormState.popBackStack(it)
        }
    }
}
