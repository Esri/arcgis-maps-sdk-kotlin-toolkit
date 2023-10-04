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

package com.arcgismaps.toolkit.featureforms.components.text

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.RangeDomain
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.FieldElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.utils.isFloatingPoint
import com.arcgismaps.toolkit.featureforms.utils.isIntegerType
import com.arcgismaps.toolkit.featureforms.utils.isNumeric
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.utils.editValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

internal class TextFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    val singleLine: Boolean,
    val minLength: Int,
    val maxLength: Int,
) : FieldProperties(label, placeholder, description, value, required, editable)

/**
 * A class to handle the state of a [FormTextField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [TextFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param context a Context scoped to the lifetime of a call to the [FieldElement] composable function.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [FormTextFieldState.onValueChanged].
 */
@Stable
internal class FormTextFieldState(
    properties: TextFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    private val context: Context,
    onEditValue: (Any?) -> Unit
) : BaseFieldState(
    properties = properties,
    initialValue = initialValue,
    scope = scope,
    onEditValue = onEditValue
) {
    // indicates singleLine only if TextBoxFeatureFormInput
    val singleLine = properties.singleLine

    // fetch the minLength based on the featureFormElement.inputType
    val minLength = properties.minLength

    // fetch the maxLength based on the featureFormElement.inputType
    val maxLength = properties.maxLength

    // supporting text will depend on multiple other states. If there is an error, it will display
    // error message. Otherwise description is displayed, unless it is empty in which case
    // the helper text is displayed when the field is focused.
    val supportingText = derivedStateOf {
        if (_hasError.value) _errorMessage else {
            description.ifEmpty {
                if (_isFocused.value) helperText else ""
            }
        }
    }

    private val _isFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()

    private val _hasError = mutableStateOf(false)
    val hasError: State<Boolean> = _hasError

    private var _errorMessage: String = ""

    // build helper text
    private val helperText =
        if (fieldType.isNumeric) {
            val domain = formElement.domain
            
            if (domain != null && domain is RangeDomain) {
                val min = domain.minValue as? Number
                val max = domain.maxValue as? Number
                
                if (min != null && max != null) {
                    context.getString(R.string.numeric_range_helper_text, min.toInt(), max.toInt())
                } else if (min != null) {
                    context.getString(R.string.less_than_min_value)
                } else if (max != null) {
                    context.getString(R.string.exceeds_max_value)
                } else {
                    ""
                }
            } else {
                ""
            }

        } else {
            if (minLength > 0 && maxLength > 0) {
                if (minLength == maxLength) {
                    context.getString(R.string.enter_n_chars, minLength)
                } else {
                    context.getString(R.string.enter_min_to_max_chars, minLength, maxLength)
                }
            } else if (maxLength > 0) {
                context.getString(R.string.maximum_n_chars, maxLength)
            } else {
                context.getString(R.string.maximum_n_chars, 254)
            }
        }

    init {
        scope.launch {
            value.drop(1).collect { newValue ->
                if (isEditable.value) {
                    validate(newValue, true)
                }
            }
        }
        scope.launch {
            isFocused.collect {
                if (it) {
                    validate(value.value, true)
                } else {
                    validate(value.value, !isRequired.value)
                }
            }
        }
    }
    
    private fun validateNumericRange(value: String): Boolean {
        require(fieldType.isNumeric)
        return if (domain != null && domain is RangeDomain) {
            val min = domain.minValue as? Number
            val max = domain.maxValue as? Number
            val doubleVal = value.toDouble()
            if (min != null && max != null) {
                doubleVal in min.toDouble()..max.toDouble()
            } else if (min != null) {
                min.toDouble() >= doubleVal
            } else if (max != null) {
                doubleVal <= max.toDouble()
            } else {
                true
            }
        } else {
            true
        }
    }

    /**
     * Validates the current [value]'s length based on the [minLength], [maxLength], and [isRequired] and sets the
     * [hasError] and [_errorMessage] if there was an error in validation.
     */
    private fun validate(value: String, canBeEmpty: Boolean) {
        _hasError.value = if (!canBeEmpty && value.isEmpty()) {
            _errorMessage = context.getString(R.string.required)
            true
        } else if (!fieldType.isNumeric && value.length !in minLength..maxLength) {
            _errorMessage = helperText
            true
        } else if (fieldType.isNumeric) {
            if (value.isEmpty() && canBeEmpty) {
                false
            } else if (fieldType.isIntegerType && value.toIntOrNull() == null) {
                _errorMessage = context.getString(R.string.value_must_be_a_whole_number)
                true
            } else if (fieldType.isFloatingPoint && value.toDoubleOrNull() == null) {
                _errorMessage = context.getString(R.string.value_must_be_a_number)
                true
            } else if (!validateNumericRange(value)) {
                _errorMessage = helperText
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun onFocusChanged(focus: Boolean) {
        _isFocused.value = focus
    }

    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            context: Context,
            scope: CoroutineScope
        ): Saver<FormTextFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value,
                    it.singleLine,
                    it.minLength,
                    it.maxLength,
                    it.hasError.value,
                    it._errorMessage
                )
            },
            restore = { list ->
                FormTextFieldState(
                    properties = TextFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.value,
                        required = formElement.isRequired,
                        editable = formElement.isEditable,
                        singleLine = list[1] as Boolean,
                        minLength = list[2] as Int,
                        maxLength = list[3] as Int
                    ),
                    initialValue = list[0] as String,
                    scope = scope,
                    context = context,
                    onEditValue = { newValue ->
                        form.editValue(formElement, newValue)
                        scope.launch { form.evaluateExpressions() }
                    },
                ).apply {
                    _hasError.value = list[4] as Boolean
                    _errorMessage = list[5] as String
                }
            }
        )
    }
}

@Composable
internal fun rememberFormTextFieldState(
    field: FieldFormElement,
    minLength: Int,
    maxLength: Int,
    form: FeatureForm,
    context: Context,
    scope: CoroutineScope
): FormTextFieldState = rememberSaveable(
    saver = FormTextFieldState.Saver(field, form, context, scope)
) {
    FormTextFieldState(
        properties = TextFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            editable = field.isEditable,
            required = field.isRequired,
            singleLine = field.input is TextBoxFormInput,
            minLength = minLength,
            maxLength = maxLength,
        ),
        scope = scope,
        context = context,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        }
    )
}
