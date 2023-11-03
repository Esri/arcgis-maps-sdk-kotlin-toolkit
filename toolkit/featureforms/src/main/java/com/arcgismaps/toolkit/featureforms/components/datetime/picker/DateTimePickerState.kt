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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.toolkit.featureforms.components.datetime.toDateMillis
import com.arcgismaps.toolkit.featureforms.components.datetime.toDateTimeInUtcZone
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
    /**
     *  Force the picker to show a zoned date by providing the utc datetime as millis, plus the zone offset,
     *  and truncated to midnight of the resulting epoch millis.
     *  the picker deals, and shows, dates as millis only, This will add the timezone offset to epoch millis,
     *  and then pass it to the picker to show the current zoned time. It is subsequently subtracted from any
     *  choice made by the user.
     *
     *  @see createFromPickerValues
     *  @since 200.3.0
     */
    internal val dateForPicker: Long?
        get() = epochMillis?.plus(epochMillis.defaultTimeZoneOffset)?.toDateMillis()
    
    /**
     * The hour of the datetime in the current timezone.
     *
     * @since 200.3.0
     */
    internal val hourForPicker: Int
        get() = epochMillis?.toZonedDateTime()?.hour ?: hour
    
    /**
     * The minutes of the datetime in the current timezone.
     *
     * @since 200.3.0
     */
    internal val minuteForPicker: Int
        get() = epochMillis?.toZonedDateTime()?.minute ?: minute
    
    companion object {
        /**
         * Creates an instance of [UtcDateTime] using [epochMillis] with the [hour], [minute] and [second]
         * representing time in the UTC zone. If the [epochMillis] value is null then the returned
         * DateTime will have no date with time set to 0:00 hrs.
         *
         * @param epochMillis The number of milliseconds since epoch (January 1, 1970) in UTC.
         * @return a new UtcDateTime
         */
        internal fun create(epochMillis: Long?): UtcDateTime {
            val utcDateTime = epochMillis?.toDateTimeInUtcZone()
            return UtcDateTime(
                epochMillis,
                epochMillis?.toDateMillis(),
                utcDateTime?.hour ?: 0,
                utcDateTime?.minute ?: 0,
                utcDateTime?.second ?: 0
            )
        }
    
        /**
         * Used to set the datetime from the result of the datetime picker dialog.
         * Since the date picker works and displays with millis only, in order to show the date and time
         * in the current zone, we pass to it a long value which is not epoch millis, but epoch millis plus the
         * current timezone offset millis. This value must now be subtracted off so the result represents epoch milliseconds.
         *
         * @param date the midnight UTC epoch millis of the date set in the picker
         * @param hour the hour selected in the picker 0-23
         * @param hour the minute selected in the picker 0-59
         * @see dateForPicker
         * @return a new UtcDateTime
         * @since 200.3.0
         */
        internal fun createFromPickerValues(date: Long?, hour: Int, minute: Int): UtcDateTime {
            val epochMillis = if (date != null) {
                (date + hour * 3_600_000 + minute * 60_000).minus(date.defaultTimeZoneOffset)
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
     * Minimum date time allowed. This should be null if no range restriction is needed.
     */
    val minDateTime: Instant?
    
    /**
     * Maximum date time allowed. This should be null if no range restriction is needed.
     */
    val maxDateTime: Instant?
    
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
     *
     * @param date the epoch millis at the start of the date (i.e. midnight)
     * @param hour the hour of the day (0-23)
     * @param minute the minute of the hour (0-59)
     */
    fun setDateTime(date: Long?, hour: Int, minute: Int)
    
    /**
     * Toggles the current picker input between [DateTimePickerInput.Date] and
     * [DateTimePickerInput.Time].
     */
    fun togglePickerInput()
    
    /**
     * Validates if the [timeStamp] is between the given ranges of [minDateTime] and [maxDateTime]
     * if they were provided. Returns true if the validation was successful, otherwise false
     * is returned. Both the [minDateTime] and [maxDateTime] are included in the range.
     */
    fun dateTimeValidator(timeStamp: Long): Boolean
    
    /**
     * Validates if the UTC date of the [timeStamp] is between the dates of the given datetime ranges [minDateTime]
     * and [maxDateTime] if they were provided. Returns true if the validation was successful, otherwise false
     * is returned. Both the [minDateTime] and [maxDateTime] are included in the range.
     */
    fun dateValidator(timeStamp: Long): Boolean

    /**
     * Sets the [dateTime]'s time value to the current time instant in local time.
     */
    fun now()

    /**
     * Sets the [dateTime]'s day to the current day while persisting the time information
     * as specified by the [hour] and [minute].
     */
    fun today(hour: Int, minute: Int)
}

/**
 * Default implementation for [DateTimePickerState]
 */
private class DateTimePickerStateImpl(
    override val pickerStyle: DateTimePickerStyle,
    override val minDateTime: Instant?,
    override val maxDateTime: Instant?,
    initialValue: Instant?,
    override val label: String,
    override val description: String = "",
    pickerInput: DateTimePickerInput
) : DateTimePickerState {
    override var dateTime = mutableStateOf(
        UtcDateTime.create(initialValue?.toEpochMilli())
    )
    override val selectedDateTimeMillis: Long?
        get() = dateTime.value.epochMillis
    
    override val timeZone: TimeZone = TimeZone.getDefault()
    
    override val timeZoneOffset = initialValue?.toEpochMilli()?.let { timeZone.getOffset(it) } ?: 0
    
    override val activePickerInput = mutableStateOf(pickerInput)
    
    override fun setDateTime(date: Long?, hour: Int, minute: Int) {
        dateTime.value = UtcDateTime.createFromPickerValues(date, hour, minute)
    }
   
    override fun togglePickerInput() {
        activePickerInput.value = if (activePickerInput.value == DateTimePickerInput.Date) {
            DateTimePickerInput.Time
        } else {
            DateTimePickerInput.Date
        }
    }
    
    override fun dateTimeValidator(timeStamp: Long): Boolean {
        // the date time validator is invoked by the date picker,
        // which operates in milliseconds that are offset from UTC
        // To compare it to min and max, the input must be converted
        // to UTC.
        val utcDateTime = timeStamp.minus(timeStamp.defaultTimeZoneOffset)
        
        return minDateTime?.toEpochMilli()?.let { min ->
            maxDateTime?.toEpochMilli()?.let { max ->
                utcDateTime in min..max
            } ?: (utcDateTime >= min)
        } ?: maxDateTime?.toEpochMilli()?.let {
            utcDateTime <= it
        } ?: true
    }
    
    override fun dateValidator(timeStamp: Long): Boolean {
        // the date validator is invoked by the date picker,
        // which operates in milliseconds that are offset from UTC
        // To compare it to min and max, the input must be converted
        // to UTC.
        val utcDate = UtcDateTime.create(timeStamp.minus(timeStamp.defaultTimeZoneOffset)).date!!
        val minDate = UtcDateTime.create(minDateTime?.toEpochMilli()).date
        val maxDate = UtcDateTime.create(maxDateTime?.toEpochMilli()).date

        return minDate?.let { min ->
            maxDate?.let { max ->
                utcDate in min..max
            } ?: (utcDate >= min)
        } ?: maxDate?.let {
            utcDate <= it
        } ?: true
    }

    override fun now() {
        val now = Instant.now().toEpochMilli().toZonedDateTime()
        setDateTime(
            dateTime.value.dateForPicker,
            now.hour,
            now.minute
        )
    }

    override fun today(hour: Int, minute: Int) {
        val now = Instant.now().toEpochMilli()
        setDateTime(
            now.plus(now.defaultTimeZoneOffset).toDateMillis(),
            hour,
            minute
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
    minDateTime: Instant? = null,
    maxDateTime: Instant? = null,
    initialValue: Instant? = null,
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
 * a Composable function to create and remember a DateTimePickerState instance
 *
 * @param style the date time picker style
 * @param minDateTime the minimum pickable date time
 * @param maxDateTime the maxiimum pickable date time
 * @param initialValue the initial value in epoch milliseconds for the picker
 * @param label the label from the element
 * @param description the description from the element
 * @param pickerInput the input type to display in the picker.
 * @return a remembered DateTimePickerState
 * @since 200.3.0
 */
@Composable
internal fun rememberDateTimePickerState(
    style: DateTimePickerStyle,
    minDateTime: Instant? = null,
    maxDateTime: Instant? = null,
    initialValue: Instant? = null,
    label: String,
    description: String = "",
    pickerInput: DateTimePickerInput
): DateTimePickerState = rememberSaveable(saver = dateTimePickerStateSaver()) {
    DateTimePickerState(
        style,
        minDateTime,
        maxDateTime,
        initialValue,
        label,
        description,
        pickerInput
    )
}

/**
 * a StateSaver for the DateTimePickerState.
 *
 * @return a StateSaver
 * @since 200.3.0
 */
internal fun dateTimePickerStateSaver(): Saver<DateTimePickerState, Any> = listSaver(
    save = {
        listOf(it.pickerStyle,
            it.minDateTime?.toEpochMilli(),
            it.maxDateTime?.toEpochMilli(),
            it.dateTime.value.epochMillis,
            it.label,
            it.description,
            it.activePickerInput.value
        )
    },
    restore = {
        // note: passes the date time picker state exactly as saved to
        // set the initial view of the dialog based on how it was saved,
        // not on initial conditions.
        DateTimePickerStateImpl(
            it[0] as DateTimePickerStyle,
            it[1]?.let { Instant.ofEpochMilli(it as Long) },
            it[2]?.let { Instant.ofEpochMilli(it as Long) },
            it[3]?.let { Instant.ofEpochMilli(it as Long) },
            it[4] as String,
            it[5] as String,
            it[6] as DateTimePickerInput
        )
    }
)

internal val Long?.defaultTimeZoneOffset: Int
    get() = this?.let {
        TimeZone.getDefault().getOffset(it)
    } ?: 0
