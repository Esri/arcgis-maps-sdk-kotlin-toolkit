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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormInput
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.rememberAttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.rememberBarcodeTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseGroupState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.MutableFormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.getState
import com.arcgismaps.toolkit.featureforms.internal.components.base.rememberBaseGroupState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.rememberComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.rememberRadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.rememberSwitchFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.rememberDateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.GroupElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.rememberFormTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.rememberTextFormElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.Associations
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationFilter
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogRequester
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

/**
 * The "property" determines the behavior of when the validation errors are visible.
 */
@Deprecated(
    message = "Deprecated without replacement",
    level = DeprecationLevel.WARNING
)
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

@Serializable
internal sealed class NavigationRoute {

    @Serializable
    data object FormView : NavigationRoute()

    @Serializable
    data class UNFilterView(
        val stateId: Int,
        val selectedFilterIndex: Int
    ) : NavigationRoute()

    @Serializable
    data class UNAssociationsView(
        val stateId: Int,
        val selectedFilterIndex: Int,
        val selectedGroupIndex: Int
    ) : NavigationRoute()
}

@Deprecated(
    message = "Maintained for binary compatibility. Use the overload that accepts a colorScheme and typography.",
    level = DeprecationLevel.HIDDEN
)
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

@Deprecated(
    message = "Use the overload that uses the FeatureFormState object",
    level = DeprecationLevel.WARNING
)
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val scope = rememberCoroutineScope()
    val states = rememberStates(
        form = featureForm,
        elements = featureForm.elements,
        scope = scope
    )
    val state = remember(featureForm) {
        FeatureFormState(
            featureForm = featureForm,
            stateCollection = states,
            coroutineScope = scope
        )
    }
    FeatureForm(
        state = state,
        modifier = modifier,
        colorScheme = colorScheme,
        typography = typography,
        onBarcodeButtonClick = onBarcodeButtonClick
    )
}

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using forms that have been configured externally. Forms may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
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
 * The colors and typography for the Form can use customized using [FeatureFormColorScheme] and
 * [FeatureFormTypography]. This customization is built on top of [MaterialTheme].
 * If a custom color is specified in both the color scheme and the typography, the color from the
 * color scheme will take precedence and will be merged with the text style, if one is provided.
 *
 * @param state the [FeatureFormState] object that contains the state of the form.
 * @param modifier the modifier to apply to this layout.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param colorScheme The [FeatureFormColorScheme] to use for the FeatureForm.
 * @param typography The [FeatureFormTypography] to use for the FeatureForm.
 *
 * @since 200.7.0
 */
