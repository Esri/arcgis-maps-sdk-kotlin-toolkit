/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureforms

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormInput
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.getState
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.GroupElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationFilter
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociations
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import com.arcgismaps.utilitynetworks.UtilityAssociation
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * The "property" determines the behavior of when the validation errors are visible.
 */
@Deprecated(
    message = "Deprecated without replacement. This property has no effect.",
    level = DeprecationLevel.WARNING
)
@Immutable
public sealed class ValidationErrorVisibility {

    /**
     * Indicates that the validation errors are only visible for editable fields that have
     * received focus.
     */
    @Suppress("DEPRECATION")
    public object Automatic : ValidationErrorVisibility()

    /**
     * Indicates the validation is run for all the editable fields regardless of their focus state,
     * and any errors are shown.
     */
    @Suppress("DEPRECATION")
    public object Visible : ValidationErrorVisibility()
}

/**
 * Indicates an event that occurs during the editing of a feature form.
 */
public sealed class FeatureFormEditingEvent {

    /**
     * Indicates that the edits have been discarded on the [featureForm].
     *
     * @param featureForm The [FeatureForm] that was edited.
     * @param willNavigate Indicates if the form will navigate to another screen after discarding the edits.
     */
    public data class DiscardedEdits(val featureForm: FeatureForm, val willNavigate: Boolean) :
        FeatureFormEditingEvent()

    /**
     * Indicates that the edits have been saved successfully on the [featureForm].
     *
     * @param featureForm The [FeatureForm] that was edited.
     * @param willNavigate Indicates if the form will navigate to another screen after saving the edits.
     */
    public data class SavedEdits(val featureForm: FeatureForm, val willNavigate: Boolean) :
        FeatureFormEditingEvent()
}

@Deprecated(
    message = "Maintained for binary compatibility. Use the overload that accepts a colorScheme and typography.",
    level = DeprecationLevel.HIDDEN
)
@Suppress("DEPRECATION")
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
) {
    FeatureForm(
        featureForm = featureForm,
        modifier = modifier,
        validationErrorVisibility = validationErrorVisibility,
        colorScheme = FeatureFormDefaults.colorScheme(),
        typography = FeatureFormDefaults.typography(),
        onBarcodeButtonClick = null
    )
}

@Deprecated(
    message = "Maintained for binary compatibility. Use the overload that provides the barcode accessory tap callback.",
    level = DeprecationLevel.HIDDEN
)
@Composable
@Suppress("DEPRECATION")
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    FeatureForm(
        featureForm = featureForm,
        modifier = modifier,
        validationErrorVisibility = validationErrorVisibility,
        colorScheme = colorScheme,
        typography = typography,
        onBarcodeButtonClick = null
    )
}

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using a [FeatureForm] that has been configured externally.
 *
 * The [FeatureForm] component supports the following [FormElement] types as part of its configuration.
 * - [AttachmentsFormElement]
 * - [FieldFormElement] with the following [FormInput] types -
 *     * [BarcodeScannerFormInput]
 *     * [ComboBoxFormInput]
 *     * [DateTimePickerFormInput]
 *     * [RadioButtonsFormInput]
 *     * [SwitchFormInput]
 *     * [TextAreaFormInput]
 *     * [TextBoxFormInput]
 * - [GroupFormElement]
 * - [TextFormElement]
 *
 * @param featureForm the [FeatureForm] object to use
 * @param modifier the modifier to apply to this layout.
 * @param validationErrorVisibility This property is deprecated and has no effect.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param colorScheme The [FeatureFormColorScheme] to use for the FeatureForm.
 * @param typography The [FeatureFormTypography] to use for the FeatureForm.
 *
 * @since 200.4.0
 */
