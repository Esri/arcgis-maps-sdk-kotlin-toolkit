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

import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import kotlinx.coroutines.CoroutineScope

internal typealias RadioButtonFieldProperties = CodedValueFieldProperties

internal class RadioButtonFieldState(
    id : Int,
    properties: RadioButtonFieldProperties,
    initialValue: Any? = properties.value.value,
    hasValueExpression : Boolean,
    scope: CoroutineScope,
    updateValue: (Any?) -> Unit,
    evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>
) : CodedValueFieldState(
    id,
    properties = properties,
    initialValue = initialValue,
    hasValueExpression = hasValueExpression,
    scope = scope,
    updateValue = updateValue,
    evaluateExpressions = evaluateExpressions
) {

    /**
     * Returns true if the initial value is not in the [codedValues]. This should
     * trigger a fallback to a ComboBox. If the [value] is null then this returns false.
     */
    val shouldFallback = if (initialValue == null) {
        false
    } else {
        !codedValues.contains(initialValue)
    }
}
