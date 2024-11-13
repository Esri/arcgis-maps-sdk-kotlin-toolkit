/*
 * Copyright 2024 Esri
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

import com.arcgismaps.data.RangeDomain
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.formattedDateTime
import com.arcgismaps.toolkit.featureforms.internal.utils.asDoubleTuple
import com.arcgismaps.toolkit.featureforms.internal.utils.asLongTuple
import com.arcgismaps.toolkit.featureforms.internal.utils.format
import com.arcgismaps.toolkit.featureforms.internal.utils.isFloatingPoint
import com.arcgismaps.toolkit.featureforms.internal.utils.isIntegerType
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Transforms the state flow [FieldFormElement.value] into a state flow of type [T].
 * This function creates a new [StateFlow].
 *
 * @throws IllegalStateException if the [FieldFormElement.value] cannot be cast to [T].
 */
internal inline fun <reified T> FieldFormElement.mapValueAsStateFlow(scope: CoroutineScope): StateFlow<T> =
    if (value.value is T) {
        value.map { it as T }.stateIn(scope, SharingStarted.Eagerly, value.value as T)
    } else {
        // usage error.
        throw IllegalStateException("the generic parameterization of the state object must match the type specified.")
    }

/**
 * Creates and returns a new [StateFlow] of type [String] that emits the [FieldFormElement.formattedValue]
 * whenever the [FieldFormElement.value] emits.
 */
internal fun FieldFormElement.formattedValueAsStateFlow(scope: CoroutineScope): StateFlow<String> {
    return value.map {
        formattedValue
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        formattedValue
    )
}

/**
 * Creates and returns a new [StateFlow] that maps [FieldFormElement.validationErrors] from
 * List<[Throwable]> into List<[ValidationErrorState]>.
 *
 * @param scope A [CoroutineScope] to run the new [StateFlow] on.
 *
 * @return A new StateFlow<[ValidationErrorState]>
 */
internal fun FieldFormElement.mapValidationErrors(scope: CoroutineScope): StateFlow<List<ValidationErrorState>> {
    return validationErrors.map {
        createValidationErrorStates(it, this)
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        createValidationErrorStates(validationErrors.value, this)
    )
}

/**
 * Creates a list of [ValidationErrorState] with appropriate messages with the given [errors] and
 * [formElement].
 */
private fun createValidationErrorStates(
    errors: List<Throwable>,
    formElement: FieldFormElement
): List<ValidationErrorState> {
    var (hasMinRangeError, hasMaxRangeError) = Pair(false, false)
    var (hasMinCharError, hasMaxCharError) = Pair(false, false)
    return buildList {
        errors.forEach { error ->
            when (error) {
                is FeatureFormValidationException.RequiredException -> {
                    add(ValidationErrorState.Required)
                }

                is FeatureFormValidationException.OutOfDomainException -> {
                    add(ValidationErrorState.NotInCodedValueDomain)
                }

                is FeatureFormValidationException.NullNotAllowedException -> {
                    add(
                        ValidationErrorState.NullNotAllowed
                    )
                }

                is FeatureFormValidationException.IncorrectValueTypeException -> {
                    if (formElement.fieldType.isFloatingPoint) {
                        add(ValidationErrorState.NotANumber)
                    } else if (formElement.fieldType.isIntegerType) {
                        add(ValidationErrorState.NotAWholeNumber)
                    }
                }

                is FeatureFormValidationException.MinCharConstraintException -> {
                    hasMinCharError = true
                }

                is FeatureFormValidationException.MaxCharConstraintException -> {
                    hasMaxCharError = true
                }

                is FeatureFormValidationException.MinNumericConstraintException -> {
                    hasMinRangeError = true
                }

                is FeatureFormValidationException.MaxNumericConstraintException -> {
                    hasMaxRangeError = true
                }

                is FeatureFormValidationException.LessThanMinimumDateTimeException -> {
                    val dateTimePickerInput = formElement.input as DateTimePickerFormInput
                    val formatted = dateTimePickerInput.min?.formattedDateTime(dateTimePickerInput.includeTime)
                    if (formatted != null) {
                        add(ValidationErrorState.MinDatetimeConstraint(formatted))
                    }
                }

                is FeatureFormValidationException.MaxDateTimeConstraintException -> {
                    val dateTimePickerInput = formElement.input as DateTimePickerFormInput
                    val formatted = dateTimePickerInput.max?.formattedDateTime(dateTimePickerInput.includeTime)
                    if (formatted != null) {
                        add(ValidationErrorState.MaxDatetimeConstraint(formatted))
                    }
                }
            }
            if (formElement.input is TextBoxFormInput || formElement.input is TextAreaFormInput || formElement.input is BarcodeScannerFormInput) {
                if (!formElement.fieldType.isNumeric && (hasMinCharError || hasMaxCharError)) {
                    val (min, max) = when (val input = formElement.input) {
                        is TextBoxFormInput -> Pair(input.minLength.toInt(), input.maxLength.toInt())
                        is TextAreaFormInput -> Pair(input.minLength.toInt(), input.maxLength.toInt())
                        is BarcodeScannerFormInput -> Pair(input.minLength.toInt(), input.maxLength.toInt())
                        // logical edge case, should never happen
                        else -> Pair(0, 256)
                    }
                    if (min > 0 && max > 0) {
                        if (min == max) {
                            add(
                                ValidationErrorState.ExactCharConstraint(
                                    min,
                                    formElement.hasValueExpression
                                )
                            )
                        } else {
                            add(
                                ValidationErrorState.MinMaxCharConstraint(
                                    min,
                                    max,
                                    formElement.hasValueExpression
                                )
                            )
                        }
                    } else {
                        add(ValidationErrorState.MaxCharConstraint(max))
                    }

                } else if (hasMinRangeError || hasMaxRangeError) {
                    val rangeDomain = formElement.domain as RangeDomain
                    val (min: Number?, max: Number?) = if (formElement.fieldType.isIntegerType) {
                        val tuple = rangeDomain.asLongTuple
                        Pair(tuple.min, tuple.max)
                    } else if (formElement.fieldType.isFloatingPoint) {
                        val tuple = rangeDomain.asDoubleTuple
                        Pair(tuple.min, tuple.max)
                    } else {
                        Pair(null, null)
                    }
                    if (min != null && max != null) {
                        add(
                            ValidationErrorState.MinMaxNumericConstraint(
                                min.format(),
                                max.format(),
                                formElement.hasValueExpression
                            )
                        )
                    } else if (min != null) {
                        add(ValidationErrorState.MinNumericConstraint(min.format()))
                    } else {
                        add(ValidationErrorState.MaxNumericConstraint(max.format()))
                    }
                }
            }
        }
    }
}
