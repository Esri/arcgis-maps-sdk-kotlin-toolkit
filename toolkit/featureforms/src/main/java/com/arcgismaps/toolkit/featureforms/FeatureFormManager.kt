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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormManagerActionBar
import com.arcgismaps.toolkit.featureforms.internal.editor.ManagerOverview
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormNavigationBar
import com.arcgismaps.toolkit.featureforms.internal.editor.ModalSheet
import com.arcgismaps.toolkit.featureforms.internal.screens.shouldEnableTopBar
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.launch

@Composable
public fun FeatureFormManager(
    state: FeatureFormManagerState,
    modifier: Modifier = Modifier,
    showNavigationBar: Boolean = true,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onDismiss: () -> Unit = {},
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {},
) {
    val featureFormState by rememberUpdatedState(state.featureFormState)
    val navController = rememberNavController(featureFormState)
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val dialogRequester = LocalDialogRequester.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
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
            if (showNavigationBar) {
                FeatureFormNavigationBar(
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
