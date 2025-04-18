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

import androidx.compose.runtime.Stable
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class SwitchFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<Any?>,
    validationErrors : StateFlow<List<ValidationErrorState>>,
    editable: StateFlow<Boolean>,
    required: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    fieldType: FieldType,
    val onValue: CodedValue,
    val offValue: CodedValue,
    val fallback: Boolean,
    showNoValueOption: FormInputNoValueOption,
    noValueLabel: String
) : CodedValueFieldProperties(
    label,
    placeholder,
    description,
    value,
    validationErrors,
    required,
    editable,
    visible,
    fieldType,
    mapOf(
        onValue.code to onValue.name,
        offValue.code to offValue.name
    ),
    showNoValueOption,
    noValueLabel
)

/**
 * A class to handle the state of a [SwitchField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [SwitchFieldProperties] associated with this state.
 * @property initialValue the initial value to set for this field. This value should be a CodedValue code or subtype.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param updateValue a function that is invoked when the user edits result in a change of value. This
 * is called in [BaseFieldState.onValueChanged].
 * @param evaluateExpressions a function that is invoked to evaluate all form expressions. This is
 * called after a successful [updateValue].
 */
@Stable
internal class SwitchFieldState(
    id : Int,
    properties: SwitchFieldProperties,
    val initialValue: Any? = properties.value.value,
    hasValueExpression : Boolean,
    scope: CoroutineScope,
    updateValue: (Any?) -> Unit,
    evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>
) : CodedValueFieldState(
    id = id,
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    hasValueExpression = hasValueExpression,
    updateValue = updateValue,
    evaluateExpressions = evaluateExpressions
) {
    /**
     * The CodedValue that represents the "on" state of the Switch.
     */
    val onValue: CodedValue = properties.onValue

    /**
     * The CodedValue that represents the "off" state of the Switch.
     */
    val offValue: CodedValue = properties.offValue

    /**
     * Whether this Switch should fall back to being displayed as a ComboBox.
     */
    val fallback: Boolean = properties.fallback

}
