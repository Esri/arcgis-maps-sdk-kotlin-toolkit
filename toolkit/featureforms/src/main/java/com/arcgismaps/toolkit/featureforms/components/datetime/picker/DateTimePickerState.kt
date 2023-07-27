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

package com.arcgismaps.toolkit.featureforms.components.datetime.picker

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.time.Instant

/**
 * State for [DateTimePicker]. Use factory [DateTimePicker()] to create an instance.
 */
internal interface DateTimePickerState {

    /**
     * Minimum date time allowed in milliseconds. This should be null if no range restriction is needed.
     */
    val minDateTime: Long?

    /**
     * Maximum date time allowed in milliseconds. This should be null if no range restriction is needed.
     */
    val maxDateTime: Long?

    /**
     * The set date time value in milliseconds.
     */
    val value: State<Instant?>

    /**
     * The picker style to use.
     */
    val pickerStyle: DateTimePickerStyle

    val activePickerInput: State<DateTimePickerInput>

    /**
     * The label for the DateTimePicker.
     */
    val label: String

    /**
     * The description for the DateTimePicker.
     */
    val description: String

    /**
     * Callback for when a value is selected on the DateTimePicker.
     */
    val onValueSet: (Long) -> Unit

    fun setValue(value: Instant)

    fun togglePickerInput()
}

/**
 * Default implementation for [DateTimePickerState]
 */
private class DateTimePickerStateImpl(
    override val pickerStyle: DateTimePickerStyle,
    override val minDateTime: Long?,
    override val maxDateTime: Long?,
    initialValue: Long?,
    override val label: String,
    override val description: String = "",
    override val onValueSet: (Long) -> Unit = {}
) : DateTimePickerState {

    override var value = mutableStateOf(initialValue?.let { Instant.ofEpochMilli(it) })

    // calculate which picker to show by default
    override var activePickerInput = mutableStateOf(
        if (pickerStyle == DateTimePickerStyle.DateTime
            || pickerStyle == DateTimePickerStyle.Date
        ) DateTimePickerInput.Date
        else DateTimePickerInput.Time
    )

    override fun setValue(value: Instant) {
        this.value.value = value
    }

    override fun togglePickerInput() {
        activePickerInput.value = if (activePickerInput.value == DateTimePickerInput.Date) {
            DateTimePickerInput.Time
        } else {
            DateTimePickerInput.Date
        }
    }
}

/**
 * Factory function to create a [DateTimePickerState].
 *
 * @param style The picker style to use.
 * @param minDateTime Minimum date time allowed in milliseconds. This should be null if no range
 * restriction is needed.
 * @param maxDateTime Maximum date time allowed in milliseconds. This should be null if no range
 * restriction is needed.
 * @param initialValue The initial date time value to display in milliseconds.
 * @param label The label for the DateTimePicker.
 * @param description The description for the DateTimePicker.
 * @param onValueSet Callback for when a value is selected on the DateTimePicker.
 */
internal fun DateTimePickerState(
    style: DateTimePickerStyle,
    minDateTime: Long? = null,
    maxDateTime: Long? = null,
    initialValue: Long? = null,
    label: String,
    description: String = "",
    onValueSet: (Long) -> Unit = {}
): DateTimePickerState = DateTimePickerStateImpl(
    style,
    minDateTime,
    maxDateTime,
    initialValue,
    label,
    description,
    onValueSet
)
