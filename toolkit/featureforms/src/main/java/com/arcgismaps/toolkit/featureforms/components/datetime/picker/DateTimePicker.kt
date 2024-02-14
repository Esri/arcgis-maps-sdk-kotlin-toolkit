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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.date.DatePicker
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.date.DatePickerDefaults
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.date.DatePickerState
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.date.DisplayMode
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.time.TimePicker
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.time.TimePickerState
import java.time.Instant
import java.util.TimeZone

/**
 * Defines the style of [DateTimePicker].
 */
internal enum class DateTimePickerStyle {
    /**
     * Date only picker style.
     */
    Date,

    /**
     * Time only picker style.
     */
    Time,

    /**
     * Date and Time picker style.
     */
    DateTime
}

/**
 * Input type for the [DateTimePicker].
 */
internal enum class DateTimePickerInput {
    Date,
    Time
}

/**
 * A material3 date and time picker presented as an [AlertDialog].
 *
 * @param state a state of the DateTimePicker. see [DateTimePickerState].
 * @param onDismissRequest Dismiss the dialog when the user clicks outside the dialog or on the back button.
 *
 */
@Composable
internal fun DateTimePicker(
    state: DateTimePickerState,
    onDismissRequest: () -> Unit,
    onCancelled: () -> Unit,
    onConfirmed: () -> Unit
) {
    // if the date time has no value, set a default value
    if (state.dateTime.value.epochMillis == null) {
        val now = Instant.now().toEpochMilli()
        // check if current day and time is a valid timestamp
        // it should be validated with the current local timestamp hence the added offset
        if (state.dateTimeValidator(now.plus(now.defaultTimeZoneOffset))) {
            // set the default timestamp value to the current local time instant
            state.today(0, 0)
            state.now()
        }
    }
    // calculate the date ranges from the state
    val datePickerRange = IntRange(
        start = state.minDateTime?.atZone(TimeZone.getDefault().toZoneId())?.year
            ?: DatePickerDefaults.YearRange.first,
        endInclusive = state.maxDateTime?.atZone(TimeZone.getDefault().toZoneId())?.year
            ?: DatePickerDefaults.YearRange.last
    )
    // The picker input type, date or time.
    val pickerInput by state.activePickerInput
    // DateTime from the state's value
    val dateTime by state.dateTime
    // create and remember a DatePickerState
    val datePickerState = rememberSaveable(dateTime, saver = DatePickerState.Saver()) {
        DatePickerState(
            initialSelectedDateMillis = dateTime.dateForPicker,
            initialDisplayedMonthMillis = dateTime.dateForPicker
                ?: (state.minDateTime?.toEpochMilli() ?: state.maxDateTime?.toEpochMilli()),
            datePickerRange,
            DisplayMode.Picker
        )
    }
    // create a DateTimePickerDialog
    DateTimePickerDialog(
        onDismissRequest = onDismissRequest
    ) {
        // create and remember a TimePickerState that resets when dateTime changes
        val timePickerState = rememberSaveable(dateTime, saver = TimePickerState.Saver()) {
            TimePickerState(
                initialHour = dateTime.hourForPicker,
                initialMinute = dateTime.minuteForPicker,
                is24Hour = false,
            )
        }
        PickerContent(
            label = state.label,
            description = state.description,
            state = state,
            datePickerState = datePickerState,
            timePickerState = timePickerState,
            style = state.pickerStyle,
            picker = pickerInput
        ) {
            state.togglePickerInput()
        }
        PickerFooter(
            state = state,
            confirmEnabled = datePickerState.selectedDateMillis?.let {
                state.dateTimeValidator(
                    it + timePickerState.hour * 3_600_000 + timePickerState.minute * 60_000
                )
            } ?: false,
            pickerInput = pickerInput,
            onToday = {
                state.today(timePickerState.hour, timePickerState.minute)
            },
            onNow = {
                state.now()
            },
            onCancelled = onCancelled,
            onConfirmed = {
                state.setDateTime(
                    date = datePickerState.selectedDateMillis,
                    hour = timePickerState.hour,
                    minute = timePickerState.minute
                )
                onConfirmed()
            }
        )
    }
}

