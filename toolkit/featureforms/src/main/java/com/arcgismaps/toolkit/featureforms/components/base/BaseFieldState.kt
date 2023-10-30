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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

internal open class FieldProperties(
    val label: String,
    val placeholder: String,
    val description: String,
    val value: StateFlow<String>,
    val required: StateFlow<Boolean>,
    val editable: StateFlow<Boolean>,
    val visible: StateFlow<Boolean>
)

/**
 * Base state class for any Field within a feature form. It provides the default set of properties
 * that are common to all [FieldFormElement]'s.
 *
 * @param properties the [FieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [FieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [BaseFieldState.onValueChanged].
 */
internal open class BaseFieldState(
    properties: FieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    protected val onEditValue: (Any?) -> Unit,
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
    protected val _value = MutableStateFlow(initialValue)

    /**
     * Current value state for the field.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val value: StateFlow<String> = flowOf(_value, properties.value.drop(1))
        .flattenMerge()
        .stateIn(scope, SharingStarted.Eagerly, initialValue)

    /**
     * Property that indicates if the field is editable.
     */
    val isEditable: StateFlow<Boolean> = properties.editable

    /**
     * Property that indicates if the field is required.
     */
    val isRequired: StateFlow<Boolean> = properties.required

    /**
     * Property that indicates if the field is visible.
     */
    val isVisible: StateFlow<Boolean> = properties.visible
   
    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    open fun onValueChanged(input: String) {
        onEditValue(input)
        _value.value = input
    }
}
