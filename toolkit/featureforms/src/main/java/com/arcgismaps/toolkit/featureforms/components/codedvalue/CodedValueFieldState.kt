/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.components.codedvalue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import com.arcgismaps.toolkit.featureforms.utils.valueFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal open class CodedValueFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    val fieldType: FieldType,
    val codedValues: List<CodedValue>,
    val showNoValueOption: FormInputNoValueOption,
    val noValueLabel: String
) : FieldProperties<String>(label, placeholder, description, value, required, editable, visible)

/**
 * A class to handle the state of a [ComboBoxField]. Essential properties are inherited
 * from the [BaseFieldState].
 *
 * @param properties the [CodedValueFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [CodedValueFieldState.onValueChanged].
 */
@Stable
internal open class CodedValueFieldState(
    properties: CodedValueFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit),
    validate: () -> List<Throwable>
) : BaseFieldState<String>(
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    onEditValue = onEditValue,
    defaultValidator = validate
) {
    /**
     * The list of coded values associated with this field.
     */
    val codedValues: List<CodedValue> = properties.codedValues

    /**
     * This property defines whether to display a special "no value" option if this field is
     * optional.
     */
    val showNoValueOption: FormInputNoValueOption = properties.showNoValueOption

    /**
     * The custom label to use if [showNoValueOption] is enabled.
     */
    val noValueLabel: String = properties.noValueLabel
    
    /**
     * The FieldType of the element's Field.
     */
    val fieldType: FieldType = properties.fieldType

    /**
     * Returns the name of the [code] if it is present in [codedValues] else returns null.
     */
    fun getCodedValueNameOrNull(code: Any?): String? {
        return codedValues.find {
            it.code.toString() == code.toString()
        }?.name
    }
    
    override fun onValueChanged(input: String) {
        val code = codedValues.firstOrNull {
            it.name == input
        }?.code
        onEditValue(code)
        _value.value = input
    }

    companion object {
        /**
         * The default saver for a [CodedValueFieldState] implemented for a [ComboBoxFormInput] type.
         * Hence for [formElement] the [FieldFormElement.input] type must be a [ComboBoxFormInput].
         */
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<CodedValueFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value.data,
                    it.wasFocused
                )
            },
            restore = { list ->
                val input = formElement.input as ComboBoxFormInput
                CodedValueFieldState(
                    properties = CodedValueFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.valueFlow(scope),
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        visible = formElement.isVisible,
                        codedValues = input.codedValues,
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel,
                        fieldType = form.fieldType(formElement)
                    ),
                    initialValue = list[0] as String,
                    scope = scope,
                    onEditValue = { newValue ->
                        form.editValue(formElement, newValue)
                        scope.launch { form.evaluateExpressions() }
                    },
                    validate = formElement::getValidationErrors
                ).apply {
                    onFocusChanged(list[1] as Boolean)
                }
            }
        )
    }
}

@Composable
internal fun rememberCodedValueFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): CodedValueFieldState = rememberSaveable(
    saver = CodedValueFieldState.Saver(field, form, scope)
) {
    val input = field.input as ComboBoxFormInput
    CodedValueFieldState(
        properties = CodedValueFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.valueFlow(scope),
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            codedValues = input.codedValues,
            showNoValueOption = input.noValueOption,
            noValueLabel = input.noValueLabel,
            fieldType = form.fieldType(field)
        ),
        scope = scope,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        },
        validate = field::getValidationErrors
    )
}
