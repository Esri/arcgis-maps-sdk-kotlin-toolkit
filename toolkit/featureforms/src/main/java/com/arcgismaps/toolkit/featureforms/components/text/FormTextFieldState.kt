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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.ExactCharConstraint
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.MaxCharConstraint
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.MaxNumericConstraint
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.MinMaxCharConstraint
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.MinMaxNumericConstraint
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.MinNumericConstraint
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.NoError
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.NotANumber
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.NotAWholeNumber
import com.arcgismaps.toolkit.featureforms.components.text.ValidationErrorState.Required
import com.arcgismaps.toolkit.featureforms.utils.asDoubleTuple
import com.arcgismaps.toolkit.featureforms.utils.asLongTuple
import com.arcgismaps.toolkit.featureforms.utils.domain
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import com.arcgismaps.toolkit.featureforms.utils.isFloatingPoint
import com.arcgismaps.toolkit.featureforms.utils.isIntegerType
import com.arcgismaps.toolkit.featureforms.utils.isNumeric
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
    visible: StateFlow<Boolean>,
    val fieldType: FieldType,
    val domain: Domain?,
    val singleLine: Boolean,
    val minLength: Int,
    val maxLength: Int,
) : FieldProperties(label, placeholder, description, value, required, editable, visible)

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
    
    private var hasBeenFocused: Boolean = false
    
    // supporting text will depend on multiple other states. If there is an error, it will display
    // error message. Otherwise description is displayed, unless it is empty in which case
    // the helper text is displayed when the field is focused.
    private val _supportingText: MutableState<String> = mutableStateOf(description)
    val supportingText: State<String> = _supportingText
    
    private val _isFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()
    
    private val _hasError = mutableStateOf(false)
    private val _supportingTextIsErrorMessage = mutableStateOf(false)
    val supportingTextIsErrorMessage: State<Boolean> = _supportingTextIsErrorMessage
    
    /**
     * The domain of the element's field.
     */
    val domain: Domain? = properties.domain
    
    /**
     * The FieldType of the element's field.
     */
    val fieldType: FieldType = properties.fieldType
    
    private val errorMessages: MutableMap<ValidationErrorState, String> by lazy {
        val min = if (domain is RangeDomain) {
            (domain.minValue as? Number)?.format()
        } else {
            ""
        }
        
        val max = if (domain is RangeDomain) {
            (domain.maxValue as? Number)?.format()
        } else {
            ""
        }
        
        mutableMapOf(
            Required to context.getString(R.string.required),
            MaxCharConstraint to context.getString(R.string.maximum_n_chars, if (maxLength > 0) maxLength else 254),
            ExactCharConstraint to context.getString(R.string.enter_n_chars, minLength),
            MinMaxCharConstraint to context.getString(R.string.enter_min_to_max_chars, minLength, maxLength),
            MinNumericConstraint to context.getString(R.string.less_than_min_value, min),
            MaxNumericConstraint to context.getString(R.string.exceeds_max_value, max),
            MinMaxNumericConstraint to context.getString(R.string.numeric_range_helper_text, min, max),
            NotANumber to context.getString(R.string.value_must_be_a_number),
            NotAWholeNumber to context.getString(R.string.value_must_be_a_whole_number)
        )
    }
    
    // build helper text
    private val helperText =
        if (fieldType.isNumeric) {
            if (domain != null && domain is RangeDomain) {
                val min = domain.minValue
                val max = domain.maxValue
                // to format the range of either integer or floating point
                // values without a lot of logic, they are formatted as strings.
                if (min is Number && max is Number) {
                    context.getString(R.string.numeric_range_helper_text, min.format(), max.format())
                } else if (min is Number) {
                    context.getString(R.string.less_than_min_value, min.format())
                } else if (max is Number) {
                    context.getString(R.string.exceeds_max_value, max.format())
                } else {
                    // not likely to happen.
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
                updateValidation(newValue)
            }
        }
        scope.launch {
            isRequired.drop(1).collect {
                updateValidation(value.value)
            }
        }
        scope.launch {
            isFocused.drop(1).collect {
                if (it) {
                    hasBeenFocused = true
                }
                updateValidation(value.value)
            }
        }
    }
    
    private fun updateValidation(value: String) {
        val errors = validate(value)
        val errorToDisplay = errorMessageToDisplay(value, errors)
        _supportingTextIsErrorMessage.value = errorToDisplay != NoError
        _supportingText.value = if (errorToDisplay != NoError) {
            errorMessages[errorToDisplay] ?: throw IllegalStateException("validation error must have a message")
        } else {
            description.ifEmpty {
                if (_isFocused.value) helperText else ""
            }
        }
        _hasError.value = errors.isNotEmpty()
    }
    
    private fun validateTextRange(value: String): ValidationErrorState =
        if (value.length !in minLength..maxLength) {
            if (minLength > 0 && maxLength > 0) {
                if (minLength == maxLength) {
                    ExactCharConstraint
                } else {
                    MinMaxCharConstraint
                }
            } else {
                MaxCharConstraint
            }
        } else {
            NoError
        }
    
    private fun validateNumber(value: String): ValidationErrorState =
        if (fieldType.isIntegerType) {
            val numberVal = value.toIntOrNull()
            if (numberVal == null) {
                NotAWholeNumber
            } else {
                validateNumericRange(numberVal)
            }
        } else {
            val numberVal = value.toDoubleOrNull()
            if (numberVal == null) {
                NotANumber
            } else {
                validateNumericRange(numberVal)
            }
        }
    
    private fun validateNumericRange(numberVal: Int): ValidationErrorState {
        require(fieldType.isIntegerType)
        return if (domain != null && domain is RangeDomain) {
            val (min, max) = domain.asLongTuple
            if (min != null && max != null) {
                if (numberVal in min..max) {
                    NoError
                } else {
                    MinMaxNumericConstraint
                }
            } else if (min != null) {
                if (min <= numberVal) {
                    NoError
                } else {
                    MinNumericConstraint
                }
            } else if (max != null) {
                if (numberVal <= max) {
                    NoError
                } else {
                    MaxNumericConstraint
                }
            } else {
                NoError
            }
        } else {
            NoError
        }
    }
    
    private fun validateNumericRange(numberVal: Double): ValidationErrorState {
        require(fieldType.isFloatingPoint)
        return if (domain != null && domain is RangeDomain) {
            val (min, max) = domain.asDoubleTuple
            if (min != null && max != null) {
                if (numberVal in min..max) {
                    NoError
                } else {
                    MinMaxNumericConstraint
                }
            } else if (min != null) {
                if (min <= numberVal) {
                    NoError
                } else {
                    MinNumericConstraint
                }
            } else if (max != null) {
                if (numberVal <= max) {
                    NoError
                } else {
                    MaxNumericConstraint
                }
            } else {
                NoError
            }
        } else {
            NoError
        }
    }
    
    private fun errorMessageToDisplay(
        value: String,
        validationErrors: List<ValidationErrorState>
    ): ValidationErrorState =
        if (validationErrors.isEmpty()) {
            NoError
        } else if (isFocused.value) {
            if (value.isEmpty()) {
                // if focused and empty, don't show the "Required" error or numeric parse errors
                validationErrors.firstOrNull { it != Required && it != NotANumber && it != NotAWholeNumber } ?: NoError
            } else {
                // if non empty, focused, show any error other than required (the Required error shouldn't be in the list)
                check (!validationErrors.contains(Required))
                validationErrors.first()
            }
        } else if (hasBeenFocused) {
            if (value.isEmpty()) {
                if (validationErrors.contains(Required)) {
                    // show any non required and non parse error before showing a Required error
                    validationErrors.firstOrNull { it != Required && it != NotANumber && it != NotAWholeNumber  } ?: Required
                } else {
                    // don't show parse errors when empty and not required (and when required, show required as above)
                    validationErrors.firstOrNull { it != NotANumber && it != NotAWholeNumber  } ?: NoError
                }
            } else {
                // if non empty, unfocused, show any error other than required (the Required error shouldn't be in the list)
                check (!validationErrors.contains(Required))
                validationErrors.first()
            }
        } else {
            // never been focused
            NoError
        }
    
    private fun validate(value: String): MutableList<ValidationErrorState> {
        val ret = mutableListOf<ValidationErrorState>()
        if (isRequired.value && value.isEmpty()) {
            ret += Required
        }
        
        if (!fieldType.isNumeric) {
            val rangeError = validateTextRange(value)
            if (rangeError != NoError) {
                ret += rangeError
            }
        } else {
            val error = validateNumber(value)
            if (error != NoError) {
                ret += error
            }
        }
        
        return ret
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
                    it.hasBeenFocused
                )
            },
            restore = { list ->
                val minLength = (formElement.input as? TextBoxFormInput)?.minLength ?: (formElement.input as TextAreaFormInput).minLength
                val maxLength = (formElement.input as? TextBoxFormInput)?.maxLength ?: (formElement.input as TextAreaFormInput).maxLength
                FormTextFieldState(
                    properties = TextFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.value,
                        required = formElement.isRequired,
                        editable = formElement.isEditable,
                        visible = formElement.isVisible,
                        domain = form.domain(formElement) as? RangeDomain,
                        fieldType = form.fieldType(formElement),
                        singleLine = formElement.input is TextBoxFormInput,
                        minLength = minLength.toInt(),
                        maxLength = maxLength.toInt()
                    ),
                    initialValue = list[0] as String,
                    scope = scope,
                    context = context,
                    onEditValue = { newValue ->
                        form.editValue(formElement, newValue)
                        scope.launch { form.evaluateExpressions() }
                    },
                ).apply {
                    // focus is lost on rotation. https://devtopia.esri.com/runtime/apollo/issues/230
                    hasBeenFocused = list[1] as Boolean
                    updateValidation(list[0] as String)
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
            visible = field.isVisible,
            singleLine = field.input is TextBoxFormInput,
            fieldType = form.fieldType(field),
            domain = form.domain(field) as? RangeDomain,
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

private sealed class ValidationErrorState {
    object NoError: ValidationErrorState()
    object Required: ValidationErrorState()
    object MinMaxCharConstraint: ValidationErrorState()
    object ExactCharConstraint: ValidationErrorState()
    object MaxCharConstraint: ValidationErrorState()
    object MinNumericConstraint: ValidationErrorState()
    object MaxNumericConstraint: ValidationErrorState()
    object MinMaxNumericConstraint: ValidationErrorState()
    object NotANumber: ValidationErrorState()
    object NotAWholeNumber: ValidationErrorState()
}

/**
 * Provide a format string for any numeric type.
 *
 * @param digits: If the number is floating point, restricts the decimal digits
 * @return a formatted string representing the number.
 */
private fun Number.format(digits: Int = 2): String =
    when (this) {
        is Double -> "%.${digits}f".format(this)
        is Float -> "%.${digits}f".format(this)
        else -> "$this"
    }
