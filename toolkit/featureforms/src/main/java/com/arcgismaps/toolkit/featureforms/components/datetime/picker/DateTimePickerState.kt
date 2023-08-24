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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.arcgismaps.toolkit.featureforms.components.datetime.toZonedDateTime
import java.time.Instant
import java.util.TimeZone

/**
 * A class to hold a DateTime. [dateTime] represents the number of milliseconds since epoch
 * (January 1, 1970) in UTC. While [hour], [minute] and [second] represent time in local time zone.
 */
internal class DateTime(
    val dateTime: Long?,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
) {

    /**
     * Converts and returns the current DateTime in number of milliseconds since epoch
     * (January 1, 1970) in UTC.
     */
    fun getDateTimeInMillis(): Long? {
        return dateTime?.let {
            val instant = Instant.ofEpochMilli(it)
            val zonedDateTime =
                instant.atZone(TimeZone.getDefault().toZoneId()).withHour(hour).withMinute(minute)
            return zonedDateTime.toInstant().toEpochMilli()
        }
    }

    companion object {
        /**
         * Creates an instance of [DateTime] using [dateTime] with the [hour], [minute] and [second]
         * representing time in the local zone. If the [dateTime] value is null then the returned
         * DateTime will have no date with time set to 0:00 hrs.
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
     * A timestamp that represents the selected date and time in UTC milliseconds from the epoch.
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
     * is returned. Both the [minDateTime] and [maxDateTime] are included in the range.
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
    override val description: String = "",
    pickerInput: DateTimePickerInput
) : DateTimePickerState {
    override var dateTime = mutableStateOf(
        DateTime.createWithLocalTime(initialValue)
    )

    override val selectedDateTimeMillis: Long?
        get() = dateTime.value.getDateTimeInMillis()

    override val timeZone: TimeZone = TimeZone.getDefault()

    override val timeZoneOffset = initialValue?.let { timeZone.getOffset(it) } ?: 0
    
    override val activePickerInput = mutableStateOf(pickerInput)
    
    override fun setDateTime(date: Long?, hour: Int, minute: Int) {
        dateTime.value = DateTime(date, hour, minute)
    }

    override fun togglePickerInput() {
        activePickerInput.value = if (activePickerInput.value == DateTimePickerInput.Date) {
            println("toggling to time")
            DateTimePickerInput.Time
        } else {
            println("toggling to date")
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
            dateTime = Instant.now().toEpochMilli(),
            hour = dateTime.value.hour,
            minute = dateTime.value.minute
        )
    }

    override fun now() {
        // only reset the time in local time zone
        val zonedTime = Instant.now().atZone(timeZone.toZoneId())
        // persist the date information
        dateTime.value = DateTime(
            dateTime = dateTime.value.dateTime,
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
    description: String = "",
    pickerInput: DateTimePickerInput
): DateTimePickerState = DateTimePickerStateImpl(
    style,
    minDateTime,
    maxDateTime,
    initialValue,
    label,
    description,
    pickerInput
)

/**
 * a StateSaver for the DateTimePickerState.
 *
 * @param initialValue the value needed to initialize a DateTimePickerState
 * @return a StateSaver
 * @since 200.3.0
 */
internal fun dateTimePickerStateSaver(initialValue: Long?): Saver<DateTimePickerState, Any> = listSaver(
    save = {
        listOf(it.pickerStyle, it.minDateTime, it.maxDateTime, initialValue, it.label, it.description, it.activePickerInput.value)
    },
    restore = {
        // note: passes the date time picker state exactly as saved to
        // set the initial view of the dialog based on how it was saved,
        // not on initial conditions.
        DateTimePickerStateImpl(it[0] as DateTimePickerStyle, it[1] as Long?, it[2] as Long?, it[3] as Long?, it[4] as String, it[5] as String, it[6] as DateTimePickerInput)
    }
)
