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

package com.arcgismaps.toolkit.featureforms.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.components.base.formattedValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.components.base.mapValidationErrors
import com.arcgismaps.toolkit.featureforms.utils.isNumeric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class TextFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    validationErrors : StateFlow<List<ValidationErrorState>>,
    val fieldType: FieldType,
    val domain: Domain?,
    val singleLine: Boolean,
    val minLength: Int,
    val maxLength: Int,
) : FieldProperties<String>(label, placeholder, description, value, validationErrors, required, editable, visible)

/**
 * A class to handle the state of a [FormTextField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [TextFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [FormTextFieldState.onValueChanged]
 */
@Stable
internal class FormTextFieldState(
    properties: TextFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    onEditValue: (Any?) -> Unit
) : BaseFieldState<String>(
    properties = properties,
    initialValue = initialValue,
    scope = scope,
    onEditValue = onEditValue
) {
    // indicates singleLine only if TextBoxFeatureFormInput
    val singleLine = properties.singleLine

    // fetch the minLength based on the featureFormElement.inputType
    val minLength = properties.minLength

    // fetch the maxLength based on the featureFormElement.inputType
    val maxLength = properties.maxLength

    /**
     * The domain of the element's field.
     */
    val domain: Domain? = properties.domain

    /**
     * The FieldType of the element's field.
     */
    val fieldType: FieldType = properties.fieldType

    override fun typeConverter(input: String): Any? {
        if (input.isEmpty() && fieldType.isNumeric) {
            return null
        }
        return when (fieldType) {
            FieldType.Int16 -> input.toIntOrNull()?.toShort()
            FieldType.Int32 -> input.toIntOrNull()
            FieldType.Int64 -> input.toLongOrNull()
            FieldType.Float32 -> input.toFloatOrNull()
            FieldType.Float64 -> input.toDoubleOrNull()
            FieldType.Text -> input
            else -> null
        } ?: input
    }

    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<FormTextFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value.data,
                    it.wasFocused
                )
            },
            restore = { list ->
                val minLength = (formElement.input as? TextBoxFormInput)?.minLength
                    ?: (formElement.input as TextAreaFormInput).minLength
                val maxLength = (formElement.input as? TextBoxFormInput)?.maxLength
                    ?: (formElement.input as TextAreaFormInput).maxLength
                FormTextFieldState(
                    properties = TextFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.formattedValueAsStateFlow(scope),
                        validationErrors = formElement.mapValidationErrors(scope),
                        required = formElement.isRequired,
                        editable = formElement.isEditable,
                        visible = formElement.isVisible,
                        domain = formElement.domain as? RangeDomain,
                        fieldType = formElement.fieldType,
                        singleLine = formElement.input is TextBoxFormInput,
                        minLength = minLength.toInt(),
                        maxLength = maxLength.toInt()
                    ),
                    initialValue = list[0] as String,
                    scope = scope,
                    onEditValue = { newValue ->
                        formElement.updateValue(newValue)
                        scope.launch { form.evaluateExpressions() }
                    }
                ).apply {
                    // focus is lost on rotation. https://devtopia.esri.com/runtime/apollo/issues/230
                    onFocusChanged(list[1] as Boolean)
                }
            }
        )
    }
}

@Composable
internal fun rememberFormTextFieldState(
    field: FieldFormElement,
    minLength: Int,
    maxLength: Int,
    form: FeatureForm,
    scope: CoroutineScope
): FormTextFieldState = rememberSaveable(
    inputs = arrayOf(form),
    saver = FormTextFieldState.Saver(field, form, scope)
) {
    FormTextFieldState(
        properties = TextFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.formattedValueAsStateFlow(scope),
            validationErrors = field.mapValidationErrors(scope),
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            singleLine = field.input is TextBoxFormInput,
            fieldType = field.fieldType,
            domain = field.domain as? RangeDomain,
            minLength = minLength,
            maxLength = maxLength
        ),
        scope = scope,
        onEditValue = { newValue ->
            field.updateValue(newValue)
            scope.launch { form.evaluateExpressions() }
        }
    )
}
