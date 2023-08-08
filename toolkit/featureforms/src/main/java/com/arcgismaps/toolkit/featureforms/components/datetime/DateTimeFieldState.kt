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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.components.text.editValue
import com.arcgismaps.toolkit.featureforms.components.text.getElementValue
import java.time.Instant

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
    val value: State<Long?>
    
    /**
     * `true` if the date time may be edited.
     *
     * @since 200.2.0
     */
    val isEditable: State<Boolean>
    
    /**
     * `true` if the field must have a datetime value
     *
     * @since 200.2.0
     */
    val isRequired: State<Boolean>
    
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
}

private class DateTimeFieldStateImpl(
    private val element: FieldFormElement,
    private val form: FeatureForm,
    input: DateTimePickerFormInput = element.input as DateTimePickerFormInput
) : DateTimeFieldState {
    override val minEpochMillis: Long? = input.min?.toEpochMilli()
    
    override val maxEpochMillis: Long? = input.max?.toEpochMilli()
    
    override val shouldShowTime: Boolean = input.includeTime
    
    override val label: String = element.label
    
    override val placeholderText: String = element.hint
    
    private val _isEditable: MutableState<Boolean> = mutableStateOf(element.editableExpressionName.isNotEmpty())
    override val isEditable: State<Boolean> = _isEditable
    
    private val _isRequired: MutableState<Boolean> = mutableStateOf(element.requiredExpressionName.isNotBlank())
    override val isRequired: State<Boolean> = _isRequired
    
    override val description: String = element.description
    
    private val _value: MutableState<Long?> = mutableStateOf(null)
    override val value: State<Long?> = _value
    
    init {
        val initialValue = form.getElementValue(element)?.let {
            when (it) {
                is Instant -> {
                    it.toEpochMilli()
                }
        
                is Long -> {
                    it
                }
        
                else -> {
                    null
                }
            }
        }
        
        setValue(initialValue)
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
    }
    
    override fun resetValue() = setValue(element.value)
    
    override fun clearValue() {
        setValue(null)
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
    form: FeatureForm
): DateTimeFieldState = DateTimeFieldStateImpl(formElement, form)
