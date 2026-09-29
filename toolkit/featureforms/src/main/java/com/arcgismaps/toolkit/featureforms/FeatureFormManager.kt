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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormManagerActionBar
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormToolbar
import com.arcgismaps.toolkit.featureforms.internal.editor.ManagerOverview
import com.arcgismaps.toolkit.featureforms.internal.editor.ModalSheet
import com.arcgismaps.toolkit.featureforms.internal.navigation.FormNavigationDirection
import com.arcgismaps.toolkit.featureforms.internal.screens.shouldEnableTopBar
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import kotlinx.coroutines.launch

/**
 * Indicates an event that occurs during the editing session of a [FeatureFormManager].
 *
 * @since 300.2.0
 */
public sealed class FeatureFormManagerEditingEvent {

    /**
     * Indicates that the edits have been discarded.
     */
    public data object DiscardedEdits: FeatureFormManagerEditingEvent()

    /**
     * Indicates that the edits have been saved successfully.
     */
    public data object SavedEdits: FeatureFormManagerEditingEvent()
}

/**
 * TODO: Add documentation for FeatureFormManager
 */
@Composable
public fun FeatureFormManager(
    state: FeatureFormManagerState,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onDismiss: () -> Unit = {},
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {},
    onEditingEvent: (FeatureFormManagerEditingEvent) -> Unit = {},
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography()
) {
    val featureFormState by rememberUpdatedState(state.featureFormState)
    val navController = rememberNavController(featureFormState)
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val dialogRequester = LocalDialogRequester.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val hasBackStack = currentBackStackEntry != null && navController.previousBackStackEntry != null
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            FeatureFormManagerActionBar(
                isVisible = currentBackStackEntry?.shouldEnableTopBar() == true,
                state = state,
                hasBackStack = hasBackStack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                onBack = {
                    currentBackStackEntry?.let {
                        featureFormState.popBackStack(it)
                    }
                },
                onSave = {
                    // If there are validation errors and more than one form, show a dialog with
                    // the option to view the errors in the overview screen.
                    if (state.formsWithErrors.value > 0 && state.featureFormStates.size > 1) {
                        val errorCount = state.formsWithErrors.value
                        val errorDialog = DialogType.ValidationErrorsDialog(
                            onDismiss = state::validateAllForms,
                            onAction = {
                                state.showOverview()
                            },
                            title = resources.getString(R.string.there_are_validation_errors),
                            body = resources.getQuantityString(
                                R.plurals.form_manager_has_validation_errors,
                                errorCount,
                                errorCount
                            ),
                            actionText = resources.getString(R.string.view_errors),
                        )
                        dialogRequester.requestDialog(errorDialog)
                    } else if (state.formsWithErrors.value > 0) {
                        // If there are validation errors and only one form, show a dialog to inform
                        // the user that they need to fix the errors before saving.
                        val errorCount = state.featureFormStates.first().featureForm.elementValidationErrors.value.size
                        val errorDialog = DialogType.ValidationErrorsDialog(
                            onDismiss = state::validateAllForms,
                            onAction = null,
                            title = resources.getString(R.string.there_are_validation_errors),
                            body = resources.getQuantityString(
                                R.plurals.you_have_errors_that_must_be_fixed_before_saving,
                                errorCount,
                                errorCount
                            ),
                            actionText = "",
                        )
                        dialogRequester.requestDialog(errorDialog)
                    } else {
                        Log.e("TAG", "FeatureFormManager: saving form", )
                        scope.launch {
                            state.finishEditing().onSuccess {
                                Log.e("TAG", "FeatureFormManager: saved", )
                                onEditingEvent(FeatureFormManagerEditingEvent.SavedEdits)
                            }.onFailure {
                                val errorDialog = DialogType.ValidationErrorsDialog(
                                    onDismiss = {},
                                    onAction = null,
                                    title = resources.getString(R.string.error_saving_edits),
                                    body = resources.getString(
                                        R.string.an_error_occurred_while_saving_the_edits,
                                        it.localizedMessage
                                    ),
                                    actionText = "",
                                )
                                dialogRequester.requestDialog(errorDialog)
                            }
                        }
                    }
                },
                onDiscard = {
                    scope.launch {
                        state.discardEdits()
                        onEditingEvent(FeatureFormManagerEditingEvent.DiscardedEdits)
                    }
                },
                onDismiss = onDismiss
            )
            FeatureForm(
                featureFormState = featureFormState,
                navController = navController,
                modifier = Modifier.weight(1f),
                showCloseIcon = false,
                showFormActions = false,
                showTopBar = false,
                allowNavigationWithEdits = true,
                onShowOnMapRequest = onShowOnMapRequest,
                onDismiss = onDismiss,
                onNavigationEvent = state::setCurrentFeatureFormRoute,
                validationErrorVisibility = validationErrorVisibility,
                isNavigationEnabled = true,
                onBarcodeButtonClick = onBarcodeButtonClick,
                colorScheme = colorScheme,
                typography = typography
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
            val forms = state.featureFormStates
            ManagerOverview(
                forms = forms,
                editable = state.isEditable,
                errorCount = state.formsWithErrors.collectAsState().value,
                modifier = Modifier.fillMaxSize(),
                onShowOnMapRequest = onShowOnMapRequest,
                onDismiss = state::hideOverview,
                onNavigateToForm = { featureForm ->
                    state.navigateToForm(featureForm, FormNavigationDirection.Default)
                    state.hideOverview()
                },
                onRemoveForm = { TODO() }
            )
        }
    }

    // only enable back navigation if there is a previous route
    BackHandler(hasBackStack) {
        currentBackStackEntry?.let {
            featureFormState.popBackStack(it)
        }
    }
}
