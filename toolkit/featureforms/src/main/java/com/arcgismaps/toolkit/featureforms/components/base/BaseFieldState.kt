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

import android.util.Log
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

    // a state flow to handle user input changes
    protected val _value = MutableStateFlow(initialValue)


    @OptIn(ExperimentalCoroutinesApi::class)
    private val _mergedValue: StateFlow<T> = flowOf(_value, properties.value.drop(1))
        .flattenMerge()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = initialValue
        )

    private val _validationError: MutableStateFlow<ValidationErrorState> =
        MutableStateFlow(ValidationErrorState.NoError)

    /**
     * Current value state for the field.
     *
     * ---validation behavior---
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

    private val _isFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()

    protected var wasFocused = false

    init {
        scope.launch {
            // validate when focus changes
            isFocused.collect {
                updateValidation(_mergedValue.value)
            }
        }
        scope.launch {
            // validate when required property changes
            isRequired.collect {
                updateValidation(_mergedValue.value)
            }
        }
        scope.launch {
            // validate when the editable property changes
            isEditable.collect {
                updateValidation(_mergedValue.value)
            }
        }
        scope.launch {
            // validate when the value changes
            _mergedValue.collect {
                updateValidation(_mergedValue.value)
            }
        }
    }

    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    open fun onValueChanged(input: T) {
        onEditValue(input)
        _value.value = input
    }

    fun onFocusChanged(focus: Boolean) {
        if (focus) wasFocused = true
        _isFocused.value = focus
    }

    open fun validate(value: T): List<ValidationErrorState> {
        val errors = defaultValidator()
        return buildList {
            if (errors.any { it is FeatureFormValidationException.RequiredException }) {
                add(ValidationErrorState.Required)
            }
        }
    }

    private fun filter(errors: List<ValidationErrorState>): ValidationErrorState {
        Log.e("TAG", "filtering: $errors with wasfocused:$wasFocused")
        return if (errors.isNotEmpty()) {
            if (!isEditable.value) {
                ValidationErrorState.NoError
            } else {
                if (wasFocused) {
                    if (!isFocused.value) {
                        if (errors.any { it is ValidationErrorState.Required }) {
                            ValidationErrorState.Required
                        } else {
                            errors.first()
                        }
                    } else {
                        if (errors.any { it !is ValidationErrorState.Required }) {
                            errors.first()
                        } else {
                            ValidationErrorState.NoError
                        }
                    }
                } else {
                    ValidationErrorState.NoError
                }
            }
        } else {
            ValidationErrorState.NoError
        }
    }

    private fun updateValidation(value: T) {
        _validationError.value = filter(validate(value))
    }
}
