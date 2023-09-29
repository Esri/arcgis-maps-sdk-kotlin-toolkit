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

package com.arcgismaps.toolkit.featureforms.components.base

import com.arcgismaps.mapping.featureforms.FieldFormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal open class FieldProperties(
    val label: String,
    val placeholder: String,
    val description: String,
    val value: StateFlow<String>,
    val required: StateFlow<Boolean>,
    val editable: StateFlow<Boolean>
)

/**
 * Base state class for any Field within a feature form. It provides the default set of properties
 * that are common to all [FieldFormElement]'s.
 */
internal open class BaseFieldState(
    properties: FieldProperties,
    initialValue: String = properties.value.value,
    private val scope: CoroutineScope,
    private val onEditValue: (Any?) -> Unit,
    private val onEvaluateExpression: suspend () -> Unit,
) {
    /**
     * Title for the field.
     */
    open val label: String = properties.label

    /**
     * Placeholder hint for the field.
     */
    open val placeholder: String = properties.placeholder

    /**
     * Description text for the field.
     */
    val description: String = properties.description

    // a state flow to handle user input changes
    private val _value = MutableStateFlow(initialValue)

    /**
     * Current value state for the field.
     */
    val value: StateFlow<String> = combine(
        _value,
        properties.value,
        properties.editable
    ) { userEdit, exprResult, editable ->
        // transform the user input value flow with the formElement value and required into a single
        // value flow based on if the field is editable
        if (editable) {
            userEdit
        } else {
            exprResult
        }
    }.stateIn(scope, SharingStarted.Eagerly, initialValue)

    /**
     * Property that indicates if the field is editable.
     */
    val isEditable: StateFlow<Boolean> = properties.editable

    /**
     * Property that indicates if the field is required.
     */
    val isRequired: StateFlow<Boolean> = properties.required

    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    fun onValueChanged(input: String) {
        onEditValue(input)
        _value.value = input
        scope.launch { onEvaluateExpression() }
    }

    /**
     * Evaluates the underlying expressions for this field. The results can be observed through the
     * [value], [isRequired] and [isEditable] state flows.
     */
//    private suspend fun evaluateExpressions() {
//        featureForm.evaluateExpressions()
//    }

    /**
     * Set the value in the feature's attribute map.
     * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
     * and refresh the feature.
     */
//    private fun editValue(value: Any?) {
//        featureForm.editValue(formElement, value)
//    }
}
