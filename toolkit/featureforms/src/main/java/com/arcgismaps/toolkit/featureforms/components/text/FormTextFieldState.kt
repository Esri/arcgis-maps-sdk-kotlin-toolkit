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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.ExactCharConstraint
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.MaxCharConstraint
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.MaxNumericConstraint
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.MinMaxCharConstraint
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.MinMaxNumericConstraint
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.MinNumericConstraint
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.NoError
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.NotANumber
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState.NotAWholeNumber
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.utils.asDoubleTuple
import com.arcgismaps.toolkit.featureforms.utils.asLongTuple
import com.arcgismaps.toolkit.featureforms.utils.domain
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import com.arcgismaps.toolkit.featureforms.utils.isFloatingPoint
import com.arcgismaps.toolkit.featureforms.utils.isIntegerType
import com.arcgismaps.toolkit.featureforms.utils.isNumeric
import com.arcgismaps.toolkit.featureforms.utils.valueFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
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
) : FieldProperties<String>(label, placeholder, description, value, required, editable, visible)

/**
 * A class to handle the state of a [FormTextField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [TextFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [FormTextFieldState.onValueChanged].
 * @param defaultValidator the default validator that returns the list of validation errors. This
 * is called in [FormTextFieldState.validate].
 */
@Stable
internal class FormTextFieldState(
    properties: TextFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    onEditValue: (Any?) -> Unit,
    defaultValidator: () -> List<Throwable>
) : BaseFieldState<String>(
    properties = properties,
    initialValue = initialValue,
    scope = scope,
    onEditValue = onEditValue,
    defaultValidator = defaultValidator
) {
    // indicates singleLine only if TextBoxFeatureFormInput
    val singleLine = properties.singleLine

    // fetch the minLength based on the featureFormElement.inputType
    val minLength = properties.minLength

    // fetch the maxLength based on the featureFormElement.inputType
    val maxLength = properties.maxLength

    /**
     * The domain of the element's field.
     */
    val domain: Domain? = properties.domain

    /**
     * The FieldType of the element's field.
     */
    val fieldType: FieldType = properties.fieldType

    private fun validateNumericRange(numberVal: Int): ValidationErrorState {
        require(fieldType.isIntegerType)
        return if (domain != null && domain is RangeDomain) {
            val (min, max) = domain.asLongTuple
            if (min != null && max != null) {
                if (numberVal in min..max) {
                    NoError
                } else {
                    MinMaxNumericConstraint(min.format(), max.format())
                }
            } else if (min != null) {
                if (min <= numberVal) {
                    NoError
                } else {
                    MinNumericConstraint(min.format())
                }
            } else if (max != null) {
                if (numberVal <= max) {
                    NoError
                } else {
                    MaxNumericConstraint(max.format())
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
                    MinMaxNumericConstraint(min.format(), max.format())
                }
            } else if (min != null) {
                if (min <= numberVal) {
                    NoError
                } else {
                    MinNumericConstraint(min.format())
                }
            } else if (max != null) {
                if (numberVal <= max) {
                    NoError
                } else {
                    MaxNumericConstraint(max.format())
                }
            } else {
                NoError
            }
        } else {
            NoError
        }
    }

    override fun validate(): List<ValidationErrorState> {
        val currentValue = _mergedValue.value
        val coreErrors = defaultValidator()
        val errors = mutableListOf<ValidationErrorState>()
        errors += super.validate()

        if (!fieldType.isNumeric) {
            if (coreErrors.any { it is FeatureFormValidationException.MinCharConstraintException }
                || coreErrors.any { it is FeatureFormValidationException.MaxCharConstraintException }
            ) {
                if (minLength > 0 && maxLength > 0) {
                    if (minLength == maxLength) {
                        errors += ExactCharConstraint(minLength)
                    } else {
                        errors += MinMaxCharConstraint(minLength, maxLength)
                    }
                } else {
                    errors += MaxCharConstraint(maxLength)
                }
            }
        } else {
            if (fieldType.isIntegerType) {
                val numberVal = currentValue.toIntOrNull()
                if (numberVal == null) {
                    errors += NotAWholeNumber
                } else {
                    val error = validateNumericRange(numberVal)
                    if (error != NoError) {
                        errors += error
                    }
                }
            } else {
                val numberVal = currentValue.toDoubleOrNull()
                if (numberVal == null) {
                    errors += NotANumber
                } else {
                    val error = validateNumericRange(numberVal)
                    if (error != NoError) {
                        errors += error
                    }
                }
            }
        }

        return errors
    }

    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<FormTextFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value.data,
                    it.wasFocused
                )
            },
            restore = { list ->
                val minLength = (formElement.input as? TextBoxFormInput)?.minLength
                    ?: (formElement.input as TextAreaFormInput).minLength
                val maxLength = (formElement.input as? TextBoxFormInput)?.maxLength
                    ?: (formElement.input as TextAreaFormInput).maxLength
                FormTextFieldState(
                    properties = TextFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.valueFlow(scope),
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
                    onEditValue = { newValue ->
                        form.editValue(formElement, newValue)
                        scope.launch { form.evaluateExpressions() }
                    },
                    defaultValidator = { formElement.getValidationErrors() }
                ).apply {
                    // focus is lost on rotation. https://devtopia.esri.com/runtime/apollo/issues/230
                    onFocusChanged(list[1] as Boolean)
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
    scope: CoroutineScope
): FormTextFieldState = rememberSaveable(
    saver = FormTextFieldState.Saver(field, form, scope)
) {
    FormTextFieldState(
        properties = TextFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.valueFlow(scope),
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            singleLine = field.input is TextBoxFormInput,
            fieldType = form.fieldType(field),
            domain = form.domain(field) as? RangeDomain,
            minLength = minLength,
            maxLength = maxLength
        ),
        scope = scope,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        },
        defaultValidator = { field.getValidationErrors() }
    )
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
