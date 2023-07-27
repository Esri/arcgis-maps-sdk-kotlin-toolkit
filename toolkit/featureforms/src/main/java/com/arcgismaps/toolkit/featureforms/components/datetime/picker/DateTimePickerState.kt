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

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.time.Instant
import java.time.ZonedDateTime
import java.util.TimeZone

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

//    fun setValue(value: Instant) : Boolean

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

    override var value = mutableStateOf(initialValue?.let { Instant.ofEpochMilli(it) })

    // current time zone
    override val timeZone: TimeZone = TimeZone.getDefault()

    // time in current time zone
    //val zonedDateTime = value.value?.atZone(timeZone.toZoneId())
    // current time zone offset compared to UTC
    override val timeZoneOffset = initialValue?.let { timeZone.getOffset(it) } ?: 0

    // calculate which picker to show by default
    override var activePickerInput = mutableStateOf(
        if (pickerStyle == DateTimePickerStyle.DateTime
            || pickerStyle == DateTimePickerStyle.Date
        ) DateTimePickerInput.Date
        else DateTimePickerInput.Time
    )

//    override fun setValue(value: Instant) : Boolean {
//        return if (dateTimeValidator(value.toEpochMilli())) {
//            this.value.value = value
//            true
//        } else false
//    }

    override fun setValue(date: Long?, hour: Int, minute: Int) {
        val pickedTime = (hour * 60 * 60 * 1000) + (minute * 60 * 1000)
        val pickedMilli = (date ?: 0) + pickedTime
        value.value = Instant.ofEpochMilli(pickedMilli)
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
        } ?: true
    }

    override fun today(selectedHour: Int, selectedMinute: Int) {
        // only reset the date and persist the time information
        val zonedToday = Instant.now().atZone(timeZone.toZoneId())
            .withHour(selectedHour)
            .withMinute(selectedMinute)
        value.value = zonedToday.toInstant()
    }

    override fun now(selectedDate: Long?) {
        // only reset the time
        var zonedNow = Instant.now().atZone(timeZone.toZoneId())
        // persist the date information
        zonedNow = if (selectedDate != null) {
            val dateTime =
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(selectedDate), timeZone.toZoneId())
            zonedNow.withYear(dateTime.year)
                .withMonth(dateTime.monthValue)
                .withDayOfMonth(dateTime.dayOfMonth)
        } else {
            val dateTime = if (minDateTime != null) {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(minDateTime), timeZone.toZoneId())
            } else if (maxDateTime != null) {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(maxDateTime), timeZone.toZoneId())
            } else {
                zonedNow
            }
            zonedNow.withYear(dateTime.year)
                .withMonth(dateTime.monthValue)
                .withDayOfMonth(dateTime.dayOfMonth)
        }
        value.value = zonedNow.toInstant()
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
