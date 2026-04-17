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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureCandidate
import com.arcgismaps.mapping.featureforms.UtilityAssociationFeatureSource
import com.arcgismaps.toolkit.featureforms.internal.components.mlkit.FeatureFormGenerativeModel
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.voice.VoiceToForm
import com.arcgismaps.toolkit.featureforms.internal.navigation.FeatureFormNavHost
import com.arcgismaps.toolkit.featureforms.internal.screens.ContentAwareTopBar
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import com.arcgismaps.utilitynetworks.UtilityAssetType
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilter
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult
import kotlinx.coroutines.launch

/**
 * Defines the visibility behavior of validation errors in a [FeatureForm].
 *
 * @since 200.4.0
 */
@Immutable
public sealed class ValidationErrorVisibility {

    /**
     * Indicates that the validation errors are only visible for editable fields that have
     * received focus.
     */
    public object Automatic : ValidationErrorVisibility()

    /**
     * Indicates the validation is run for all the editable fields regardless of their focus state,
     * and any errors are shown.
     */
    public object Visible : ValidationErrorVisibility()
}

/**
 * Indicates an event that occurs during the editing of a feature form.
 *
 * @since 200.8.0
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

/**
 * Indicates the navigation route within a [FeatureForm] when dealing with [UtilityAssociation]s.
 *
 * @since 300.0.0
 */
public sealed class FeatureFormNavigationRoute {

    /**
     * Indicates the route for the main [FeatureForm] screen. The associated [FeatureForm] can be
     * obtained from the [FeatureFormState.activeFeatureForm] property.
     *
     * @since 300.0.0
     */
    public data object Form : FeatureFormNavigationRoute()

    /**
     * Indicates the route for [UtilityAssociationsFilterResult] screen.
     *
     * @param element The [UtilityAssociationsFormElement] associated with the filter result.
     * @param filterResult The selected [UtilityAssociationsFilterResult].
     *
     * @since 300.0.0
     */
    public data class AssociationsFilterResult(
        val element: UtilityAssociationsFormElement,
        val filterResult: UtilityAssociationsFilterResult
    ) : FeatureFormNavigationRoute()

    /**
     * Indicates the route for [UtilityAssociationGroupResult] screen.
     *
     * @param element The [UtilityAssociationsFormElement] associated with the group result.
     * @param filter The selected [UtilityAssociationsFilter].
     * @param groupResult The selected [UtilityAssociationGroupResult].
     *
     * @since 300.0.0
     */
    public data class AssociationGroupResult(
        val element: UtilityAssociationsFormElement,
        val filter: UtilityAssociationsFilter,
        val groupResult: UtilityAssociationGroupResult
    ) : FeatureFormNavigationRoute()

    /**
     * Indicates the route for [UtilityAssociationResult] details screen.
     *
     * @param element The [UtilityAssociationsFormElement] associated with the association result.
     * @param result The selected [UtilityAssociationResult].
     *
     * @since 300.0.0
     */
    public data class AssociationResult(
        val element: UtilityAssociationsFormElement,
        val result: UtilityAssociationResult
    ) : FeatureFormNavigationRoute()

    /**
     * Indicates the route for selecting a [UtilityAssociationFeatureSource] when adding a new
     * association.
     *
     * @param element The [UtilityAssociationsFormElement] where the association is being added.
     * @param filter The selected [UtilityAssociationsFilter].
     *
     * @since 300.0.0
     */
    public data class SelectAssociationFeatureSource(
        val element: UtilityAssociationsFormElement,
        val filter : UtilityAssociationsFilter
    ) : FeatureFormNavigationRoute()

