package com.arcgismaps.toolkit.featureforms.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePicker(dateTimePickerState: DateTimePickerState) {
    val openDialog = remember { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState()
    val confirmEnabled = remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
    DateTimePickerDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            openDialog.value = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    openDialog.value = false
                },
                enabled = confirmEnabled.value
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    openDialog.value = false
                }
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
        //TimePicker(state = TimePickerState(2, 2, false))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(),
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
                .heightIn(max = DateTimePickerDialogTokens.containerHeight),
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

internal interface DateTimePickerState {
    enum class PickerType {
        Date,
        Time,
        DateTime
    }

    val type: PickerType
}

private class DateTimePickerStateImpl(
    override val type: DateTimePickerState.PickerType
) : DateTimePickerState {

}

internal fun DateTimePickerState(type: DateTimePickerState.PickerType): DateTimePickerState =
    DateTimePickerStateImpl(type)
