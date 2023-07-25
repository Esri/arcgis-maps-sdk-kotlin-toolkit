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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.components.datetime.toZonedDateTime
import java.sql.Time
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePicker(state: DateTimePickerState) {
    // time instant in UTC
    val instant = state.value?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
    // time in current time zone
    val zonedDateTime = instant.atZone(TimeZone.getDefault().toZoneId())
    // current time zone offset compared to UTC
    val offset = TimeZone.getDefault().getOffset(instant.toEpochMilli())
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.value?.plus(offset)
    )
    val timePickerState = rememberTimePickerState(
        initialHour = zonedDateTime.hour,
        initialMinute = zonedDateTime.minute
    )
    // calculate which picker to show by default
    val calculateActivePicker = {
        if (state.pickerStyle == DateTimePickerStyle.DateTime
            || state.pickerStyle == DateTimePickerStyle.Date
        ) DateTimePickerStyle.Date
        else DateTimePickerStyle.Time
    }
    var activePicker by remember { mutableStateOf(calculateActivePicker()) }
    val confirmEnabled by remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
    val visibility by state.visible
    if (visibility) {
        DateTimePickerDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button
                state.setVisibility(false)
            }
        ) {
            if (state.pickerStyle == DateTimePickerStyle.DateTime) {
                TextButton(
                    onClick = {
                        activePicker = if (activePicker == DateTimePickerStyle.Date)
                            DateTimePickerStyle.Time
                        else DateTimePickerStyle.Date
                    }
                ) {
                    Text("Switch")
                }
            }
            PickerContent(
                datePickerState = datePickerState,
                timePickerState = timePickerState,
                showTime = activePicker == DateTimePickerStyle.Time
            )
            Row(
                Modifier
                    .wrapContentHeight()
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {
                if (activePicker == DateTimePickerStyle.Date) {
                    TextButton(
                        onClick = {

                        }
                    ) {
                        Text("Today")
                    }
                } else {
                    TextButton(
                        onClick = {

                        }
                    ) {
                        Text("Now")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { state.setVisibility(false) }
                ) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        state.setVisibility(false)
                    },
                    enabled = confirmEnabled
                ) {
                    Text("OK")
                }
            }
        }
    } else {
        activePicker = calculateActivePicker()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun (ColumnScope).PickerContent(
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    showTime: Boolean
) {
    if (showTime) {
        TimePicker(state = timePickerState, modifier = Modifier.weight(1f, fill = false))
    } else {
        DatePicker(state = datePickerState, modifier = Modifier.weight(1f, fill = false))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePickerDialog(
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

internal object DateTimePickerDialogTokens {
    val containerHeight = 568.0.dp
    val containerWidth = 360.0.dp
}

internal enum class DateTimePickerStyle {
    Date,
    Time,
    DateTime
}