@Deprecated(
    message = "Use the overload that uses the FeatureFormState object. This will become an error" +
        " in a future release.",
    level = DeprecationLevel.WARNING
)
@Composable
@Suppress("DEPRECATION")
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val scope = rememberCoroutineScope()
    val states = remember(featureForm) {
        createStates(
            form = featureForm,
            elements = featureForm.elements,
            // Ignore the UtilityAssociationsFormElement as it is not supported with this API
            ignoreList = setOf(
                UtilityAssociationsFormElement::class.java
            ),
            scope = scope
        )
    }
    val state = remember(featureForm) {
        FeatureFormState(
            featureForm = featureForm,
            stateCollection = states,
            coroutineScope = scope
        )
    }
    FeatureForm(
        featureFormState = state,
        modifier = modifier,
        // Hide the close and action bar in the form since it is not supported via this API
        showCloseIcon = false,
        showFormActions = false,
        onBarcodeButtonClick = onBarcodeButtonClick,
        colorScheme = colorScheme,
        typography = typography,
    )
}

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using a [FeatureForm] that has been configured externally. Forms may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)) and can be obtained from either
 * an `ArcGISFeature`, `ArcGISFeatureTable`, `FeatureLayer` or `SubtypeSublayer`.
 *
 * The [FeatureForm] component supports the following [FormElement] types as part of its configuration.
 * - [AttachmentsFormElement]
 * - [FieldFormElement] with the following [FormInput] types -
 *     * [BarcodeScannerFormInput]
 *     * [ComboBoxFormInput]
 *     * [DateTimePickerFormInput]
 *     * [RadioButtonsFormInput]
 *     * [SwitchFormInput]
 *     * [TextAreaFormInput]
 *     * [TextBoxFormInput]
 * - [GroupFormElement]
 * - [TextFormElement]
 * - [UtilityAssociationsFormElement]
 *
 * If there are any edits on the current [FeatureForm] as indicated by [FeatureForm.hasEdits] and
 * the [showFormActions] is true, an action bar is displayed at the top of the form with save and
 * discard buttons. The save button will save the edits using [FeatureForm.finishEditing] and the
 * discard button will discard the edits using [FeatureForm.discardEdits]. The save or discard
 * actions will also trigger the [onEditingEvent] callback with the appropriate event type.
 *
 * The Form is visible as long as it is part of the composition hierarchy. In order to let the user
 * dismiss the Form, the implementation of [onDismiss] should contain a way to remove the form from
 * the composition hierarchy. If the form has edits when the close icon is clicked, the user will be
 * prompted to save or discard the edits before the callback is invoked. The callback is also not
 * invoked if there are validation errors in the form.
 *
 * For any elements of input type [BarcodeScannerFormInput], a default barcode scanner based on MLKit
 * is provided. The scanner requires the [Manifest.permission.CAMERA] permission to be granted.
 * A callback is also provided via the [onBarcodeButtonClick] parameter, which is invoked with
 * the [FieldFormElement] when its barcode accessory is clicked. This can be used to provide a custom
 * barcode scanning experience. Simply call [FieldFormElement.updateValue] with the scanned barcode
 * value to update the field value.
 *
 * For adding any attachments, camera permissions are required. If the permissions are not granted,
 * then the specific functionality is disabled in the form.
 *
 * Any [AttachmentsFormElement] present in the [FeatureForm.elements] collection are not
 * currently supported. A default attachments editing support is provided using the
 * [FeatureForm.defaultAttachmentsElement] property.
 *
 * If any [UtilityAssociationsFormElement] are present in the [FeatureForm.elements] collection,
 * the Form supports navigation between the associated [ArcGISFeature]s and their [FeatureForm]
 *
 * If any [UtilityAssociationsFormElement] is part of the [FeatureForm.elements] collection, the
 * Form will display [UtilityAssociation]s that are associated with the selected feature and allow
 * the user to navigate to the associated feature on the other end of the association. The Android
 * system's back action can be used to navigate back to the previous [FeatureForm] screen. The
 * [FeatureFormState.activeFeatureForm] will be updated when the user navigates forward or back
 * through the associations. If there are any edits on the current [FeatureForm], the user will be
 * prompted to save or discard the edits before navigating to the next [FeatureForm].
 *
 * The colors and typography for the Form can use customized using [FeatureFormColorScheme] and
 * [FeatureFormTypography]. This customization is built on top of [MaterialTheme].
 * If a custom color is specified in both the color scheme and the typography, the color from the
 * color scheme will take precedence and will be merged with the text style, if one is provided.
 *
 * @param featureFormState the [FeatureFormState] object that contains the state of the form.
 * @param modifier the modifier to apply to this layout.
 * @param showCloseIcon Indicates if the close icon should be displayed. If true, the [onDismiss]
 * callback will be invoked when the close icon is clicked. Default is true.
 * @param showFormActions Indicates if the form actions (save and discard buttons) should be displayed.
 * Default is true.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param onDismiss A callback that is invoked when the close icon is visible and is clicked.
 * @param onEditingEvent A callback that is invoked when an editing event occurs in the form. This
 * is triggered when the edits are saved or discarded using the save or discard buttons, respectively.
 * If the edit action is triggered by navigating to another form, the `willNavigate` parameter will
 * be true. Note that if the action happens due to the close button, the `willNavigate` parameter
 * will be false.
 * @param colorScheme The [FeatureFormColorScheme] to use for the FeatureForm.
 * @param typography The [FeatureFormTypography] to use for the FeatureForm.
 *
 * @since 200.7.0
 */
