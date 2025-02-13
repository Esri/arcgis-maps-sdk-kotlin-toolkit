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

package com.arcgismaps.toolkit.featureforms

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavController
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormGroupState
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.BarcodeFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.BarcodeTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseGroupState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.MutableFormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.formattedValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValidationErrors
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.CodedValueFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.RadioButtonFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.RadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.SwitchFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.SwitchFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityNetworkAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.fieldIsNullable
import com.arcgismaps.toolkit.featureforms.internal.utils.toMap
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Stable
public class FeatureFormState(
    private val featureForm: FeatureForm,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    private val coroutineScope: CoroutineScope,
    private val utilityNetwork: UtilityNetwork? = null
) {
    private val store: ArrayDeque<StateData> = ArrayDeque()

    private var navController: NavController? = null

    private var _validationErrorVisibility: MutableState<ValidationErrorVisibility> =
        mutableStateOf(validationErrorVisibility)

    private val _activeFeatureForm: MutableState<FeatureForm> = mutableStateOf(featureForm)

    public val validationErrorVisibility: ValidationErrorVisibility by _validationErrorVisibility

    public val activeFeatureForm : FeatureForm by _activeFeatureForm

    init {
        val states = createStates(featureForm, featureForm.elements, coroutineScope)
        val unState = utilityNetwork?.let {
            val element = utilityNetwork.createElementOrNull(featureForm.feature)
            UtilityNetworkAssociationsElementState(
                id = featureForm.hashCode(),
                label = "Associations",
                description = "This is a description",
                isVisible = MutableStateFlow(true),
                utilityNetwork = utilityNetwork,
                utilityElement = element,
                scope = coroutineScope
            )
        }
        store.addLast(StateData(featureForm, states, unState))
    }

    internal constructor(
        featureForm: FeatureForm,
        stateCollection: FormStateCollection,
        validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
        coroutineScope: CoroutineScope
    ) : this(
        featureForm,
        validationErrorVisibility,
        coroutineScope
    ) {
        // Since the state collection is provided, clear the store and add the provided state collection
        store.clear()
        store.addLast(StateData(featureForm, stateCollection, null))
    }

    internal fun setNavController(navController: NavController?) {
        this.navController = navController
    }

    internal fun navigateTo(form: FeatureForm) {
        val states = createStates(form, form.elements, coroutineScope)
        val unState = utilityNetwork?.let {
            val element = utilityNetwork.createElementOrNull(form.feature)
            UtilityNetworkAssociationsElementState(
                id = form.hashCode(),
                label = "Associations",
                description = "This is a description",
                isVisible = MutableStateFlow(true),
                utilityNetwork = utilityNetwork,
                utilityElement = element,
                scope = coroutineScope
            )
        }
        if (navController != null) {
            store.addLast(StateData(form, states, unState))
            navController!!.navigate(NavigationRoute.FormView)
            _activeFeatureForm.value = form
        }
    }

    internal fun popBackStack(): Boolean {
        return if (navController != null) {
            if (navController!!.currentBackStackEntry == null || store.size <= 1) {
                return false
            }
            store.removeLast()
            navController!!.popBackStack()
            _activeFeatureForm.value = getActiveStateData().featureForm
            true
        } else false
    }

    internal fun hasBackStack(): Boolean {
        return navController?.previousBackStackEntry != null
    }

    internal fun getActiveStateData(): StateData {
        return store.last()
    }

    public fun setValidationErrorVisibility(visibility: ValidationErrorVisibility) {
        _validationErrorVisibility.value = visibility
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
    private fun createStates(
        form: FeatureForm,
        elements: List<FormElement>,
        scope: CoroutineScope
    ): FormStateCollection {
        val states = MutableFormStateCollection()
        elements.forEach { element ->
            when (element) {
                is FieldFormElement -> {
                    val state = createFieldState(element, form, scope)
                    if (state != null) {
                        states.add(element, state)
                    }
                }

                is GroupFormElement -> {
                    val fieldStates = createStates(
                        form = form,
                        elements = element.elements,
                        scope = scope
                    )
                    val groupState = BaseGroupState(
                        id = element.hashCode(),
                        label = element.label,
                        description = element.description,
                        isVisible = element.isVisible,
                        expanded = element.initialState == FormGroupState.Expanded,
                        fieldStates = fieldStates
                    )
                    states.add(element, groupState)
                }

                is TextFormElement -> {
                    val state = TextFormElementState(
                        id = element.hashCode(),
                        label = element.label,
                        description = element.description,
                        isVisible = element.isVisible,
                        text = element.text,
                        format = element.format
                    )
                    states.add(element, state)
                }

                else -> {}
            }
        }
        // The Toolkit currently only supports AttachmentsFormElements via the
        // default attachments element. Once AttachmentsFormElements can be authored
        // the switch case above should have a case added for AttachmentsFormElement.
        if (form.defaultAttachmentsElement != null) {
            val state = AttachmentElementState(
                formElement = form.defaultAttachmentsElement!!,
                scope = scope,
                id = form.defaultAttachmentsElement!!.hashCode(),
                evaluateExpressions = form::evaluateExpressions
            )
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
    private fun createFieldState(
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
                FormTextFieldState(
                    id = element.hashCode(),
                    properties = TextFieldProperties(
                        label = element.label,
                        placeholder = element.hint,
                        description = element.description,
                        value = element.formattedValueAsStateFlow(scope),
                        validationErrors = element.mapValidationErrors(scope),
                        required = element.isRequired,
                        editable = element.isEditable,
                        visible = element.isVisible,
                        domain = element.domain as? RangeDomain,
                        fieldType = element.fieldType,
                        singleLine = element.input is TextBoxFormInput,
                        minLength = minLength,
                        maxLength = maxLength
                    ),
                    hasValueExpression = element.hasValueExpression,
                    scope = scope,
                    updateValue = element::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                )
            }

            is BarcodeScannerFormInput -> {
                BarcodeTextFieldState(
                    id = element.hashCode(),
                    properties = BarcodeFieldProperties(
                        label = element.label,
                        placeholder = element.hint,
                        description = element.description,
                        value = element.formattedValueAsStateFlow(scope),
                        required = element.isRequired,
                        editable = element.isEditable,
                        visible = element.isVisible,
                        validationErrors = element.mapValidationErrors(scope),
                        fieldType = element.fieldType,
                        domain = element.domain,
                        minLength = (element.input as BarcodeScannerFormInput).minLength.toInt(),
                        maxLength = (element.input as BarcodeScannerFormInput).maxLength.toInt()
                    ),
                    hasValueExpression = element.hasValueExpression,
                    scope = scope,
                    updateValue = element::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                )
            }

            is DateTimePickerFormInput -> {
                val input = element.input as DateTimePickerFormInput
                DateTimeFieldState(
                    id = element.hashCode(),
                    properties = DateTimeFieldProperties(
                        label = element.label,
                        placeholder = element.hint,
                        description = element.description,
                        value = element.mapValueAsStateFlow(scope),
                        validationErrors = element.mapValidationErrors(scope),
                        editable = element.isEditable,
                        required = element.isRequired,
                        visible = element.isVisible,
                        minEpochMillis = input.min,
                        maxEpochMillis = input.min,
                        shouldShowTime = input.includeTime,
                        fieldType = element.fieldType
                    ),
                    hasValueExpression = element.hasValueExpression,
                    scope = scope,
                    updateValue = element::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                )
            }

            is ComboBoxFormInput -> {
                val input = element.input as ComboBoxFormInput
                ComboBoxFieldState(
                    id = element.hashCode(),
                    properties = CodedValueFieldProperties(
                        label = element.label,
                        placeholder = element.hint,
                        description = element.description,
                        value = element.value,
                        validationErrors = element.mapValidationErrors(scope),
                        editable = element.isEditable,
                        required = element.isRequired,
                        visible = element.isVisible,
                        codedValues = input.codedValues.toMap(),
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel,
                        fieldType = element.fieldType
                    ),
                    hasValueExpression = element.hasValueExpression,
                    scope = scope,
                    updateValue = element::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                )
            }

            is SwitchFormInput -> {
                val input = element.input as SwitchFormInput
                val initialValue = element.formattedValue
                val fallback = initialValue.isEmpty()
                    || (element.value.value != input.onValue.code && element.value.value != input.offValue.code)
                SwitchFieldState(
                    id = element.hashCode(),
                    properties = SwitchFieldProperties(
                        label = element.label,
                        placeholder = element.hint,
                        description = element.description,
                        value = element.value,
                        validationErrors = element.mapValidationErrors(scope),
                        editable = element.isEditable,
                        required = element.isRequired,
                        visible = element.isVisible,
                        fieldType = element.fieldType,
                        onValue = input.onValue,
                        offValue = input.offValue,
                        fallback = fallback,
                        showNoValueOption = if (form.fieldIsNullable(element))
                            FormInputNoValueOption.Show
                        else
                            FormInputNoValueOption.Hide,
                        noValueLabel = "No Value"
                    ),
                    hasValueExpression = element.hasValueExpression,
                    scope = scope,
                    updateValue = element::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                )
            }

            is RadioButtonsFormInput -> {
                val input = element.input as RadioButtonsFormInput
                RadioButtonFieldState(
                    id = element.hashCode(),
                    properties = RadioButtonFieldProperties(
                        label = element.label,
                        placeholder = element.hint,
                        description = element.description,
                        value = element.value,
                        validationErrors = element.mapValidationErrors(scope),
                        editable = element.isEditable,
                        required = element.isRequired,
                        visible = element.isVisible,
                        fieldType = element.fieldType,
                        codedValues = input.codedValues.toMap(),
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel
                    ),
                    hasValueExpression = element.hasValueExpression,
                    scope = scope,
                    updateValue = element::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                )
            }

            else -> {
                null
            }
        }
    }
}

@Immutable
internal data class StateData(
    val featureForm: FeatureForm,
    val stateCollection: FormStateCollection,
    val unState: UtilityNetworkAssociationsElementState?
)
