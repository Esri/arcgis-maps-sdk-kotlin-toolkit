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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.datetime.toZonedDateTime
import java.time.Instant

/**
 * Defines the style of [DateTimePicker]
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

internal enum class DateTimePickerInput {
    Date,
    Time
}

/**
 * A material3 date and time picker presented as an [AlertDialog]
 *
 * @param state a state of the DateTimePicker. see [DateTimePickerState]
 * @param onDismissRequest Dismiss the dialog when the user clicks outside the dialog or on the back button
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePicker(
    state: DateTimePickerState,
    onDismissRequest: () -> Unit,
    onCancelled: () -> Unit,
    onConfirmed: () -> Unit
) {
    // calculate the date ranges from the state
    val datePickerRange = IntRange(
        start = state.minDateTime?.toZonedDateTime()?.year
            ?: DatePickerDefaults.YearRange.first,
        endInclusive = state.maxDateTime?.toZonedDateTime()?.year
            ?: DatePickerDefaults.YearRange.last
    )
    // time instant in UTC from the state's value
    val dateTime by state.dateTime
    // create and remember a DatePickerState that resets when instant changes
    val datePickerState = rememberSaveable(dateTime, saver = DatePickerState.Saver()) {
        DatePickerState(
            initialSelectedDateMillis = dateTime.date?.plus(state.timeZoneOffset),
            initialDisplayedMonthMillis = dateTime.date?.plus(state.timeZoneOffset)
                ?: (state.minDateTime ?: state.maxDateTime),
            datePickerRange,
            DisplayMode.Picker
        )
    }
    // create and remember a TimePickerState that resets when instant changes
    val timePickerState = rememberSaveable(dateTime, saver = TimePickerState.Saver()) {
        TimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = false,
        )
    }
    val confirmEnabled by remember(dateTime) {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }
    DateTimePickerDialog(
        onDismissRequest = onDismissRequest
    ) {
        PickerContent(
            label = state.label,
            description = state.description,
            state = state,
            datePickerState = datePickerState,
            timePickerState = timePickerState,
            style = state.pickerStyle,
            picker = state.activePickerInput.value,
        ) {
            state.togglePickerInput()
        }
        PickerFooter(
            state = state,
            confirmEnabled = confirmEnabled,
            onToday = {
                state.today()
            },
            onNow = {
                state.now()
            },
            onCancelled = onCancelled,
            onConfirmed = {
                // remove time zone offset before setting value
                state.setDateTime(
                    date = datePickerState.selectedDateMillis?.minus(state.timeZoneOffset),
                    hour = timePickerState.hour,
                    minute = timePickerState.minute
                )
                onConfirmed()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    if (picker == DateTimePickerInput.Time) {
        title(if (style == DateTimePickerStyle.Time) null else Icons.Rounded.CalendarMonth)
        Spacer(modifier = Modifier.height(10.dp))
        TimePicker(state = timePickerState, modifier = Modifier.weight(1f, fill = false))
    } else {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.weight(1f, fill = false),
            dateValidator = { timeStamp ->
                // validate selectable dates if a range is provided
                state.dateTimeValidator(timeStamp)
            },
            title = { title(if (style == DateTimePickerStyle.Date) null else Icons.Rounded.AccessTime) }
        )
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
            .padding(start = 25.dp, top = 25.dp, end = 15.dp, bottom = 15.dp)
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
                    contentDescription = "Set Time"
                )
            }
        }
    }
}

@Composable
private fun PickerFooter(
    state: DateTimePickerState,
    confirmEnabled: Boolean,
    onToday: () -> Unit = {},
    onNow: () -> Unit = {},
    onCancelled: () -> Unit = {},
    onConfirmed: () -> Unit = {}
) {
    Row(
        Modifier
            .wrapContentHeight()
            .padding(10.dp)
            .fillMaxWidth()
    ) {
        if (state.activePickerInput.value == DateTimePickerInput.Date) {
            TextButton(
                onClick = onToday,
                enabled = state.dateTimeValidator(Instant.now().toEpochMilli())
            ) {
                Text(stringResource(R.string.today))
            }
        } else {
            TextButton(onClick = onNow) {
                Text(stringResource(R.string.now))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancelled) {
            Text(stringResource(R.string.cancel))
        }
        TextButton(onClick = onConfirmed, enabled = confirmEnabled) {
            Text(stringResource(R.string.ok))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.wrapContentHeight(),
        properties = properties
    ) {
        Surface(
            modifier = Modifier
                .requiredWidth(DateTimePickerDialogTokens.containerWidth)
                .heightIn(DateTimePickerDialogTokens.containerHeight),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = tonalElevation,
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

private object DateTimePickerDialogTokens {
    val containerHeight = 568.0.dp
    val containerWidth = 360.0.dp
}

@Preview
@Composable
private fun DateTimePickerPreview() {
    val state = DateTimePickerState(
        style = DateTimePickerStyle.DateTime,
        label = "Next Inspection Date",
        description = "Enter a date in the next six months"
    )
    DateTimePicker(state = state, {}, {}, {})
}
