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

import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.featureforms.FieldFormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal open class FieldProperties<T>(
    val label: String,
    val placeholder: String,
    val description: String,
    val value: StateFlow<T>,
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
 * @param defaultValidator the default validator that returns the list of validation errors. This
 * is called in [BaseFieldState.validate].
 */
internal open class BaseFieldState<T>(
    properties: FieldProperties<T>,
    initialValue: T = properties.value.value,
    scope: CoroutineScope,
    protected val onEditValue: (Any?) -> Unit,
    protected val defaultValidator: () -> List<Throwable>
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
     * A mutable state flow to handle user input changes.
     */
    @Suppress("PropertyName")
    protected val _value = MutableStateFlow(initialValue)

    /**
     * A state flow that combines the user input [_value] and calculated property callbacks.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("PropertyName")
    protected val _mergedValue: StateFlow<T> = flowOf(_value, properties.value)
        .flattenMerge()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = initialValue
        )

    // a state flow for sending validation errors
    private val _validationError: MutableStateFlow<ValidationErrorState> =
        MutableStateFlow(ValidationErrorState.NoError)

    /**
     * Current value for this field state. The actual data of this type is wrapped in a [Value]
     * object. The [Value.error] provides the current validation error for the [Value.data].
     */
    val value: StateFlow<Value<T>> = combine(_mergedValue, _validationError) { newValue, error ->
        Value(newValue, error)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Value(_mergedValue.value)
    )

    /**
     * Property that indicates if the field is editable.
     */
    val isEditable: StateFlow<Boolean> = properties.editable

    /**
     * Property that indicates if the field is required.
     */
    val isRequired: StateFlow<Boolean> = properties.required

    /**
     * A mutable state flow to handle current focus state.
     */
    private val _isFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Property that indicates if the field for this state currently has focus. Use [onFocusChanged]
     * to set this value.
     */
    val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()

    protected var wasFocused = false

    init {
        // ignore the first values for all the flows since validate() is open and must
        // NOT be called during initialization due to any derived class initialization order
        scope.launch {
            // validate when focus changes
            isFocused.drop(1).collect {
                updateValidation()
            }
        }
        scope.launch {
            // validate when required property changes
            isRequired.drop(1).collect {
                updateValidation()
            }
        }
        scope.launch {
            // validate when the editable property changes
            isEditable.drop(1).collect {
                updateValidation()
            }
        }
        scope.launch {
            // validate when the value changes
            _mergedValue.drop(1).collect {
                updateValidation()
            }
        }
    }

    /**
     * Changes the current focus state for the field. Use [isFocused] to read the value.
     */
    fun onFocusChanged(focus: Boolean) {
        if (focus) wasFocused = true
        _isFocused.value = focus
    }

    /**
     * Filters a list of validation errors using the "field validation ui messaging algorithm"
     * and returns a single validation error based on the current focus state, editable state
     * and the value.
     *
     * @param errors the list of validation errors
     * @return A single validation error
     */
    private fun filter(errors: List<ValidationErrorState>): ValidationErrorState {
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
                    if (_mergedValue.value is String && (_mergedValue.value as String).isEmpty()) {
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
     * Runs and updates the validation using [validate] and [filter].
     */
    private fun updateValidation() {
        _validationError.value = filter(validate())
    }

    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    open fun onValueChanged(input: T) {
        // infer that a value change event comes from a user interaction and hence treat it as a
        // focus event
        wasFocused = true
        onEditValue(input)
        _value.value = input
    }

    /**
     * Validates the current value using the [defaultValidator].
     *
     * @return Returns the list of validation errors.
     */
    open fun validate(): List<ValidationErrorState> {
        val errors = defaultValidator()
        return buildList {
            if (errors.any { it is FeatureFormValidationException.RequiredException }) {
                add(ValidationErrorState.Required)
            }
        }
    }
}