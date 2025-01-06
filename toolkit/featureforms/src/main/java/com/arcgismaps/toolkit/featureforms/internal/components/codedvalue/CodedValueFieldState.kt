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

package com.arcgismaps.toolkit.featureforms.internal.components.codedvalue

import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.utils.isNullOrEmptyString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal open class CodedValueFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<Any?>,
    validationErrors: StateFlow<List<ValidationErrorState>>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    fieldType: FieldType,
    val codedValues: Map<Any?, String>,
    val showNoValueOption: FormInputNoValueOption,
    val noValueLabel: String
) : FieldProperties<Any?>(
    label,
    placeholder,
    description,
    value,
    validationErrors,
    required,
    editable,
    visible,
    fieldType,
    null
)

/**
 * A class to handle the state of any coded value type. Essential properties are inherited
 * from the [BaseFieldState].
 *
 * When calling [CodedValueFieldState.onValueChanged], to indicate a no value option or clearing the
 * value, pass in null.
 *
 * @param id Unique identifier for the field.
 * @param properties the [CodedValueFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param hasValueExpression a flag to indicate if the field has a value expression.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param updateValue a function that is invoked when the user edits result in a change of value. This
 * is called in [BaseFieldState.onValueChanged].
 * @param evaluateExpressions a function that is invoked to evaluate all form expressions. This is
 * called after a successful [updateValue].
 */
internal abstract class CodedValueFieldState(
    id : Int,
    properties: CodedValueFieldProperties,
    initialValue: Any? = properties.value.value,
    hasValueExpression : Boolean,
    scope: CoroutineScope,
    updateValue: (Any?) -> Unit,
    evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>
) : BaseFieldState<Any?>(
    id = id,
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    hasValueExpression = hasValueExpression,
    updateValue = updateValue,
    evaluateExpressions = evaluateExpressions
) {
    /**
     * The map of coded values associated with this field.
     */
    val codedValues: Map<Any?, String> = properties.codedValues

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
     * Returns the name of the [code] if it is present in [codedValues]. If not, it returns the
     * [code] as a string. If [code] is null, it returns an empty string.
     */
    fun getNameForCodedValue(code: Any?): String {
        return codedValues[code] ?: code?.toString().orEmpty()
    }

    override fun typeConverter(input: Any?): Any? {
        return if (input.isNullOrEmptyString()) {
            null
        } else {
            input
        }
    }
}
