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

package com.arcgismaps.toolkit.featureforms.internal.components.barcode

import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.base.handleCharConstraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class BarcodeFieldProperties(
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

internal class BarcodeTextFieldState(
    id: Int,
    properties: BarcodeFieldProperties,
    initialValue: String = properties.value.value,
    hasValueExpression: Boolean,
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
    /**
     * The minimum length of the field.
     */
    val minLength = properties.minLength

    /**
     * The maximum length of the field.
     */
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