@Composable
private fun (ColumnScope).PickerContent(
    label: String,
    description: String,
    state: DateTimePickerState,
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    style: DateTimePickerStyle,
    picker: DateTimePickerInput,
    onPickerToggle: () -> Unit
) {
    val title: @Composable (ImageVector?) -> Unit = {
        PickerTitle(
            label = label,
            description = description,
            icon = it,
            onIconTap = onPickerToggle
        )
    }
    // make the picker content scrollable if the screen height sizing is more restrictive
    // like in landscape mode
    LazyColumn(
        modifier = Modifier.weight(1f, false),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if (picker == DateTimePickerInput.Time) {
                title(if (style == DateTimePickerStyle.Time) null else Icons.Rounded.CalendarMonth)
                Spacer(modifier = Modifier.height(10.dp))
                TimePicker(state = timePickerState, modifier = Modifier.padding(10.dp))
            } else {
                key(state.dateTime.value) {
                    DatePicker(
                        state = datePickerState,
                        dateValidator = { timeStamp ->
                            state.dateValidator(timeStamp)
                        },
                        title = { title(if (style == DateTimePickerStyle.Date) null else Icons.Rounded.AccessTime) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerTitle(
    label: String,
    description: String,
    icon: ImageVector?,
    onIconTap: () -> Unit = {}
) {
    Row(
        Modifier
            .padding(start = 25.dp, end = 15.dp, bottom = 10.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(5.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        icon?.let {
            IconButton(onClick = onIconTap) {
                Icon(
                    imageVector = it,
                    contentDescription = "toggle date and time"
                )
            }
        }
    }
}

@Composable
private fun PickerFooter(
    state: DateTimePickerState,
    confirmEnabled: Boolean,
    pickerInput: DateTimePickerInput,
    onToday: () -> Unit = {},
    onNow: () -> Unit = {},
    onCancelled: () -> Unit = {},
    onConfirmed: () -> Unit = {}
) {
    Row(
        Modifier
            .wrapContentHeight()
            .padding(start = 10.dp, end = 10.dp)
            .fillMaxWidth()
    ) {
        if (pickerInput == DateTimePickerInput.Date) {
            TextButton(
                onClick = onToday,
                // only enable Today button if today is within the range if provided
                // the date validator assumes the Long is from the picker,
                // i.e. offset from UTC.
                enabled = state.dateValidator(
                    UtcDateTime.create(Instant.now().toEpochMilli()).dateForPicker!!
                ),
                modifier = Modifier.semantics { contentDescription = "current date or time button" }
            ) {
                Text(stringResource(R.string.today))
            }
        } else {
            TextButton(onClick = onNow) {
                Text(stringResource(R.string.now))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancelled, modifier = Modifier.semantics { contentDescription = "cancel" }) {
            Text(stringResource(R.string.cancel))
        }
        TextButton(onClick = onConfirmed, enabled = confirmEnabled) {
            Text(stringResource(R.string.ok))
        }
    }
}

@Composable
private fun DateTimePickerDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        //modifier = modifier.wrapContentHeight(),
        properties = properties
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = DateTimePickerDialogTokens.horizontalPadding)
                .widthWithOrientation(DateTimePickerDialogTokens.containerWidth)
                .height(DateTimePickerDialogTokens.containerHeight)
                .scaleIfNarrow(DateTimePickerDialogTokens.containerWidth + DateTimePickerDialogTokens.horizontalPadding * 2)
                .semantics { contentDescription = "DateTimePickerDialogSurface" },
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = modifier.padding(top = 25.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

/**
 * Properties for the [DateTimePickerDialog].
 */
private object DateTimePickerDialogTokens {
    val containerHeight = 600.0.dp
    val containerWidth = 360.0.dp
    val horizontalPadding = 25.dp
}

/**
 * Constraints the width of the content based on the orientation and the [width]. If the
 * current orientation is portrait, [Modifier.requiredWidth] used. If it is landscape then
 * [Modifier.widthIn] is used. This is useful when different layouts are needed in portrait
 * and landscape orientations.
 */
internal fun Modifier.widthWithOrientation(width: Dp) : Modifier = composed {
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        this.widthIn(width)
    } else {
        this.requiredWidth(width)
    }
}

/**
 * Scales the content appropriately if the current screen width is less than the [minWidth].
 */
internal fun Modifier.scaleIfNarrow(minWidth: Dp): Modifier = composed {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val scale = if (screenWidth < minWidth)
        screenWidth / minWidth
    else 1f
    this.scale(scale)
}

@Preview
@Composable
private fun DateTimePickerPreview() {
    val state = DateTimePickerState(
        style = DateTimePickerStyle.DateTime,
        label = "Next Inspection Date",
        description = "Enter a date in the next six months",
        pickerInput = DateTimePickerInput.Date
    )
    DateTimePicker(state = state, {}, {}, {})
}
