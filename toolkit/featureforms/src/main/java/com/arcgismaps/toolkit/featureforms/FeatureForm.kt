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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormInput
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.navigation.FeatureFormNavHost
import com.arcgismaps.toolkit.featureforms.internal.screens.ContentAwareTopBar
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import com.arcgismaps.utilitynetworks.UtilityAssociation

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
    // Hold the list of form elements.
    val formElements: List<FormElement> = remember(featureForm) {
        // Add the default attachments element, if present.
        featureForm.elements + listOfNotNull(featureForm.defaultAttachmentsElement)
    }
    val states = remember(featureForm) {
        createStates(
            form = featureForm,
            elements = formElements,
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
 * If you are providing your own save and discard buttons, be sure to use the [FeatureFormState.discardEdits]
 * to discard the edits. This will ensure the data and attachments in the form are updated correctly.
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
 * @param isNavigationEnabled Indicates if the navigation is enabled for the form when there are
 * [UtilityAssociationsFormElement]s present. If true, the user can navigate to associated features
 * and back. If false, this navigation is disabled. If there are geometry edits on the feature,
 * this flag can be set to false to prevent navigation until the geometry edits are saved or discarded.
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
 * @since 200.8.0
 */
@Composable
public fun FeatureForm(
    featureFormState: FeatureFormState,
    modifier: Modifier = Modifier,
    showCloseIcon: Boolean = true,
    showFormActions: Boolean = true,
    isNavigationEnabled : Boolean = true,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    onEditingEvent: (FeatureFormEditingEvent) -> Unit = {},
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val state by rememberUpdatedState(featureFormState)
    val navController = rememberNavController(state)
    state.setNavigationCallback { route ->
        navController.navigate(route)
    }
    state.setNavigateBack {
        navController.navigateUp()
    }
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
                // Send a saved edits event if the save was successful
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
    suspend fun discardForm(willNavigate: Boolean) {
        state.discardEdits()
        // Send a discarded edits event
        val event = FeatureFormEditingEvent.DiscardedEdits(
            state.getActiveFormStateData().featureForm,
            willNavigate
        )
        onEditingEvent(event)
    }

    FeatureFormLayout(
        topBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            // Track if there is a back stack entry
            val hasBackStack = remember(backStackEntry) {
                navController.previousBackStackEntry != null
            }
            backStackEntry?.let { entry ->
                ContentAwareTopBar(
                    backStackEntry = entry,
                    state = state,
                    onSaveForm = ::saveForm,
                    onDiscardForm = ::discardForm,
                    onDismissRequest = onDismiss,
                    hasBackStack = hasBackStack,
                    showFormActions = showFormActions,
                    showCloseIcon = showCloseIcon,
                    isNavigationEnabled = isNavigationEnabled,
                    modifier = Modifier
                        .padding(
                            vertical = 8.dp,
                            horizontal = if (hasBackStack) 8.dp else 16.dp
                        )
                        .fillMaxWidth(),
                )
            }
        },
        content = {
            FeatureFormNavHost(
                navController = navController,
                state = state,
                isNavigationEnabled = isNavigationEnabled,
                onSaveForm = ::saveForm,
                onDiscardForm = ::discardForm,
                onBarcodeButtonClick = onBarcodeButtonClick,
                modifier = Modifier.fillMaxSize()
            )
        },
        modifier = modifier,
        colorScheme = colorScheme,
        typography = typography
    )
    DisposableEffect(state) {
        onDispose {
            // Clear the navigation actions when the composition is disposed
            state.setNavigationCallback(null)
            state.setNavigateBack(null)
        }
    }
}

@Composable
internal fun FeatureFormLayout(
    topBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    colorScheme: FeatureFormColorScheme,
    typography: FeatureFormTypography
) {
    FeatureFormTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        Column(modifier = modifier) {
            topBar()
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
            content()
        }
    }
}

@Composable
internal fun rememberNavController(vararg inputs: Any): NavHostController {
    val context = LocalContext.current
    rememberNavController()
    return rememberSaveable(
        inputs = inputs, saver = Saver(
            save = { it.saveState() },
            restore = { createNavController(context).apply { restoreState(it) } }
        )) {
        createNavController(context)
    }
}

private fun createNavController(context: Context): NavHostController {
    return NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }
}
