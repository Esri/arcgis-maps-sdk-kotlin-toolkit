/*
 * Copyright 2024 Esri
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
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A concrete class for use with a [ComboBoxField].
 */
internal class ComboBoxFieldState(
    properties: CodedValueFieldProperties,
    initialValue: Any? = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit),
    defaultValidator: () -> List<Throwable>
) : CodedValueFieldState(properties, initialValue, scope, onEditValue, defaultValidator) {

    init {
        // Start observing the properties. Since this method cannot be invoked from any open base
        // class initializer blocks, it is safe to invoke it here.
        observeProperties()
    }

    companion object {
        /**
         * The default saver for a [ComboBoxFieldState] implemented for a [ComboBoxFormInput] type.
         * Hence for [formElement] the [FieldFormElement.input] type must be a [ComboBoxFormInput].
         */
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<ComboBoxFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value.data,
                    it.wasFocused
                )
            },
            restore = { list ->
                val input = formElement.input as ComboBoxFormInput
                ComboBoxFieldState(
                    properties = CodedValueFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.value,
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        visible = formElement.isVisible,
                        codedValues = input.codedValues,
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel,
                        fieldType = formElement.fieldType
                    ),
                    initialValue = list[0],
                    scope = scope,
                    onEditValue = { newValue ->
                        formElement.updateValue(newValue)
                        scope.launch { form.evaluateExpressions() }
                    },
                    defaultValidator = formElement::getValidationErrors
                ).apply {
                    onFocusChanged(list[1] as Boolean)
                }
            }
        )
    }
}

@Composable
internal fun rememberComboBoxFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): ComboBoxFieldState = rememberSaveable(
    saver = ComboBoxFieldState.Saver(field, form, scope)
) {
    val input = field.input as ComboBoxFormInput
    ComboBoxFieldState(
        properties = CodedValueFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            codedValues = input.codedValues,
            showNoValueOption = input.noValueOption,
            noValueLabel = input.noValueLabel,
            fieldType = field.fieldType
        ),
        scope = scope,
        onEditValue = {
            field.updateValue(it)
            scope.launch { form.evaluateExpressions() }
        },
        defaultValidator = field::getValidationErrors
    )
}
