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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import kotlinx.coroutines.CoroutineScope

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
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val stateData = remember(featureForm) {
        StateData(featureForm)
    }
    FeatureFormTheme(colorScheme, typography) {
        FeatureForm(
            stateData = stateData,
            modifier = modifier,
            validationErrorVisibility = validationErrorVisibility,
            onBarcodeButtonClick = onBarcodeButtonClick
        )
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
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?
) {
    val featureForm = stateData.featureForm
    // Hold the list of form elements.
    val formElements: List<FormElement> = remember(featureForm) {
        // Add the default attachments element, if present.
        featureForm.elements + listOfNotNull(featureForm.defaultAttachmentsElement)
    }
    val scope = rememberCoroutineScope()
    val states = rememberStates(
        form = featureForm,
        elements = formElements,
        scope = scope
    )
    FeatureFormBody(
        form = featureForm,
        states = states,
        modifier = modifier,
        onBarcodeButtonClick = onBarcodeButtonClick
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
                        (childEntry.state as? BaseFieldState<*>)?.forceValidation()
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
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}

@Composable
private fun FeatureFormBody(
    form: FeatureForm,
    states: FormStateCollection,
    modifier: Modifier = Modifier,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?
) {
    var initialEvaluation by rememberSaveable(form) { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    Column(
        modifier = modifier.fillMaxSize(),
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

                        is AttachmentsFormElement -> {
                            AttachmentFormElement(
                                state = entry.getState(),
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 15.dp, vertical = 10.dp)
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

                        else -> {
                            // other form elements are not created
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(form) {
        // ensure expressions are evaluated
        form.evaluateExpressions()
        initialEvaluation = true
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

            is AttachmentsFormElement -> {
                val state = rememberAttachmentElementState(form, element)
                states.add(element, state)
            }

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
