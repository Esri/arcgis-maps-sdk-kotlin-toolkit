package com.arcgismaps.toolkit.featureforms.components.datetime

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
import java.time.format.DateTimeFormatter
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormDateTimeField(value: Long?) {
    val datePattern = "MMM d, y HH:mm a"
    val currentValue = remember {
        mutableStateOf(
            if (value != null) {
                Instant.ofEpochMilli(value).format(datePattern)
            } else "Pick a date"
        )
    }
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
        DateTimePicker(dateTimePickerState = DateTimePickerState(DateTimePickerState.PickerType.DateTime))
    }
}

private fun Instant.format(pattern: String): String {
    val formatter = DateTimeFormatter
        .ofPattern(pattern).withZone(TimeZone.getDefault().toZoneId())
    return LocalDateTime.ofInstant(this, TimeZone.getDefault().toZoneId()).format(formatter)
}