    /**
     * Indicates the route for selecting a [UtilityAssetType] from a [UtilityAssociationFeatureSource]
     * when adding a new association.
     *
     * @param element The [UtilityAssociationsFormElement] where the association is being added.
     * @param filter The selected [UtilityAssociationsFilter].
     * @param featureSource The selected [UtilityAssociationFeatureSource].
     *
     * @since 300.0.0
     */
    public data class SelectUtilityAssetType(
        val element: UtilityAssociationsFormElement,
        val filter : UtilityAssociationsFilter,
        val featureSource: UtilityAssociationFeatureSource
    ) : FeatureFormNavigationRoute()

    /**
     * Indicates the route for selecting a [UtilityAssociationFeatureCandidate] from a
     * [UtilityAssetType] when adding a new association.
     *
     * @param element The [UtilityAssociationsFormElement] where the association is being added.
     * @param filter The selected [UtilityAssociationsFilter].
     * @param featureSource The selected [UtilityAssociationFeatureSource] the asset type belongs to.
     * @param assetType The selected [UtilityAssetType].
     *
     * @since 300.0.0
     */
    public data class SelectAssociationFeatureCandidate(
        val element: UtilityAssociationsFormElement,
        val filter : UtilityAssociationsFilter,
        val featureSource: UtilityAssociationFeatureSource,
        val assetType: UtilityAssetType
    ) : FeatureFormNavigationRoute()

