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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.base.formattedValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValidationErrors
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
    val fieldType: FieldType,
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
    visible
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
     * The [FieldType] of the field.
     */
    val fieldType = properties.fieldType

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

    companion object {
        fun Saver(
            field: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<BarcodeTextFieldState, *> = listSaver(
            save = {
                listOf(
                    it.wasFocused
                )
            },
            restore = {
                val focused = it[0]
                val input = field.input as BarcodeScannerFormInput
                BarcodeTextFieldState(
                    id = field.hashCode(),
                    properties = BarcodeFieldProperties(
                        label = field.label,
                        placeholder = field.hint,
                        description = field.description,
                        value = field.formattedValueAsStateFlow(scope),
                        required = field.isRequired,
                        editable = field.isEditable,
                        visible = field.isVisible,
                        validationErrors = field.mapValidationErrors(scope),
                        fieldType = field.fieldType,
                        minLength = input.minLength.toInt(),
                        maxLength = input.maxLength.toInt()
                    ),
                    hasValueExpression = field.hasValueExpression,
                    scope = scope,
                    updateValue = field::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                ).apply {
                    // Restore the focus state
                    onFocusChanged(focused)
                }
            }
        )
    }
}

@Composable
internal fun rememberBarcodeTextFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope,
): BarcodeTextFieldState = rememberSaveable(
    inputs = arrayOf(form),
    saver = BarcodeTextFieldState.Saver(field, form, scope)
) {
    BarcodeTextFieldState(
        id = field.hashCode(),
        properties = BarcodeFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.formattedValueAsStateFlow(scope),
            required = field.isRequired,
            editable = field.isEditable,
            visible = field.isVisible,
            validationErrors = field.mapValidationErrors(scope),
            fieldType = field.fieldType,
            minLength = (field.input as BarcodeScannerFormInput).minLength.toInt(),
            maxLength = (field.input as BarcodeScannerFormInput).maxLength.toInt()
        ),
        hasValueExpression = field.hasValueExpression,
        scope = scope,
        updateValue = field::updateValue,
        evaluateExpressions = form::evaluateExpressions
    )
}
