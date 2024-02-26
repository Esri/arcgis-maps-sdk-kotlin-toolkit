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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.featureforms.FieldFormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal open class FieldProperties<T>(
    val label: String,
    val placeholder: String,
    val description: String,
    val value: StateFlow<T>,
    val validationErrors: StateFlow<List<ValidationErrorState>>,
    val required: StateFlow<Boolean>,
    val editable: StateFlow<Boolean>,
    val visible: StateFlow<Boolean>
)

/**
 * A class that provides a validation error [error] for the value of [data].
 */
internal data class Value<T>(
    val data: T,
    val error: ValidationErrorState = ValidationErrorState.NoError
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
internal abstract class BaseFieldState<T>(
    properties: FieldProperties<T>,
    initialValue: T = properties.value.value,
    private val scope: CoroutineScope,
    protected val onEditValue: (Any?) -> Unit
) : FormElementState(
    label = properties.label,
    description = properties.description,
    isVisible = properties.visible
) {
    /**
     * Placeholder hint for the field.
     */
    open val placeholder: String = properties.placeholder

    /**
     * A state flow to handle calculated value changes.
     */
    private val _attributeValue = properties.value

    /**
     * Backing mutable state for the [value].
     */
    private val _value: MutableState<Value<T>> = mutableStateOf(Value(initialValue))

    /**
     * Current value for this field state. The actual data of this type is wrapped in a [Value]
     * object. The [Value.error] provides the current validation error for the [Value.data]. Use
     * [onValueChanged] to set a value for this state.
     */
    val value: State<Value<T>> = _value

    /**
     * Property that indicates if the field is editable.
     */
    val isEditable: StateFlow<Boolean> = properties.editable

    /**
     * Property that indicates if the field is required.
     */
    val isRequired: StateFlow<Boolean> = properties.required

    /**
     * The validation errors for this field.
     */
    val validationErrors: StateFlow<List<ValidationErrorState>> = properties.validationErrors

    /**
     * A mutable state flow to handle current focus state.
     */
    private val _isFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Property that indicates if the field for this state currently has focus. Use [onFocusChanged]
     * to set this value.
     */
    val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()

    /**
     * A flag to indicate if the field ever gained focus.
     */
    protected var wasFocused = false

    init {
        scope.launch {
            // combine the attribute and validation errors flow so that we always have the latest
            // value for both
            combine(_attributeValue, validationErrors) { newValue, errors ->
                Pair(newValue, errors)
            }.collect {
                // validate with the latest value and validation errors
                updateValueWithValidation(it.first, it.second)
            }
        }
        scope.launch {
            // validate when focus changes
            isFocused.collect {
                updateValueWithValidation(_value.value.data, validationErrors.value)
            }
        }
        scope.launch {
            // validate when the editable property changes
            isEditable.collect {
                updateValueWithValidation(_value.value.data, validationErrors.value)
            }
        }
    }

    /**
     * Callback to update the current value of this state object to the given [input]. This also
     * sets the value on the feature using [onEditValue].
     */
    fun onValueChanged(input: T) {
        // infer that a value change event comes from a user interaction and hence treat it as a
        // focus event
        wasFocused = true
        // set the ui state immediately
        _value.value = Value(input)
        // update the attributes
        onEditValue(typeConverter(input))
    }

    /**
     * Changes the current focus state for the field. Use [isFocused] to read the value.
     */
    fun onFocusChanged(focus: Boolean) {
        if (focus) wasFocused = true
        _isFocused.value = focus
    }

    /**
     * Forces the validation of this field irrespective of the current focus state [isFocused] and
     * generates any validation errors via the [value] property. Avoid calling this method in any
     * open/abstract class constructors since it indirectly invokes open members.
     */
    fun forceValidation() {
        wasFocused = true
        updateValueWithValidation(_value.value.data, validationErrors.value)
    }

    /**
     * Updates the [value] with a validation error if any, using [filterErrors].
     */
    private fun updateValueWithValidation(value: T, errors: List<ValidationErrorState>) {
        val error = filterErrors(errors)
        // update the value with the validation error.
        _value.value = Value(value, error)
    }

    /**
     * Filters a list of validation errors using the "field validation ui messaging algorithm"
     * and returns a single validation error based on the current focus state, editable state
     * and the value.
     *
     * @param errors the list of validation errors
     * @return A single validation error
     */
    private fun filterErrors(errors: List<ValidationErrorState>): ValidationErrorState {
        // if editable
        return if (errors.isNotEmpty() && isEditable.value) {
            // if it has been focused
            if (wasFocused) {
                // if not in focus
                if (!isFocused.value) {
                    // show a required error if it is present
                    if (errors.any { it is ValidationErrorState.Required }) {
                        ValidationErrorState.Required
                    } else {
                        // show any other error
                        errors.first()
                    }
                } else {
                    // if focused and empty, don't show the "Required" error or numeric parse errors
                    if (value.value.data is String && (value.value.data as String).isEmpty()) {
                        ValidationErrorState.NoError
                    } else {
                        // show the first non-required error
                        errors.firstOrNull { it !is ValidationErrorState.Required }
                        // if none is found, do not show any error
                            ?: ValidationErrorState.NoError
                    }
                }
            } else {
                // never been focused
                ValidationErrorState.NoError
            }
        } else {
            // not editable
            ValidationErrorState.NoError
        }
    }

    /**
     * Implement this method to provide the proper type conversion from [T] to an Any?. This
     * method is used by [onValueChanged] to cast the [input] before calling
     * [FieldFormElement.updateValue].
     *
     * @param input The value to convert
     */
    abstract fun typeConverter(input: T): Any?
}