@Composable
public fun FeatureForm(
    featureFormState: FeatureFormState,
    modifier: Modifier = Modifier,
    showCloseIcon: Boolean = true,
    showFormActions: Boolean = true,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onEditingEvent: (FeatureFormEditingEvent) -> Unit = {},
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val state by rememberUpdatedState(featureFormState)
    val navController = rememberNavController(state)
    state.setNavigationCallback {
        navController.navigate(it)
    }
    state.setNavigateBack {
        navController.popBackStack()
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dialogRequester = LocalDialogRequester.current
    val focusManager = LocalFocusManager.current

    // A function that provides the action to save edits on the form
    suspend fun saveForm(form: FeatureForm, willNavigate: Boolean): Result<Unit> {
        focusManager.clearFocus()
        // Check for validation errors
        val errorCount = form.validationErrors.value.entries.count()
        return if (errorCount == 0) {
            // Finish editing the form if there are no validation errors
            form.finishEditing().onSuccess {
                // Send a saved edits event
                val event = FeatureFormEditingEvent.SavedEdits(form, willNavigate)
                onEditingEvent(event)
            }
        } else {
            // Show a dialog with the validation errors if they exist
            val errorDialog = DialogType.ValidationErrorsDialog(
                onDismiss = { },
                title = context.getString(R.string.the_form_has_validation_errors),
                body = context.resources.getQuantityString(
                    R.plurals.you_have_errors_that_must_be_fixed_before_saving,
                    errorCount,
                    errorCount
                )
            )
            dialogRequester.requestDialog(errorDialog)
            Result.failure(Exception("Validation errors found"))
        }
    }

    // A function that provides the action to discard edits on the form
    fun discardForm(form: FeatureForm, willNavigate: Boolean) {
        form.discardEdits()
        // run expressions evaluation
        state.evaluateExpressions()
        // Send a discarded edits event
        val event = FeatureFormEditingEvent.DiscardedEdits(form, willNavigate)
        onEditingEvent(event)
    }

    // A function  that provides the action to dismiss the form
    fun dismissForm(form: FeatureForm, onDismiss: (() -> Unit)?) {
        if (form.hasEdits.value) {
            // Show a dialog to save or discard edits before dismissing the form, if there are edits
            val dialog = DialogType.SaveFeatureDialog(
                onSave = {
                    saveForm(form, false).onSuccess {
                        // Dismiss the form after saving the edits
                        onDismiss?.invoke()
                    }
                },
                onDiscard = {
                    discardForm(form, false)
                    // Dismiss the form after discarding the edits
                    onDismiss?.invoke()
                },
            )
            dialogRequester.requestDialog(dialog)
        } else onDismiss?.invoke()
    }

    FeatureFormTheme(
        colorScheme = colorScheme,
        typography = typography
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
                val featureForm = formData.featureForm
                val states = formData.stateCollection
                val hasBackStack = remember(featureForm, navController) {
                    navController.previousBackStackEntry != null
                }
                val hasEdits by featureForm.hasEdits.collectAsState()
                val onBackAction: () -> Unit = {
                    if (hasEdits) {
                        val dialog = DialogType.SaveFeatureDialog(
                            onSave = {
                                saveForm(featureForm, hasBackStack).onSuccess {
                                    if (hasBackStack) {
                                        // Navigate back to the previous form after saving the edits.
                                        state.popBackStack()
                                    }
                                }
                            },
                            onDiscard = {
                                discardForm(featureForm, hasBackStack)
                                if (hasBackStack) {
                                    // Navigate back to the previous form after discarding the edits.
                                    state.popBackStack()
                                }
                            }
                        )
                        dialogRequester.requestDialog(dialog)
                    } else {
                        if (hasBackStack) {
                            // Navigate back to the previous form if there are no edits
                            state.popBackStack()
                        }
                    }
                }

                FeatureFormLayout(
                    title = {
                        val title by featureForm.title.collectAsState()
                        FeatureFormTitle(
                            title = title,
                            subTitle = featureForm.description,
                            hasEdits = if (showFormActions) hasEdits else false,
                            showCloseIcon = showCloseIcon,
                            modifier = Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = if (hasBackStack) 8.dp else 16.dp
                                )
                                .fillMaxWidth(),
                            onBackPressed = if (hasBackStack) onBackAction else null,
                            onClose = {
                                dismissForm(featureForm, onDismiss)
                            },
                            onSave = {
                                scope.launch { saveForm(featureForm, false) }
                            },
                            onDiscard = { discardForm(featureForm, false) }
                        )
                        InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
                            state.isEvaluatingExpressions
                        }
                    },
                    content = {
                        FormContent(
                            formStateData = formData,
                            onBarcodeButtonClick = onBarcodeButtonClick,
                            onUtilityAssociationFilterClick = { stateId, index ->
                                val route = NavigationRoute.UNFilterView(
                                    stateId = stateId,
                                    selectedFilterIndex = index
                                )
                                // Navigate to the filter view
                                navController.navigate(route)
                            }
                        )
                    }
                )
                // only enable back navigation if there is a previous route
                BackHandler(hasBackStack) {
                    onBackAction()
                }
                FeatureFormDialog(states)
            }

            composable<NavigationRoute.UNFilterView> { backStackEntry ->
                val routeData = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
                val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
                val formData = remember(backStackEntry) { state.getActiveFormStateData() }
                val featureForm = formData.featureForm
                val states = formData.stateCollection
                // Get the selected UtilityAssociationsElementState from the state collection
                val utilityAssociationsElementState = states[routeData.stateId]
                    as? UtilityAssociationsElementState ?: return@composable
                // Get the selected filter from the UtilityAssociationsElementState
                val filterResult = remember(utilityAssociationsElementState.filters) {
                    utilityAssociationsElementState.filters.getOrNull(route.selectedFilterIndex)
                }
                val hasEdits by featureForm.hasEdits.collectAsState()
                FeatureFormLayout(
                    title = {
                        val title by featureForm.title.collectAsState()
                        FeatureFormTitle(
                            title = filterResult?.filter?.title
                                ?: stringResource(R.string.none_selected),
                            subTitle = title,
                            hasEdits = if (showFormActions) hasEdits else false,
                            showCloseIcon = showCloseIcon,
                            modifier = Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 8.dp
                                )
                                .fillMaxWidth(),
                            onBackPressed = navController::popBackStack,
                            onClose = {
                                dismissForm(featureForm, onDismiss)
                            },
                            onSave = {
                                scope.launch { saveForm(featureForm, false) }
                            },
                            onDiscard = { discardForm(featureForm, false) }
                        )
                    },
                    content = {
                        if (filterResult != null) {
                            UtilityAssociationFilter(
                                groupResults = filterResult.groupResults,
                                onGroupClick = { index ->
                                    val newRoute = NavigationRoute.UNAssociationsView(
                                        stateId = utilityAssociationsElementState.id,
                                        selectedFilterIndex = route.selectedFilterIndex,
                                        selectedGroupIndex = index
                                    )
                                    navController.navigate(newRoute)
                                },
                                onBackPressed = navController::popBackStack,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .wrapContentSize()
                            )
                        }
                    }
                )
            }

            composable<NavigationRoute.UNAssociationsView> { backStackEntry ->
                val routeData = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
                val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
                val formData = remember(backStackEntry) { state.getActiveFormStateData() }
                val featureForm = formData.featureForm
                val states = formData.stateCollection
                // Get the selected UtilityAssociationsElementState from the state collection
                val utilityAssociationsElementState = states[routeData.stateId] as?
                    UtilityAssociationsElementState ?: return@composable
                // Get the selected filter from the UtilityAssociationsElementState
                val filter = remember(utilityAssociationsElementState.filters) {
                    utilityAssociationsElementState.filters.getOrNull(route.selectedFilterIndex)
                }
                // Guard against null filter.
                if (filter == null) return@composable
                // Get the selected group from the filter
                val group = remember(utilityAssociationsElementState.filters) {
                    filter.groupResults.getOrNull(route.selectedGroupIndex)
                }
                val hasEdits by featureForm.hasEdits.collectAsState()
                FeatureFormLayout(
                    title = {
                        FeatureFormTitle(
                            title = group?.name ?: stringResource(R.string.none_selected),
                            subTitle = filter.filter.title,
                            hasEdits = if (showFormActions) hasEdits else false,
                            showCloseIcon = showCloseIcon,
                            modifier = Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 8.dp
                                )
                                .fillMaxWidth(),
                            onBackPressed = navController::popBackStack,
                            onClose =  {
                                // Show the close button only if the onDismiss callback is provided
                                dismissForm(featureForm, onDismiss)
                            },
                            onSave = {
                                scope.launch { saveForm(featureForm, false) }
                            },
                            onDiscard = { discardForm(featureForm, false) }
                        )
                    },
                    content = {
                        if (group != null) {
                            UtilityAssociations(
                                groupResult = group,
                                onItemClick = { info ->
                                    if (hasEdits) {
                                        val dialog = DialogType.SaveFeatureDialog(
                                            onSave = {
                                                saveForm(featureForm, true).onSuccess {
                                                    // Navigate to the next form after saving the edits.
                                                    state.navigateTo(info.associatedFeature)
                                                }
                                            },
                                            onDiscard = {
                                                discardForm(featureForm, true)
                                                // Navigate to the next form after discarding the edits.
                                                state.navigateTo(info.associatedFeature)
                                            },
                                        )
                                        dialogRequester.requestDialog(dialog)
                                    } else {
                                        // Navigate to the next form if there are no edits.
                                        state.navigateTo(info.associatedFeature)
                                    }
                                },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                )
                FeatureFormDialog(states)
            }
        }
    }
    DisposableEffect(state) {
        onDispose {
            state.setNavigationCallback(null)
            state.setNavigateBack(null)
        }
    }
}

