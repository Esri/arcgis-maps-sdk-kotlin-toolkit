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

import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Base state class for any Field within a feature form. It provides the default set of properties
 * that are common to all [FieldFormElement]'s.
 */
internal open class BaseFieldState(
    private val formElement: FieldFormElement,
    private val featureForm: FeatureForm,
    private val scope: CoroutineScope
) {
    /**
     * Title for the field.
     */
    open val label: String = formElement.label

    /**
     * Description text for the field.
     */
    val description: String = formElement.description
    
    val fieldType: FieldType = featureForm.fieldType(formElement)
    
    val domain: Domain? = formElement.domain

    /**
     * Placeholder hint for the field.
     */
    open val placeholder: String = formElement.hint

    // a state flow to handle user input changes
    private val _value = MutableStateFlow(formElement.value.value)

    /**
     * Current value state for the field.
     */
    val value: StateFlow<String> = combine(
        _value,
        formElement.value,
        formElement.isEditable
    ) { userEdit, exprResult, editable ->
        // transform the user input value flow with the formElement value and required into a single
        // value flow based on if the field is editable
        if (editable) {
            userEdit
        } else {
            exprResult
        }
    }.stateIn(scope, SharingStarted.Eagerly, _value.value)

    /**
     * Property that indicates if the field is editable.
     */
    val isEditable: StateFlow<Boolean> = formElement.isEditable

    /**
     * Property that indicates if the field is required.
     */
    val isRequired: StateFlow<Boolean> = formElement.isRequired

    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    fun onValueChanged(input: String) {
        editValue(input)
        _value.value = input
        scope.launch { evaluateExpressions() }
    }

    /**
     * Evaluates the underlying expressions for this field. The results can be observed through the
     * [value], [isRequired] and [isEditable] state flows.
     */
    private suspend fun evaluateExpressions() {
        featureForm.evaluateExpressions()
    }

    /**
     * Set the value in the feature's attribute map.
     * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
     * and refresh the feature.
     */
    private fun editValue(value: Any?) {
        featureForm.editValue(formElement, value)
    }
}
