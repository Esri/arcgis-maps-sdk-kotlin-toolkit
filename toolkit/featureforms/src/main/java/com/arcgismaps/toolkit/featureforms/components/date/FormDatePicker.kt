package com.arcgismaps.toolkit.featureforms.components.date

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormDatePicker(value: Long?) {
    val datePattern = "MMM d, y HH:mm a"
    var currentValue = remember {
        mutableStateOf(
            if (value != null) {
                Instant.ofEpochMilli(value).format(datePattern)
            } else "Pick a date"
        )
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value)
    var openDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = currentValue.value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxSize(),
            interactionSource = interactionSource
        )
    }

    if (interactionSource.collectIsPressedAsState().value) {
        openDialog = true
    }

    if (openDialog) {
        FormDatePickerDialog(
            state = datePickerState,
            onDismissRequest = {
                openDialog = false
            },
            onDismissed = {
                openDialog = false
            }
        ) {
            it?.let { value ->
                currentValue.value = Instant.ofEpochMilli(value).format(datePattern)
            }
            openDialog = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDatePickerDialog(
    state: DatePickerState,
    onDismissRequest: () -> Unit = {},
    onDismissed: () -> Unit = {},
    onConfirmed: (Long?) -> Unit = {},
) {
    val confirmEnabled = remember {
        derivedStateOf { state.selectedDateMillis != null }
    }
    DatePickerDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmed(state.selectedDateMillis)
                },
                enabled = confirmEnabled.value
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { }
            ) {
                Text("Today")
            }
            TextButton(
                onClick = onDismissed
            ) {
                Text("Cancel")
            }
        }
    ) {

        DatePicker(state = state)
    }
}

private fun Instant.format(pattern : String) : String {
    val formatter = DateTimeFormatter
        .ofPattern(pattern).withZone(TimeZone.getDefault().toZoneId())
    return LocalDateTime.ofInstant(this, TimeZone.getDefault().toZoneId()).format(formatter)
}
