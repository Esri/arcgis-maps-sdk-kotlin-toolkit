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
import com.arcgismaps.toolkit.featureforms.components.datetime.formattedUtcDateTime
import com.arcgismaps.toolkit.featureforms.components.datetime.toDateMillis
import com.arcgismaps.toolkit.featureforms.components.datetime.toUtcDateTime
import com.arcgismaps.toolkit.featureforms.components.datetime.toZonedDateTime
import java.time.Instant
import java.util.TimeZone

/**
 * A class to hold a DateTime. [date] represents the number of milliseconds of the date (at Midnight) since epoch
 * (January 1, 1970) in UTC. While [hour], [minute] and [second] represent time in local time zone.
 */
internal class UtcDateTime private constructor(
    val epochMillis: Long?,
    val date: Long?,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
) {
    // Force the picker to show a zoned date by providing the utc date plus the zone offset, converted to a UTC date
    // (the picker deals in utc dates only)
    val dateForPicker: Long?
        get() {
            println("date for picker without offset ${epochMillis?.formattedUtcDateTime(true)}")
            println("date for picker  with offset ${epochMillis?.plus(epochMillis.offset)?.toDateMillis()?.formattedUtcDateTime(true)}")
            return epochMillis?.plus(epochMillis.offset)?.toDateMillis()
        }
    
    val hourForPicker: Int
        get() = epochMillis?.toZonedDateTime()?.hour ?: hour
    
    val minuteForPicker: Int
        get() = epochMillis?.toZonedDateTime()?.minute ?: minute
    
    companion object {
        /**
         * Creates an instance of [UtcDateTime] using [dateTime] with the [hour], [minute] and [second]
         * representing time in the local zone. If the [dateTime] value is null then the returned
         * DateTime will have no date with time set to 0:00 hrs.
         *
         * @param dateTime The number of milliseconds since epoch (January 1, 1970) in UTC.
         * @return a new UtcDateTime
         */
        fun create(dateTime: Long?): UtcDateTime {
            val utcDateTime = dateTime?.toUtcDateTime()
            return UtcDateTime(
                dateTime,
                dateTime?.toDateMillis(),
                utcDateTime?.hour ?: 0,
                utcDateTime?.minute ?: 0,
                utcDateTime?.second ?: 0
            )
        }
        
        /**
         * Used to set the datetime from the result of the datetime picker dialog.
         *
         * @date the midnight UTC epoch millis of the date set in the picker.
         * @hour the hour selected in the picker 0-23
         * @hour the minute selected in the picker 0-59
         *
         * @return a new UtcDateTime
         */
        fun createFromDateAndTime(date: Long?, hour: Int, minute: Int): UtcDateTime {
            val epochMillis = if (date != null) {
                println("createFromDateAndTime without offset ${date.formattedUtcDateTime(true)} hour $hour minute $minute ")
                println("createFromDateAndTime with offset ${date.minus(date.offset).toDateMillis().formattedUtcDateTime(true) } hour $hour minute $minute ")
                val millis = (date + hour * 3_600_000 + minute * 60_000).minus(date.offset)
                println("final epoch millis to set the attribute: ${millis.formattedUtcDateTime(true)}")
                millis
            } else {
                null
            }
            return UtcDateTime(epochMillis, date, hour, minute)
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
    val dateTime: State<UtcDateTime>
    
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
    override val description: String = ""
) : DateTimePickerState {
    
    override var dateTime = mutableStateOf(
        UtcDateTime.create(initialValue)
    )
    
    override val selectedDateTimeMillis: Long?
        get() = dateTime.value.epochMillis
    
    override val timeZone: TimeZone = TimeZone.getDefault()
    
    override val timeZoneOffset = initialValue?.let { timeZone.getOffset(it) } ?: 0
    
    override var activePickerInput = mutableStateOf(
        if (pickerStyle == DateTimePickerStyle.DateTime
            || pickerStyle == DateTimePickerStyle.Date
        ) DateTimePickerInput.Date
        else DateTimePickerInput.Time
    )
    
    override fun setDateTime(date: Long?, hour: Int, minute: Int) {
        dateTime.value = UtcDateTime.createFromDateAndTime(date, hour, minute)
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
        dateTime.value = UtcDateTime.createFromDateAndTime(
            Instant.now().toEpochMilli().toDateMillis(),
            dateTime.value.hour,
            dateTime.value.minute
        )
    }
    
    override fun now() {
        val now = Instant.now().toEpochMilli().toZonedDateTime()
        dateTime.value = UtcDateTime.createFromDateAndTime(dateTime.value.date, now.hour, now.minute)
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

private val Long?.offset: Int
    get() = this?.let {
        TimeZone.getDefault().getOffset(it)
    } ?: 0