@Composable
private fun FeatureFormLayout(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        title()
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        content()
    }
}

/**
 * Represents the title bar of the form.
 *
 * @param title The title to display.
 * @param subTitle The subtitle to display.
 * @param hasEdits Indicates if the form has unsaved edits. An unsaved edits indicator is displayed
 * along with the save and discard buttons if this is true.
 * @param onBackPressed The callback to invoke when the back button is clicked. If null, the back
 * button is not displayed.
 * @param onClose The callback to invoke when the close button is clicked. If null, the close button
 * is not displayed.
 * @param onSave The callback to invoke when the save button is clicked.
 * @param onDiscard The callback to invoke when the discard button is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
private fun FeatureFormTitle(
    title: String,
    subTitle: String,
    hasEdits: Boolean,
    showCloseIcon: Boolean,
    onBackPressed: (() -> Unit)?,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (onBackPressed != null) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
            Column {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (hasEdits) {
                        Spacer(Modifier.width(8.dp))
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = Color(0xFFB3261E))
                        }
                    }
                }
                if (subTitle.isNotEmpty()) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (hasEdits) {
                    Text(
                        text = "Unsaved Changes",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showCloseIcon) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "close form")
                }
            }
        }
        AnimatedVisibility(visible = hasEdits) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSave) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onDiscard) {
                    Text(
                        text = stringResource(R.string.discard),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FormContent(
    formStateData: FormStateData,
    modifier: Modifier = Modifier,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityAssociationFilterClick: (Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val lazyListState = rememberSaveable(
        inputs = arrayOf(formStateData),
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }
    // form content
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "lazy column" },
        state = lazyListState
    ) {
        formStateData.stateCollection.forEach { entry ->
            item {
                when (entry.formElement) {
                    is FieldFormElement -> {
                        FieldElement(
                            state = entry.getState<BaseFieldState<*>>(),
                            // set the onClick callback for the field element only if provided
                            onClick = handleFieldFormElementTapAction(
                                fieldFormElement = entry.formElement as FieldFormElement,
                                barcodeTapAction = onBarcodeButtonClick
                            ),
                        )
                    }

                    is GroupFormElement -> {
                        GroupElement(
                            state = entry.getState(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            // set the onClick callback for the group element only if provided
                            onFormElementClick = handleFormElementTapAction(
                                barcodeTapAction = onBarcodeButtonClick
                            )
                        )
                    }

                    is TextFormElement -> {
                        TextFormElement(
                            state = entry.getState(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    }

                    is AttachmentsFormElement -> {
                        AttachmentFormElement(
                            state = entry.getState(),
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    }

                    is UtilityAssociationsFormElement -> {
                        val state = entry.getState<UtilityAssociationsElementState>()
                        UtilityAssociationsElement(
                            state = state,
                            onItemClick = { index ->
                                onUtilityAssociationFilterClick(state.id, index)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                    top = 10.dp,
                                    bottom = 20.dp
                                )
                        )
                    }

                    else -> {
                        // other form elements are not created
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        formStateData.featureForm.hasEdits.collect {
            if (it) {
                val insets = ViewCompat.getRootWindowInsets(view) ?: return@collect
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                with(density) {
                    if (imeVisible) {
                        lazyListState.animateScrollBy(60.dp.toPx())
                    }
                }
            }
        }
    }
}

@Composable
internal fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    val alpha by animateFloatAsState(
        if (evaluationProvider()) 1f else 0f,
        label = "evaluation loading alpha"
    )
    Surface(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        LinearProgressIndicator(modifier)
    }
}

@Composable
internal fun rememberNavController(vararg inputs: Any): NavHostController {
    val context = LocalContext.current
    rememberNavController()
    return rememberSaveable(inputs = inputs, saver = Saver(
        save = { it.saveState() },
        restore = { createNavController(context).apply { restoreState(it) } }
    )) {
        createNavController(context)
    }
}

/**
 * Navigation routes for the form.
 */