    /**
     * Indicates the route for creating a new association with the selected
     * [UtilityAssociationFeatureCandidate].
     *
     * @param element The [UtilityAssociationsFormElement] where the association is being added.
     * @param filter The selected [UtilityAssociationsFilter].
     * @param featureSource The selected [UtilityAssociationFeatureSource] the candidate belongs to.
     * @param candidate The selected [UtilityAssociationFeatureCandidate].
     *
     * @since 300.0.0
     */
    public data class CreateAssociation(
        val element: UtilityAssociationsFormElement,
        val filter : UtilityAssociationsFilter,
        val featureSource: UtilityAssociationFeatureSource,
        val candidate: UtilityAssociationFeatureCandidate
    ) : FeatureFormNavigationRoute()
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
 * prompted to save or discard the edits before navigating to the next [FeatureForm]. [UtilityAssociation]s
 * can also be created or deleted when the [UtilityAssociationsFormElement.isEditable] property is
 * true.
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
 * [UtilityAssociationsFormElement]s present. When true, the user can navigate to associated features
 * and back. If false, this navigation is disabled. If there are geometry edits on the feature,
 * this flag can be set to false to prevent navigation until the geometry edits are saved or discarded.
 * Default is true.
 * @param validationErrorVisibility The [ValidationErrorVisibility] that determines the behavior of
 * when the validation errors are visible. Default is [ValidationErrorVisibility.Automatic] which
 * indicates errors are only visible once the respective field gains focus.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param onShowOnMapRequest A callback that is invoked when a request to highlight a feature is made.
 * Invoked when the locate icon is tapped on a [UtilityAssociationFeatureCandidate] inside a
 * [UtilityAssociationsFormElement] during new association candidate selection. This can be used to
 * highlight the feature in the map view, helping visually confirm the correct feature to associate.
 * Note that this in only invoked for spatial features that have a geometry.
 * @param onDismiss A callback that is invoked when the close icon is visible and is clicked.
 * @param onEditingEvent A callback that is invoked when an editing event occurs in the form. This
 * is triggered when the edits are saved or discarded using the save or discard buttons, respectively.
 * If the edit action is triggered by navigating to another form, the `willNavigate` parameter will
 * be true. Note that if the action happens due to the close button, the `willNavigate` parameter
 * will be false.
 * @param onNavigationEvent A callback that is invoked when a navigation event occurs in the form.
 * This is triggered when the user navigates to different screens within the form, currently when
 * dealing with [UtilityAssociation]s. The specific [FeatureFormNavigationRoute] is provided which
 * contains the relevant data for the route.
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
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    onShowOnMapRequest : (ArcGISFeature) -> Unit = {},
    onDismiss: () -> Unit = {},
    onEditingEvent: (FeatureFormEditingEvent) -> Unit = {},
    onNavigationEvent: (FeatureFormNavigationRoute) -> Unit = {},
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val state by rememberUpdatedState(featureFormState)
    val scope = rememberCoroutineScope()
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
    suspend fun saveForm(state: FeatureFormState, willNavigate: Boolean): Result<Unit> {
        val form = state.getActiveFormStateData().featureForm
        focusManager.clearFocus()
        // Check for validation errors
        val errorCount = form.validationErrors.value.entries.count()
        return if (errorCount == 0) {
            // Finish editing the form if there are no validation errors
            state.saveEdits().onSuccess {
                // Send a saved edits event if the save was successful
                val event = FeatureFormEditingEvent.SavedEdits(form, willNavigate)
                onEditingEvent(event)
            }
        } else {
            // Show a dialog with the validation errors if they exist
            val errorDialog = DialogType.ValidationErrorsDialog(
                onDismiss = {
                    state.validateAllFields()
                },
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
    // Get the current back stack entry as state
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the form data for the active entry (destination) in the back stack
    val formData = remember(backStackEntry) { state.getActiveFormStateData() }
    FeatureFormLayout(
        topBar = {
            // Track if there is a back stack entry
            val hasBackStack = remember(backStackEntry) {
                navController.previousBackStackEntry != null
            }
            var showVoiceInput by rememberSaveable { mutableStateOf(false) }
            val model = remember(formData) {
                FeatureFormGenerativeModel(formData)
            }
            backStackEntry?.let { entry ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    ContentAwareTopBar(
                        backStackEntry = entry,
                        state = state,
                        onSaveForm = { willNavigate ->
                            saveForm(state, willNavigate)
                        },
                        onDiscardForm = ::discardForm,
                        onDismissRequest = onDismiss,
                        onVoice = {
                            showVoiceInput = true
                        },
                        hasBackStack = hasBackStack,
                        showFormActions = showFormActions,
                        showCloseIcon = showCloseIcon,
                        showVoiceIcon = !showVoiceInput,
                        isNavigationEnabled = isNavigationEnabled,
                        modifier = Modifier
                            .padding(
                                vertical = 8.dp,
                                horizontal = if (hasBackStack) 8.dp else 16.dp
                            )
                            .fillMaxWidth(),
                    )
                    AnimatedVisibility(showVoiceInput) {
                        VoiceToForm(
                            model = model,
                            onDismiss = { showVoiceInput = false },
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        )
                    }
                }
            }
        },
        content = {
            FeatureFormNavHost(
                navController = navController,
                state = state,
                isNavigationEnabled = isNavigationEnabled,
                validationErrorVisibility = validationErrorVisibility,
                onSaveForm = { willNavigate ->
                    saveForm(state, willNavigate)
                },
                onDiscardForm = ::discardForm,
                onBarcodeButtonClick = onBarcodeButtonClick,
                onShowOnMapRequest = onShowOnMapRequest,
                onNavigationEvent = onNavigationEvent,
                modifier = Modifier.fillMaxSize()
            )
        },
        overlay = {
//            var showVoiceInput by rememberSaveable { mutableStateOf(false) }
//            FloatingActionButton(
//                onClick = {
//                    showVoiceInput = true
//                }
//            ) {
//                Icon(Icons.Default.Mic, contentDescription = "Voice input")
//            }
//            if (showVoiceInput) {
//                VoiceToForm()
//            }
        },
        modifier = modifier,
        colorScheme = colorScheme,
        typography = typography
    )
    FeatureFormDialog(states = formData.stateCollection)
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
    overlay: @Composable BoxScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    colorScheme: FeatureFormColorScheme,
    typography: FeatureFormTypography
) {
    FeatureFormTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        Box(modifier = modifier) {
            Column(modifier = Modifier.fillMaxSize()) {
                topBar()
                content()
            }
            overlay()
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
