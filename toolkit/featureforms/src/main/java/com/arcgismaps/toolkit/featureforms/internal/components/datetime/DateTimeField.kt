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

package com.arcgismaps.toolkit.featureforms.internal.components.datetime

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun DateTimeField(
    state: DateTimeFieldState,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(newValue = state)
    val dialogRequester = LocalDialogRequester.current
    val isEditable by currentState.isEditable.collectAsState()
    val isRequired by currentState.isRequired.collectAsState()
    val value by currentState.value
    val interactionSource = remember { MutableInteractionSource() }
    val isError = value.error !is ValidationErrorState.NoError
    // if any errors are present, show the error as the supporting text
    val supportingText = if (!isError) {
        currentState.description
    } else {
        value.error.getString()
    }

    BaseTextField(
        text = value.data?.formattedDateTime(currentState.shouldShowTime) ?: "",
        onValueChange = {
            // the only allowable change is to clear the text
            if (it.isEmpty()) {
                currentState.onValueChanged(null)
            }
        },
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = currentState.label,
        placeholder = currentState.placeholder.ifEmpty { stringResource(id = R.string.no_value) },
        supportingText = supportingText,
        isError = isError,
        isRequired = isRequired,
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = Icons.Rounded.EditCalendar,
        onFocusChange = currentState::onFocusChanged,
        trailingContent =
        if (isRequired) {
            {
                Icon(
                    imageVector = Icons.Rounded.EditCalendar,
                    contentDescription = "date time picker button"
                )
            }
        } else {
            null
        },
        hasValueExpression = currentState.hasValueExpression
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                // request to show the date picker dialog only when the touch is released
                // the dialog is responsible for updating the value on the state
                if (isEditable) {
                    dialogRequester.requestDialog(DialogType.DateTimeDialog(currentState.id))
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
                validationErrors = MutableStateFlow(emptyList()),
                editable = MutableStateFlow(true),
                required = MutableStateFlow(false),
                visible = MutableStateFlow(true),
                minEpochMillis = null,
                maxEpochMillis = null,
                shouldShowTime = true,
                fieldType = FieldType.Date
            ),
            hasValueExpression = false,
            scope = scope,
            updateValue = {},
            id = 1,
            evaluateExpressions = {
                return@DateTimeFieldState Result.success(emptyList())
            }
        )
        DateTimeField(state = state)
    }
}
