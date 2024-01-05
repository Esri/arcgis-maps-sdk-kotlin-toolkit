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

package com.arcgismaps.toolkit.featureforms.components.datetime

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.utils.DialogType
import com.arcgismaps.toolkit.featureforms.utils.LocalDialogRequester
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun DateTimeField(
    state: DateTimeFieldState,
    modifier: Modifier = Modifier
) {
    val dialogRequester = LocalDialogRequester.current
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    val instant by state.value.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    // to check if the field was ever focused by the user
    var wasFocused by rememberSaveable { mutableStateOf(false) }
    val label = if (isRequired) {
        "${state.label} *"
    } else {
        state.label
    }

    BaseTextField(
        text = instant?.formattedDateTime(state.shouldShowTime) ?: "",
        onValueChange = {
            // the only allowable change is to clear the text
            if (it.isEmpty()) {
                state.onValueChanged(null)
            }
        },
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = label,
        placeholder = state.placeholder.ifEmpty { stringResource(id = R.string.no_value) },
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = if (isEditable) Icons.Rounded.EditCalendar else Icons.Rounded.CalendarMonth,
        supportingText = {
            // if the field was focused and is required, validate the current value
            if (wasFocused && isRequired && instant == null) {
                Text(
                    text = stringResource(id = R.string.required),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = state.description,
                    modifier = Modifier.semantics { contentDescription = "description" },
                )
            }
        }
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                wasFocused = true
                // request to show the date picker dialog only when the touch is released
                // the dialog is responsible for updating the value on the state
                if (isEditable) {
                    dialogRequester.requestDialog(DialogType.DateTimeDialog(state))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DateTimeFieldPreview() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val state = DateTimeFieldState(
            properties = DateTimeFieldProperties(
                label = "Launch Date and Time",
                placeholder = "",
                description = "Enter the date for apollo 11 launch",
                value = MutableStateFlow(null),
                editable = MutableStateFlow(true),
                required = MutableStateFlow(false),
                visible = MutableStateFlow(true),
                minEpochMillis = null,
                maxEpochMillis = null,
                shouldShowTime = true
            ),
            scope = scope,
            onEditValue = {}
        )
        DateTimeField(state = state)
    }
}