@Serializable
internal sealed class NavigationRoute {

    /**
     * Represents the [FeatureForm] view.
     */
    @Serializable
    data object FormView : NavigationRoute()

    /**
     * Represents a view for the [UtilityAssociationFilter].
     */
    @Serializable
    data class UNFilterView(
        val stateId: Int,
        val selectedFilterIndex: Int,
    ) : NavigationRoute()

    /**
     * Represents a view for the [UtilityAssociation]s.
     */
    @Serializable
    data class UNAssociationsView(
        val stateId: Int,
        val selectedFilterIndex: Int,
        val selectedGroupIndex: Int
    ) : NavigationRoute()
}

private fun createNavController(context: Context): NavHostController {
    return NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }
}

/**
 * Handles the tap action for a [FormElement] based on the  input type and the provided tap
 * actions.
 *
 * This will potentially handle taps for any input types that provide custom tap actions.
 *
 * @param fieldFormElement the [FieldFormElement] to handle the tap action for.
 * @param barcodeTapAction the action to perform when the barcode accessory of a [FieldFormElement]
 * with a [BarcodeScannerFormInput] is tapped.
 */
private fun handleFieldFormElementTapAction(
    fieldFormElement: FieldFormElement,
    barcodeTapAction: ((FieldFormElement) -> Unit)?
): (() -> Unit)? {
    return when (fieldFormElement.input) {
        is BarcodeScannerFormInput -> {
            barcodeTapAction?.let {
                { it(fieldFormElement) }
            }
        }

        else -> null
    }
}

/**
 * Handles the tap action for a [FormElement] based on the provided tap actions.
 *
 * This will potentially handle taps for any form element types that provide custom tap actions.
 *
 * @param barcodeTapAction the action to perform when the barcode accessory of a [FieldFormElement]
 * with a [BarcodeScannerFormInput] is tapped.
 */
private fun handleFormElementTapAction(
    barcodeTapAction: ((FieldFormElement) -> Unit)?
): ((FormElement) -> Unit)? {
    return when {
        barcodeTapAction != null -> {
            { formElement ->
                if (formElement is FieldFormElement) {
                    handleFieldFormElementTapAction(formElement, barcodeTapAction)?.invoke()
                }
            }
        }

        else -> null
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFormTitlePreview() {
    FeatureFormTitle(
        title = "Feature Form",
        subTitle = "Edit feature attributes",
        hasEdits = true,
        showCloseIcon = true,
        onBackPressed = null,
        onClose = {},
        onSave = {},
        onDiscard = {},
        modifier = Modifier.padding(8.dp)
    )
}
