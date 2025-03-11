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

package com.arcgismaps.toolkit.featureforms.internal.components.base

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.utils.asDoubleTuple
import com.arcgismaps.toolkit.featureforms.internal.utils.asLongTuple
import com.arcgismaps.toolkit.featureforms.internal.utils.isFloatingPoint
import com.arcgismaps.toolkit.featureforms.internal.utils.isIntegerType
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Immutable
internal open class FieldProperties<T>(
    val label: String,
    val placeholder: String,
    val description: String,
    val value: StateFlow<T>,
    val validationErrors: StateFlow<List<ValidationErrorState>>,
    val required: StateFlow<Boolean>,
    val editable: StateFlow<Boolean>,
    val visible: StateFlow<Boolean>,
    val fieldType: FieldType,
    val domain: Domain?
)

/**
 * A class that provides a validation error [error] for the value of [data].
 */
@Immutable
internal data class Value<T>(
    val data: T,
    val error: ValidationErrorState = ValidationErrorState.NoError
)

/**
 * Base state class for any Field within a feature form. It provides the default set of properties
 * that are common to all [FieldFormElement]'s.
 *
 * @param id Unique identifier for the field.
 * @param properties the [FieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [FieldProperties.value] by default.
 * @property hasValueExpression a flag to indicate if the field has a value expression.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param updateValue a function that is invoked when the user edits result in a change of value. This
 * is called in [BaseFieldState.onValueChanged].
 * @param evaluateExpressions a function that is invoked to evaluate all form expressions. This is
 * called after a successful [updateValue].
 */
internal abstract class BaseFieldState<T>(
    id: Int,
    properties: FieldProperties<T>,
    initialValue: T = properties.value.value,
    val hasValueExpression: Boolean,
    private val scope: CoroutineScope,
    private val updateValue: (Any?) -> Unit,
    private val evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>,
) : FormElementState(
    id = id,
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
     * The [FieldType] of the field.
     */
    val fieldType = properties.fieldType

    /**
     * The domain for this field, if any.
     */
    val domain = properties.domain

    /**
     * Text that indicates instructions or constraints for the field. This is derived from the
     * properties of field and represented as a [ValidationErrorState]. See [calculateHelperText]
     * on how to provide custom helper text.
     *
     * Use [ValidationErrorState.getString] to get the actual text from within a composition.
     */
    val helperText: ValidationErrorState by lazy {
        calculateHelperText()
    }

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
     * sets the value on the feature using [updateValue] and calls [evaluateExpressions].
     */
    fun onValueChanged(input: T) {
        // set the ui state immediately with the current error if any
        _value.value = Value(input, _value.value.error)
        // update the attributes
        updateValue(typeConverter(input))
        // evaluate expressions
        scope.launch {
            evaluateExpressions().onSuccess {
                if (it.isNotEmpty()) {
                    Log.e("FeatureForm", "Errors found while evaluating expressions:")
                    it.forEach { error ->
                        Log.e("FeatureForm", "Evaluation Error:", error.error)
                    }
                }
            }
        }
    }

    /**
     * Changes the current focus state for the field. Use [isFocused] to read the value.
     */
    fun onFocusChanged(focus: Boolean) {
        _isFocused.value = focus
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
    private fun filterErrors(errors: List<ValidationErrorState>): ValidationErrorState =
        when {
            // if there are no errors
            hasNoErrors(errors) -> ValidationErrorState.NoError
            // if the field has a value expression
            hasValueExpression -> errors.first()
            // if the field was focused and is focused
            isFocused.value -> handleFocusedErrors(errors)
            // if the field was focused but is not currently focused
            !isFocused.value -> handleNonFocusedErrors(errors)
            // if the field has never been focused
            else -> ValidationErrorState.NoError
        }


    /**
     * Implement this method to provide the proper type conversion from [T] to an Any?. This
     * method is used by [onValueChanged] to cast the [input] before calling
     * [FieldFormElement.updateValue].
     *
     * @param input The value to convert
     */
    abstract fun typeConverter(input: T): Any?

    /**
     * This method is called when [helperText] is accessed to provide the helper text for the field.
     * The default implementation only handles a [RangeDomain] with numeric fields. Override this
     * method to provide custom helper text.
     */
    open fun calculateHelperText(): ValidationErrorState {
        return when {
            fieldType.isNumeric && domain is RangeDomain -> {
                val (min: Number?, max: Number?) = when {
                    fieldType.isIntegerType -> {
                        val tuple = domain.asLongTuple
                        Pair(tuple.min, tuple.max)
                    }

                    fieldType.isFloatingPoint -> {
                        val tuple = domain.asDoubleTuple
                        Pair(tuple.min, tuple.max)
                    }

                    else -> Pair(null, null)
                }
                handleNumericConstraints(min, max, hasValueExpression)
            }

            else -> ValidationErrorState.NoError
        }
    }
}

/**
 * Returns true if the field should not show any errors.
 */
private fun BaseFieldState<*>.hasNoErrors(errors: List<ValidationErrorState>): Boolean {
    return errors.isEmpty() || !(hasValueExpression || isEditable.value)
}

/**
 * Returns the appropriate error to show when the field is focused.
 */
private fun BaseFieldState<*>.handleFocusedErrors(errors: List<ValidationErrorState>): ValidationErrorState {
    // if is a text field that is empty and has a description do not show any error
    return if (value.value.data is String && (value.value.data as String).isEmpty() && description.isNotEmpty()) {
        ValidationErrorState.NoError
    } else {
        // show the first non-required error
        errors.firstOrNull { it !is ValidationErrorState.Required } ?: ValidationErrorState.NoError
    }
}

/**
 * Returns the appropriate error to show when the field is not focused.
 */
private fun BaseFieldState<*>.handleNonFocusedErrors(errors: List<ValidationErrorState>): ValidationErrorState {
    // show a required error if it is present
    return if (errors.any { it is ValidationErrorState.Required }) {
        ValidationErrorState.Required
    } else {
        // show any other error
        errors.first()
    }
}
