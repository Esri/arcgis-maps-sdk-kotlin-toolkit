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
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityNetworkAssociationLayers
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityNetworkAssociationsElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityNetworkAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The "property" determines the behavior of when the validation errors are visible.
 */
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

private val store = mutableMapOf<String, FeatureForm>()

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
 * Note : Even though the [FeatureForm] class is not stable, there exists an internal mechanism to
 * enable smart recompositions.
 *
 * @param featureForm The [FeatureForm] configuration.
 * @param modifier The [Modifier] to be applied to layout corresponding to the content of this
 * FeatureForm.
 * @param validationErrorVisibility The [ValidationErrorVisibility] that determines the behavior of
 * when the validation errors are visible. Default is [ValidationErrorVisibility.Automatic] which
 * indicates errors are only visible once the respective field gains focus.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param colorScheme The [FeatureFormColorScheme] to use for the FeatureForm.
 * @param typography The [FeatureFormTypography] to use for the FeatureForm.
 *
 * @since 200.4.0
 */
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    utilityNetwork: UtilityNetwork? = null,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    onUtilityFeatureEdit: ((FeatureForm) -> Boolean)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography()
) {
    var isGraphReady by rememberSaveable(featureForm) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val homeRoute = NavigationRoute.Form(featureForm.id)
    if (isGraphReady) {
        NavHost(
            navController,
            startDestination = homeRoute,
            enterTransition = { slideInHorizontally { h -> h } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { h -> h } }
        ) {
            composable<NavigationRoute.Form> {
                val routeData = it.toRoute<NavigationRoute.Form>()
                val form = store[routeData.formId] ?: return@composable
                val stateData = StateData(form)
                FeatureFormTheme(colorScheme, typography) {
                    FeatureForm(
                        stateData = stateData,
                        modifier = modifier,
                        validationErrorVisibility = validationErrorVisibility,
                        onBarcodeButtonClick = onBarcodeButtonClick,
                        onUtilityElementClicked = { element ->
                            stateData.featureForm.clearSelection()
                            scope.launch {
                                if (utilityNetwork != null) {
                                    utilityNetwork.getFeaturesForElements(listOf(element))
                                        .onSuccess { results ->
                                            val feature =
                                                results.firstOrNull() ?: return@onSuccess
                                            val newForm = FeatureForm(feature)
                                            // invoke the callback if provided, else navigate to the element
                                            val res = onUtilityFeatureEdit?.invoke(newForm) ?: true
                                            // if the callback returns false, do not navigate
                                            if (res) {
                                                store[newForm.id] = newForm
                                                val newRoute = NavigationRoute.Form(newForm.id)
                                                navController.navigate(newRoute) {
                                                    // to prevent a circular navigation
                                                    popUpTo(newRoute) {
                                                        inclusive = true
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        },
                        utilityNetwork = utilityNetwork
                    )
                }
                // only enable back navigation if there is a previous route
                BackHandler(navController.previousBackStackEntry != null) {
                    if (navController.popBackStack()) {
                        form.clearSelection()
                    }
                }
            }
        }
    }
    LaunchedEffect(featureForm) {
        if (!isGraphReady) {
            store.clear()
            store[featureForm.id] = featureForm
            isGraphReady = true
        }
    }
}

/**
 * A wrapper to hold state data. This provides a [Stable] class to enable smart recompositions,
 * since [FeatureForm] is not stable.
 */
@Immutable
internal data class StateData(@Stable val featureForm: FeatureForm)

/**
 * This composable uses the [StateData] class to display a [FeatureForm].
 */
@Composable
private fun FeatureForm(
    stateData: StateData,
    modifier: Modifier = Modifier,
    utilityNetwork: UtilityNetwork?,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityElementClicked: (UtilityElement) -> Unit,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
) {
    val featureForm = stateData.featureForm
    featureForm.selectFeature()
    // hold the list of form elements in a mutable state to make them observable
    val formElements = remember(featureForm) {
        mutableStateOf(featureForm.elements)
    }
    val scope = rememberCoroutineScope()
    val states = rememberStates(
        form = featureForm,
        elements = formElements.value,
        scope = scope
    )
    FeatureFormBody(
        form = featureForm,
        states = states,
        modifier = modifier,
        onExpressionsEvaluated = {
            // expressions evaluated, load attachments
            formElements.value = featureForm.elements
        },
        onBarcodeButtonClick = onBarcodeButtonClick,
        onUtilityElementClick = onUtilityElementClicked,
        utilityNetwork = utilityNetwork
    )
    FeatureFormDialog(states)
    // launch a new side effect in a launched effect when validationErrorVisibility changes
    LaunchedEffect(validationErrorVisibility) {
        // if it set to always show errors force each field to validate itself and show any errors
        if (validationErrorVisibility == ValidationErrorVisibility.Visible) {
            states.forEach { entry ->
                // validate all fields
                if (entry.formElement is FieldFormElement) {
                    entry.getState<BaseFieldState<*>>().forceValidation()
                }
                // validate any fields that are within a group
                if (entry.formElement is GroupFormElement) {
                    entry.getState<BaseGroupState>().fieldStates.forEach { childEntry ->
                        childEntry.getState<BaseFieldState<*>>().forceValidation()
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureFormTitle(featureForm: FeatureForm, modifier: Modifier = Modifier) {
    val title by featureForm.title.collectAsState()
    Text(
        text = title + " ${featureForm.id}",
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}

@Composable
private fun FeatureFormBody(
    form: FeatureForm,
    states: FormStateCollection,
    modifier: Modifier = Modifier,
    utilityNetwork: UtilityNetwork?,
    onExpressionsEvaluated: () -> Unit,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityElementClick: (UtilityElement) -> Unit
) {
    var initialEvaluation by rememberSaveable(form) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberSaveable(inputs = arrayOf(form), saver = LazyListState.Saver) {
        LazyListState()
    }
    val navController = rememberNavController(
        // set the navigator to the nav controller, this is required so that every time the
        // navigator (and form) changes, the nav controller is reset
        form
    )
    var currentRoute = rememberSaveable(form, saver = NavRouteSaver) {
        NavRoute.Form
    }
    // create a state for the utility network associations element if the utility network is
    // provided
    val unState = utilityNetwork?.let {
        val utilityElement = utilityNetwork.createElementOrNull(form.feature)
        UtilityNetworkAssociationsElementState(
            id = form.hashCode(),
            label = "Associations",
            description = "This is a description",
            isVisible = MutableStateFlow(true),
            utilityNetwork = it,
            utilityElement = utilityElement,
            scope = scope
        )
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavHost(
            navController,
            startDestination = currentRoute,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable<NavRoute.Form> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // title
                    FeatureFormTitle(
                        featureForm = form,
                        modifier = Modifier.padding(horizontal = 15.dp)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                    )
                    InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
                        initialEvaluation
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

                                    else -> {
                                        // other form elements are not created
                                    }
                                }
                            }
                        }
                        item {
                            if (unState != null) {
                                UtilityNetworkAssociationsElement(
                                    state = unState,
                                    onAssociationTypeClick = {
                                        // navigate to the associations screen
                                        navController.navigate(
                                            NavRoute.UNAssociations(
                                                unState.id,
                                                it
                                            )
                                        )
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
                        }
                    }
                }
            }

            composable<NavRoute.UNAssociations>(
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) { backStackEntry ->
                val data = backStackEntry.toRoute<NavRoute.UNAssociations>()
                currentRoute = data
                val group = unState!!.groups.value.getOrNull(data.selectedGroupIndex) ?: run {
                    // guard against out of bounds index
                    navController.navigate(NavRoute.Form) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    return@composable
                }
                UtilityNetworkAssociationLayers(
                    group = group,
                    source = unState.utilityElement!!,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onUtilityElementClick = onUtilityElementClick
                )
            }
        }
    }

    LaunchedEffect(form) {
        // ensure expressions are evaluated
        form.evaluateExpressions()
        initialEvaluation = true
        onExpressionsEvaluated()
    }
}

@Serializable
private sealed class NavRoute {
    @Serializable
    data object Form : NavRoute()

    @Serializable
    data class UNAssociations(
        val stateId: Int,
        val selectedGroupIndex: Int
    ) : NavRoute()
}

@Serializable
private sealed class NavigationRoute {
    @Serializable
    data class Form(val formId: String) : NavigationRoute()
}

private val NavRouteSaver: Saver<NavRoute, Bundle> = object : Saver<NavRoute, Bundle> {
    override fun SaverScope.save(value: NavRoute): Bundle {
        return Bundle().apply {
            putString("navRoute", Json.encodeToString(NavRoute.serializer(), value))
        }
    }

    override fun restore(value: Bundle): NavRoute? {
        return value.getString("navRoute")?.let { Json.decodeFromString(it) }
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

@Composable
internal fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    val alpha by animateFloatAsState(
        if (evaluationProvider()) 0f else 1f,
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

private val FeatureForm.id: String
    get() = feature.attributes["objectid"].toString()

private fun FeatureForm.selectFeature() {
    val table = feature.featureTable as? ArcGISFeatureTable ?: return
    val layer = when(table.layer) {
        is SubtypeFeatureLayer -> table.layer as SubtypeFeatureLayer
        is FeatureLayer -> table.layer as FeatureLayer
        else -> return
    }
    layer.selectFeature(feature)
}

private fun FeatureForm.clearSelection() {
    val table = feature.featureTable as? ArcGISFeatureTable ?: return
    val layer = when(table.layer) {
        is SubtypeFeatureLayer -> table.layer as SubtypeFeatureLayer
        is FeatureLayer -> table.layer as FeatureLayer
        else -> return
    }
    layer.unselectFeature(feature)
}
