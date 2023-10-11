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

package com.arcgismaps.toolkit.featureforms.components.radio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.utils.editValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class RadioButtonFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    val codedValues: List<String>,
    val showNoValueOption: FormInputNoValueOption,
    val noValueLabel: String
) : FieldProperties(label, placeholder, description, value, required, editable)

internal class RadioButtonFieldState(
    properties: RadioButtonFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit)
) : BaseFieldState(properties, initialValue, scope, onEditValue) {

    /**
     * The list of coded values associated with this field.
     */
    val codedValues: List<String> = properties.codedValues

    /**
     * This property defines whether to display a special "no value" option if this field is
     * optional.
     */
    val showNoValueOption: FormInputNoValueOption = properties.showNoValueOption

    /**
     * The custom label to use if [showNoValueOption] is enabled.
     */
    val noValueLabel: String = properties.noValueLabel

    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<RadioButtonFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value
                )
            },
            restore = { list ->
                val input = formElement.input as RadioButtonsFormInput
                RadioButtonFieldState(
                    properties = RadioButtonFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.value,
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        codedValues = input.codedValues.map { it.code.toString() },
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel
                    ),
                    initialValue = list[0],
                    scope = scope,
                    onEditValue = { newValue ->
                        form.editValue(formElement, newValue)
                        scope.launch { form.evaluateExpressions() }
                    }
                )
            }
        )
    }
}

@Composable
internal fun rememberRadioButtonFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): RadioButtonFieldState = rememberSaveable(
    saver = RadioButtonFieldState.Saver(field, form, scope)
) {
    val input = field.input as RadioButtonsFormInput
    RadioButtonFieldState(
        properties = RadioButtonFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            editable = field.isEditable,
            required = field.isRequired,
            codedValues = input.codedValues.map { it.code.toString() },
            showNoValueOption = input.noValueOption,
            noValueLabel = input.noValueLabel
        ),
        scope = scope,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        }
    )
}
