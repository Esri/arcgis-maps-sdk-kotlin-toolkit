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
import com.arcgismaps.toolkit.featureforms.components.datetime.toZonedDateTime
import java.time.Instant
import java.util.TimeZone

internal data class DateTime(
    val date: Long?,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
) {
    fun getDateTimeInMillis(): Long? {
        return date?.let {
            val timeInMillis = (hour * 60 * 60 * 1000) + (minute * 60 * 1000)
            return date + timeInMillis
        }
    }
}

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
    val dateTime: State<DateTime>

    val selectedDateTimeMillis: Long?

    /**
     * Current time zone, calculated automatically based on locale.
     */
    val timeZone: TimeZone

    /**
     * Current time zone offset compared to UTC in milliseconds, calculated from [timeZone]
     */
    val timeZoneOffset: Int

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

    fun setValue(date: Long?, hour: Int, minute: Int)

    fun togglePickerInput()

    fun dateTimeValidator(timeStamp: Long): Boolean

    fun today(selectedHour: Int, selectedMinute: Int)

    fun now(selectedDate: Long?)
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
    override val description: String = ""
) : DateTimePickerState {

    override var dateTime = mutableStateOf(
        DateTime(
            date = initialValue,
            hour = initialValue?.toZonedDateTime()?.hour ?: 0,
            minute = initialValue?.toZonedDateTime()?.minute ?: 0,
        )
    )

    override val selectedDateTimeMillis: Long?
        get() = dateTime.value.getDateTimeInMillis()

    // current time zone
    override val timeZone: TimeZone = TimeZone.getDefault()

    // current time zone offset compared to UTC
    override val timeZoneOffset = initialValue?.let { timeZone.getOffset(it) } ?: 0

    // calculate which picker to show by default
    override var activePickerInput = mutableStateOf(
        if (pickerStyle == DateTimePickerStyle.DateTime
            || pickerStyle == DateTimePickerStyle.Date
        ) DateTimePickerInput.Date
        else DateTimePickerInput.Time
    )

    override fun setValue(date: Long?, hour: Int, minute: Int) {
        dateTime.value = DateTime(date, hour, minute)
    }

    override fun togglePickerInput() {
        activePickerInput.value = if (activePickerInput.value == DateTimePickerInput.Date) {
            DateTimePickerInput.Time
        } else {
            DateTimePickerInput.Date
        }
    }

    override fun dateTimeValidator(timeStamp: Long): Boolean {
        return minDateTime?.let { min ->
            maxDateTime?.let { max ->
                timeStamp in min..max
            } ?: (timeStamp >= min)
        } ?: maxDateTime?.let {
            timeStamp <= it
        } ?: true
    }

    override fun today(selectedHour: Int, selectedMinute: Int) {
        // only reset the date in UTC and persist the time information
        dateTime.value = DateTime(
            date = Instant.now().toEpochMilli(),
            hour = dateTime.value.hour,
            minute = dateTime.value.minute
        )
    }

    override fun now(selectedDate: Long?) {
        // only reset the time in local time zone
        val zonedTime = Instant.now().atZone(timeZone.toZoneId())
        // persist the date information
        dateTime.value = DateTime(
            date = selectedDate,
            hour = zonedTime.hour,
            minute = zonedTime.minute
        )
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
 */
internal fun DateTimePickerState(
    style: DateTimePickerStyle,
    minDateTime: Long? = null,
    maxDateTime: Long? = null,
    initialValue: Long? = null,
    label: String,
    description: String = ""
): DateTimePickerState = DateTimePickerStateImpl(
    style,
    minDateTime,
    maxDateTime,
    initialValue,
    label,
    description
)