@Composable
public fun FeatureForm(
    state: FeatureFormState,
    modifier: Modifier = Modifier,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val navController = rememberNavController(state)
    state.setNavController(navController)
    val dialogRequester = LocalDialogRequester.current
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
                val stateData = remember(backStackEntry) { state.getActiveStateData() }
                val featureForm = stateData.featureForm
                val states = stateData.stateCollection
                val hasBackStack = remember(featureForm) { state.hasBackStack() }
                val onBack: (FeatureFormState) -> Unit = { state ->
                    val dialog = createSaveEditsDialog(
                        featureForm = featureForm,
                        navigationAction = {
                            state.popBackStack()
                        },
                        onDismiss = {},
                        dialogRequester = dialogRequester
                    )
                    dialogRequester.requestDialog(dialog)
                }
                FormContent(
                    form = featureForm,
                    states = states,
                    evaluatingExpressions = state.evaluatingExpressions,
                    onBarcodeButtonClick = onBarcodeButtonClick,
                    onUtilityAssociationFilterClick = { stateId, index ->
                        val route = NavigationRoute.UNFilterView(
                            stateId = stateId,
                            selectedFilterIndex = index
                        )
                        navController.navigate(route)
                    },
                    onBackPressed = {
                        onBack(state)
                    },
                    showBackButton = hasBackStack
                )
                // only enable back navigation if there is a previous route
                BackHandler(hasBackStack) {
                    onBack(state)
                }
                FeatureFormDialog(states)
//                // launch a new side effect in a launched effect when validationErrorVisibility changes
//                LaunchedEffect(state.validationErrorVisibility) {
//                    // if it set to always show errors force each field to validate itself and show any errors
//                    if (state.validationErrorVisibility == ValidationErrorVisibility.Visible) {
//                        states.forEach { entry ->
//                            // validate all fields
//                            if (entry.formElement is FieldFormElement) {
//                                entry.getState<BaseFieldState<*>>().forceValidation()
//                            }
//                            // validate any fields that are within a group
//                            if (entry.formElement is GroupFormElement) {
//                                entry.getState<BaseGroupState>().fieldStates.forEach { childEntry ->
//                                    childEntry.getState<BaseFieldState<*>>().forceValidation()
//                                }
//                            }
//                        }
//                    }
//                }
            }

            composable<NavigationRoute.UNFilterView> { backStackEntry ->
                val routeData = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
                val stateData = remember(backStackEntry) { state.getActiveStateData() }
                val unState =
                    stateData.stateCollection[routeData.stateId] as? UtilityAssociationsElementState
                        ?: return@composable
                val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
                val title by stateData.featureForm.title.collectAsState()
                val filters by unState.filters
                val filter = remember(filters) { filters.getOrNull(route.selectedFilterIndex) }
                if (filter == null) return@composable
                UtilityAssociationFilter(
                    filter = filter,
                    subTitle = title,
                    onGroupClick = { index ->
                        val newRoute = NavigationRoute.UNAssociationsView(
                            stateId = unState.id ,
                            selectedFilterIndex = route.selectedFilterIndex,
                            selectedGroupIndex = index
                        )
                        navController.navigate(newRoute)
                    },
                    onBackPressed = navController::popBackStack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable<NavigationRoute.UNAssociationsView> { backStackEntry ->
                val routeData = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
                val stateData = remember(backStackEntry) { state.getActiveStateData() }
                val unState =
                    stateData.stateCollection[routeData.stateId] as? UtilityAssociationsElementState
                        ?: return@composable
                val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
                val filters by unState.filters
                val filter = remember(filters) { filters.getOrNull(route.selectedFilterIndex) }
                if (filter == null) return@composable
                val group = remember(filters) { filter.groups.getOrNull(route.selectedGroupIndex) }
                if (group == null) return@composable
                Associations(
                    state = group,
                    onItemClick = { info ->
                        val dialog = createSaveEditsDialog(
                            featureForm = stateData.featureForm,
                            navigationAction = {
                                state.navigateTo(FeatureForm(info.associatedFeature))
                            },
                            onDismiss = {},
                            dialogRequester = dialogRequester
                        )
                        dialogRequester.requestDialog(dialog)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
                FeatureFormDialog(stateData.stateCollection)
            }
        }
    }
    DisposableEffect(state) {
        onDispose {
            state.setNavController(null)
        }
    }
}

@Composable
private fun FeatureFormTitle(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    showBackButton: Boolean
) {
    val title by featureForm.title.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {
        if (showBackButton) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Navigate back")
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun FormContent(
    form: FeatureForm,
    states: FormStateCollection,
    evaluatingExpressions: Boolean,
    modifier: Modifier = Modifier,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityAssociationFilterClick: (Int, Int) -> Unit,
    onBackPressed: () -> Unit,
    showBackButton: Boolean
) {
    //var initialEvaluation by rememberSaveable(form) { mutableStateOf(false) }
    val lazyListState = rememberSaveable(inputs = arrayOf(form), saver = LazyListState.Saver) {
        LazyListState()
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // title
        FeatureFormTitle(
            featureForm = form,
            modifier = Modifier
                .padding(
                    vertical = if (showBackButton) 8.dp else 16.dp,
                    horizontal = if (showBackButton) 8.dp else 16.dp
                )
                .fillMaxWidth(),
            onBackPressed = onBackPressed,
            showBackButton = showBackButton
        )
        InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
            evaluatingExpressions
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        // form content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "lazy column" },
            state = lazyListState
        ) {
            states.forEach { entry ->
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
                                onFilterClick = { index ->
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
    }
//    LaunchedEffect(form) {
//        // ensure expressions are evaluated
//        Log.e("TAG", "FormContent: running exp", )
//        form.evaluateExpressions()
//        initialEvaluation = true
//    }
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

/**
 * Creates and remembers state objects for all the supported element types that are part of the
 * provided FeatureForm. These state objects are returned as part of a [FormStateCollection].
 *
 * @param form the [FeatureForm] to create the states for.
 * @param scope a [CoroutineScope] to run collectors and calculations on.
 *
 * @return returns the [FormStateCollection] created.
 */
@Composable
internal fun rememberStates(
    form: FeatureForm,
    elements: List<FormElement>,
    scope: CoroutineScope
): FormStateCollection {
    val states = MutableFormStateCollection()
    elements.forEach { element ->
        when (element) {
            is FieldFormElement -> {
                val state = rememberFieldState(element, form, scope)
                if (state != null) {
                    states.add(element, state)
                }
            }

            is GroupFormElement -> {
                val stateCollection = rememberStates(
                    form = form,
                    elements = element.elements,
                    scope = scope
                )
                val groupState = rememberBaseGroupState(
                    form = form,
                    groupElement = element,
                    fieldStates = stateCollection
                )
                states.add(element, groupState)
            }

            is TextFormElement -> {
                val state = rememberTextFormElementState(element, form)
                states.add(element, state)
            }

            else -> {}
        }
    }
    // The Toolkit currently only supports AttachmentsFormElements via the
    // default attachments element. Once AttachmentsFormElements can be authored
    // the switch case above should have a case added for AttachmentsFormElement.
    if (form.defaultAttachmentsElement != null) {
        val state = rememberAttachmentElementState(form, form.defaultAttachmentsElement!!)
        states.add(form.defaultAttachmentsElement!!, state)
    }
    return states
}

/**
 * Creates and remembers a [BaseFieldState] for the provided [element].
 *
 * @param element the [FieldFormElement] to create the state for.
 * @param form the [FeatureForm] the [element] is part of.
 * @param scope a [CoroutineScope] to run collectors and calculations on.
 *
 * @return returns the [BaseFieldState] created.
 */
@Composable
internal fun rememberFieldState(
    element: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): BaseFieldState<out Any?>? {
    return when (element.input) {
        is TextBoxFormInput, is TextAreaFormInput -> {
            val minLength = if (element.input is TextBoxFormInput) {
                (element.input as TextBoxFormInput).minLength.toInt()
            } else {
                (element.input as TextAreaFormInput).minLength.toInt()
            }
            val maxLength = if (element.input is TextBoxFormInput) {
                (element.input as TextBoxFormInput).maxLength.toInt()
            } else {
                (element.input as TextAreaFormInput).maxLength.toInt()
            }
            rememberFormTextFieldState(
                field = element,
                minLength = minLength,
                maxLength = maxLength,
                form = form,
                scope = scope
            )
        }

        is BarcodeScannerFormInput -> {
            rememberBarcodeTextFieldState(field = element, form = form, scope = scope)
        }

        is DateTimePickerFormInput -> {
            val input = element.input as DateTimePickerFormInput
            rememberDateTimeFieldState(
                field = element,
                minEpochMillis = input.min,
                maxEpochMillis = input.max,
                shouldShowTime = input.includeTime,
                form = form,
                scope = scope
            )
        }

        is ComboBoxFormInput -> {
            rememberComboBoxFieldState(
                field = element,
                form = form,
                scope = scope
            )
        }

        is SwitchFormInput -> {
            rememberSwitchFieldState(
                field = element,
                form = form,
                scope = scope,
                noValueString = stringResource(R.string.no_value)
            )
        }

        is RadioButtonsFormInput -> {
            rememberRadioButtonFieldState(
                field = element,
                form = form,
                scope = scope
            )
        }

        else -> {
            null
        }
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

private fun createNavController(context: Context): NavHostController {
    return NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }
}

private fun createSaveEditsDialog(
    featureForm: FeatureForm,
    navigationAction: () -> Unit,
    onDismiss: () -> Unit,
    dialogRequester: DialogRequester,
): DialogType.SaveFeatureDialog {
    return DialogType.SaveFeatureDialog(
        onSave = {
            if (featureForm.validationErrors.value.isNotEmpty()) {
                val count = featureForm.validationErrors.value.size
                val errorDialog = DialogType.ValidationErrorsDialog(
                    onDismiss = onDismiss,
                    title = "The form has errors",
                    body = "$count errors were found in the form. Please correct them before saving."
                )
                dialogRequester.requestDialog(errorDialog)
            } else {
                featureForm.finishEditing().onFailure {
                    val errorDialog = DialogType.ValidationErrorsDialog(
                        onDismiss = onDismiss,
                        title = "Error saving Form",
                        body = it.localizedMessage ?: "An error occurred while saving the form."
                    )
                    dialogRequester.requestDialog(errorDialog)
                }.onSuccess {
                    navigationAction()
                }
            }
        },
        onDiscard = {
            featureForm.discardEdits()
            navigationAction()
        }
    )
}
