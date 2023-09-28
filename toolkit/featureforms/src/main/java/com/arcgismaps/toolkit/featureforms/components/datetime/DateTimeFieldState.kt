/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureforms.components.datetime

import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * State for the [DateTimeField].
 *
 * @since 200.2.0
 */
internal interface DateTimeFieldState {
    /**
     * The minimum allowable date and time.
     *
     * @since 200.2.0
     */
    val minEpochMillis: Long?
    
    /**
     * The maximum allowable date and time.
     *
     * @since 200.2.0
     */
    val maxEpochMillis: Long?
    
    /**
     * `true` if the field should show time or allow time to be set on the field.
     *
     * @since 200.2.0
     */
    val shouldShowTime: Boolean
    
    /**
     * The label used on the Field.
     *
     * @since 200.2.0
     */
    val label: String
    
    /**
     * The placeholder text.
     *
     * @since 200.2.0
     */
    val placeholderText: String
    
    /**
     * The name of the field.
     *
     * @since 200.2.0
     */
    val description: String
    
    /**
     * The text representation of the date time.
     *
     * @since 200.2.0
     */
    val value: StateFlow<Long?>
    
    /**
     * `true` if the date time may be edited.
     *
     * @since 200.2.0
     */
    val isEditable: StateFlow<Boolean>
    
    /**
     * `true` if the field must have a datetime value
     *
     * @since 200.2.0
     */
    val isRequired: StateFlow<Boolean>
    
    /**
     * Updates the attribute.
     *
     * @param dateTime the date time expressed as epoch milliseconds
     * @since 200.2.0
     */
    fun setValue(dateTime: Long?)
    
    /**
     * Reset to the original value of the Feature attribute
     *
     * @since 200.2.0
     */
    fun resetValue()
    
    /**
     * Clear the value of the Feature
     *
     * @since 200.2.0
     */
    fun clearValue()
    
    /**
     * evaluate arcade expressions as needed due to user input
     *
     * @since 200.3.0
     */
    suspend fun evaluateExpressions()
}

private class DateTimeFieldStateImpl(
    private val element: FieldFormElement,
    private val form: FeatureForm,
    private val scope: CoroutineScope,
    input: DateTimePickerFormInput = element.input as DateTimePickerFormInput
) : DateTimeFieldState {
    override val minEpochMillis: Long? = input.min?.toEpochMilli()
    
    override val maxEpochMillis: Long? = input.max?.toEpochMilli()
    
    override val shouldShowTime: Boolean = input.includeTime
    
    override val label: String = element.label
    
    override val placeholderText: String = element.hint
    
    override val isEditable: StateFlow<Boolean> = element.isEditable
    
    override val isRequired: StateFlow<Boolean> = element.isRequired
    
    override val description: String = element.description
    
    private val _value: MutableStateFlow<Long?> = MutableStateFlow(null)
    override val value: StateFlow<Long?> = combine(_value, element.value, isEditable) { userEdit, exprResult, editable ->
        if (editable) {
            userEdit
        } else {
            dateTimeFromString(exprResult)
        }
    }.stateIn(scope, SharingStarted.Eagerly, dateTimeFromString(element.value.value))
    
    init {
        _value.value = dateTimeFromString(element.value.value)
    }
 
    private fun setValue(value: String) {
        if (value.isNotEmpty()) {
            val asLong = value.toLong()
            setValue(asLong)
        }
    }
    
    override fun setValue(dateTime: Long?) {
        form.editValue(element, dateTime)
        _value.value = dateTime
        scope.launch {
            evaluateExpressions()
        }
    }
    
    override fun resetValue() = setValue(element.value.value)
    
    override fun clearValue() {
        setValue(null)
    }
    
    override suspend fun evaluateExpressions() {
        form.evaluateExpressions()
    }
}

/**
 * Factory function to create a [DateTimeFieldState] using the [formElement].
 *
 * @param formElement the form element.
 * @param form the FeatureForm which provides access to the Feature (for now).
 */
internal fun DateTimeFieldState(
    formElement: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): DateTimeFieldState = DateTimeFieldStateImpl(formElement, form, scope)


/**
 * Maps the [FieldFormElement.value] from a String to Long?
 * Empty strings are made to be null Longs.
 *
 * @since 200.3.0
 */
internal fun dateTimeFromString(formattedDateTime: String): Long? {
    if (formattedDateTime.isNotEmpty()) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        return LocalDateTime.parse(formattedDateTime, formatter).atZone(TimeZone.getDefault().toZoneId())
            .toInstant()
            .toEpochMilli()
    }
    return null
}

