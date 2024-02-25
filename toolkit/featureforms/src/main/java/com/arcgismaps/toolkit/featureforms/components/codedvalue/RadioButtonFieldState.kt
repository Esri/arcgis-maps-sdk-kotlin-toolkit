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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.toolkit.featureforms.components.base.mapValidationErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal typealias RadioButtonFieldProperties = CodedValueFieldProperties

internal class RadioButtonFieldState(
    properties: RadioButtonFieldProperties,
    initialValue: Any? = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit)
) : CodedValueFieldState(
    properties = properties,
    initialValue = initialValue,
    scope = scope,
    onEditValue = onEditValue
) {

    /**
     * Returns true if the initial value is not in the [codedValues]. This should
     * trigger a fallback to a ComboBox. If the [value] is false then this returns false.
     */
    val shouldFallback = if (initialValue == null) {
        false
    } else {
        !codedValues.any {
            it.name == value.value.data
        }
    }

    companion object {

        /**
         * Default saver for the [RadioButtonFieldState].
         */
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<RadioButtonFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value.data
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
                        validationErrors = formElement.mapValidationErrors(scope),
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        visible = formElement.isVisible,
                        fieldType = formElement.fieldType,
                        codedValues = input.codedValues,
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel
                    ),
                    initialValue = list[0],
                    scope = scope,
                    onEditValue = { newValue ->
                        formElement.updateValue(newValue)
                        scope.launch { form.evaluateExpressions() }
                    },
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
    inputs = arrayOf(form),
    saver = RadioButtonFieldState.Saver(field, form, scope)
) {
    val input = field.input as RadioButtonsFormInput
    RadioButtonFieldState(
        properties = RadioButtonFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            validationErrors = field.mapValidationErrors(scope),
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            fieldType = field.fieldType,
            codedValues = input.codedValues,
            showNoValueOption = input.noValueOption,
            noValueLabel = input.noValueLabel
        ),
        scope = scope,
        onEditValue = {
            field.updateValue(it)
            scope.launch { form.evaluateExpressions() }
        },
    )
}
