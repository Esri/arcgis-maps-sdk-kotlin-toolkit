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

/**
 * A class to hold a DateTime. [date] represents the number of milliseconds since epoch
 * (January 1, 1970) in UTC. While [hour], [minute] and [second] represent time in local time zone.
 */
internal data class DateTime(
    val date: Long?,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
) {

    /**
     * Converts and returns the current DateTime in number of milliseconds since epoch
     * (January 1, 1970) in UTC.
     */
    fun getDateTimeInMillis(): Long? {
        return date?.let {
            val instant = Instant.ofEpochMilli(it)
            val zonedDateTime =
                instant.atZone(TimeZone.getDefault().toZoneId()).withHour(hour).withMinute(minute)
            return zonedDateTime.toInstant().toEpochMilli()
        }
    }

    companion object {
        /**
         * Creates an instance of [DateTime] using [dateTime] with the [hour], [minute] and [second]
         * representing time in the local zone.
         *
         * @param dateTime The number of milliseconds since epoch (January 1, 1970) in UTC.
         */
        fun createWithLocalTime(dateTime: Long?): DateTime {
            val zonedDateTime = dateTime?.toZonedDateTime()
            return DateTime(
                dateTime,
                zonedDateTime?.hour ?: 0,
                zonedDateTime?.minute ?: 0,
                zonedDateTime?.second ?: 0
            )
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
     * The current date time value. Use [setDateTime] to set this state.
     */
    val dateTime: State<DateTime>

    /**
     * A timestamp that represents the the selected date and time in UTC milliseconds from the epoch.
     * In case no date was selected or provided, this will hold a null value.
     */
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

    /**
     * Current picker input type.
     */
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
     * Sets the [dateTime].
     */
    fun setDateTime(date: Long?, hour: Int, minute: Int)

    /**
     * Toggles the current picker input between [DateTimePickerInput.Date] and
     * [DateTimePickerInput.Time].
     */
    fun togglePickerInput()

    /**
     * Validates if the [timeStamp] is between the given ranges of [minDateTime] and [maxDateTime]
     * if they were provided and returns true if the validation was successful, otherwise false
     * is returned.
     */
    fun dateTimeValidator(timeStamp: Long): Boolean

    /**
     * Sets the current [dateTime]'s date value to today's date while preserving the time, if any
     * time was previously set.
     */
    fun today()

    /**
     * Sets the current [dateTime]'s hour, minute and second values to the current local time while
     * preserving the date, if any date was previously set.
     */
    fun now()
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
        DateTime.createWithLocalTime(initialValue)
    )

    override val selectedDateTimeMillis: Long?
        get() = dateTime.value.getDateTimeInMillis()

    override val timeZone: TimeZone = TimeZone.getDefault()

    override val timeZoneOffset = initialValue?.let { timeZone.getOffset(it) } ?: 0

    override var activePickerInput = mutableStateOf(
        if (pickerStyle == DateTimePickerStyle.DateTime
            || pickerStyle == DateTimePickerStyle.Date
        ) DateTimePickerInput.Date
        else DateTimePickerInput.Time
    )

    override fun setDateTime(date: Long?, hour: Int, minute: Int) {
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

    override fun today() {
        // only reset the date in UTC and persist the time information
        dateTime.value = DateTime(
            date = Instant.now().toEpochMilli(),
            hour = dateTime.value.hour,
            minute = dateTime.value.minute
        )
    }

    override fun now() {
        // only reset the time in local time zone
        val zonedTime = Instant.now().atZone(timeZone.toZoneId())
        // persist the date information
        dateTime.value = DateTime(
            date = dateTime.value.date,
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
