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

package com.arcgismaps.toolkit.featureforms.internal.components.text

import androidx.compose.runtime.Stable
import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.base.handleCharConstraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class TextFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    validationErrors: StateFlow<List<ValidationErrorState>>,
    fieldType: FieldType,
    domain: Domain?,
    val singleLine: Boolean,
    val minLength: Int,
    val maxLength: Int,
) : FieldProperties<String>(
    label,
    placeholder,
    description,
    value,
    validationErrors,
    required,
    editable,
    visible,
    fieldType,
    domain
)

/**
 * A class to handle the state of a [FormTextField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param id Unique identifier for the field.
 * @param properties the [TextFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param updateValue a function that is invoked when the user edits result in a change of value. This
 * is called in [BaseFieldState.onValueChanged].
 * @param evaluateExpressions a function that is invoked to evaluate all form expressions. This is
 * called after a successful [updateValue].
 */
@Stable
internal class FormTextFieldState(
    id : Int,
    properties: TextFieldProperties,
    initialValue: String = properties.value.value,
    hasValueExpression : Boolean,
    scope: CoroutineScope,
    updateValue: (Any?) -> Unit,
    evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>
) : BaseFieldState<String>(
    id = id,
    properties = properties,
    initialValue = initialValue,
    hasValueExpression = hasValueExpression,
    scope = scope,
    updateValue = updateValue,
    evaluateExpressions = evaluateExpressions
) {
    // indicates singleLine only if TextBoxFeatureFormInput
    val singleLine = properties.singleLine

    // fetch the minLength based on the featureFormElement.inputType
    val minLength = properties.minLength

    // fetch the maxLength based on the featureFormElement.inputType
    val maxLength = properties.maxLength

    override fun typeConverter(input: String): Any? {
        if (input.isEmpty()) {
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

    override fun calculateHelperText(): ValidationErrorState {
        // If field type is text, handle character constraints
        return if (fieldType == FieldType.Text) {
            handleCharConstraints(minLength, maxLength, hasValueExpression)
        } else {
            // Otherwise, call the super class method which handles numeric constraints
            super.calculateHelperText()
        }
    }
}
