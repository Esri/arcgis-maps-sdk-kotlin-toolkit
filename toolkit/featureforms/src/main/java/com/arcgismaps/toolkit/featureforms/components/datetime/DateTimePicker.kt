package com.arcgismaps.toolkit.featureforms.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    val currentPickerType = remember { mutableStateOf(PickerType.Date) }
    val confirmEnabled = remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
    if (dateTimePickerState.visible.value) {
        DateTimePickerDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                dateTimePickerState.setVisibility(false)
            }
        ) {
            TextButton(
                onClick = {
                    currentPickerType.value = if (currentPickerType.value == PickerType.Date)
                        PickerType.Time
                    else PickerType.Date
                }
            ) {
                Text("Switch")
            }
            if (currentPickerType.value == PickerType.Date) {
                DatePicker(state = datePickerState, modifier = Modifier.weight(1f, fill = false))
            } else {
                TimePicker(state = timePickerState, modifier = Modifier.weight(1f, fill = false))
            }
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        dateTimePickerState.setVisibility(false)
                    }
                ) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        dateTimePickerState.setVisibility(false)
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("OK")
                }
            }
        }
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

internal interface DateTimePickerState {

    val type: PickerType

    val visible: State<Boolean>

    fun setVisibility(value: Boolean)
}

internal enum class PickerType {
    Date,
    Time,
    DateTime
}

private class DateTimePickerStateImpl(
    override val type: PickerType
) : DateTimePickerState {

    override var visible = mutableStateOf(false)
        private set

    override fun setVisibility(value: Boolean) {
        visible.value = value
    }

}

internal fun DateTimePickerState(type: PickerType): DateTimePickerState =
    DateTimePickerStateImpl(type)
