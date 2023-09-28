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

import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.utils.editValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Base interface for any Field within a feature form. It provides the default set of properties
 * that are common to all [FieldFormElement]'s.
 */
internal interface BaseFieldState {
    /**
     * Title for the field.
     */
    val label: String

    /**
     * Description text for the field.
     */
    val description: String

    /**
     * Placeholder hint for the field.
     */
    val placeholder: String

    /**
     * Current value state for the field.
     */
    val value: StateFlow<String>

    /**
     * Property that indicates if the field is editable.
     */
    val isEditable : StateFlow<Boolean>

    /**
     * Property that indicates if the field is required.
     */
    val isRequired : StateFlow<Boolean>

    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    fun onValueChanged(input: String)

    suspend fun evaluateExpressions()
}

/**
 * Default implementation for [BaseFieldState]. See [BaseFieldState()] for the factory.
 */
private class BaseFieldStateImpl(
    private val formElement: FieldFormElement,
    private val featureForm: FeatureForm,
    private val scope: CoroutineScope
) : BaseFieldState {

    private val _value = MutableStateFlow(formElement.value.value)

    override val value: StateFlow<String> =
        combine(_value, formElement.value, formElement.isEditable) { userEdit, exprResult, editable ->
            if (editable) {
                userEdit
            } else {
                exprResult
            }
        }.stateIn(scope, SharingStarted.Eagerly, _value.value)

    override val isEditable: StateFlow<Boolean> = formElement.isEditable

    override val isRequired: StateFlow<Boolean> = formElement.isRequired

    override val description: String = formElement.description

    override val placeholder: String = formElement.hint

    // set the label from the FieldFeatureFormElement
    // note when isRequired becomes a StateFlow, this logic will move into the compose function
    override val label = formElement.label
//    if (!isRequired.value) {
//        formElement.label
//    } else {
//        "${formElement.label} *"
//    }

    override fun onValueChanged(input: String) {
        editValue(input)
        _value.value = input
        scope.launch { evaluateExpressions() }
    }

    override suspend fun evaluateExpressions() {
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

/**
 * Factory function to create a [BaseFieldState].
 *
 * @param formElement The [FieldFormElement] to create the state from.
 * @param featureForm The [FeatureForm] that the [formElement] is a part of.
 */
internal fun BaseFieldState(
    formElement: FieldFormElement,
    featureForm: FeatureForm,
    scope: CoroutineScope
): BaseFieldState = BaseFieldStateImpl(formElement, featureForm, scope)
