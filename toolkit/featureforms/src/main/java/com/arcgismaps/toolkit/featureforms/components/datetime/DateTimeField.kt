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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePicker
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePickerInput
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePickerStyle
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.rememberDateTimePickerState
import com.arcgismaps.toolkit.featureforms.utils.PlaceholderTransformation

@Composable
internal fun DateTimeField(
    state: DateTimeFieldState,
    modifier: Modifier = Modifier
) {
    val isEditable by state.isEditable
    val isRequired by state.isRequired
    val epochMillis by state.value
    val shouldShowTime = remember {
        state.shouldShowTime
    }
    val pickerStyle = if (shouldShowTime) {
        DateTimePickerStyle.DateTime
    } else {
        DateTimePickerStyle.Date
    }
    var openDialog by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // the field
    if (isEditable) {
        val textFieldColors = if (epochMillis == null) {
            OutlinedTextFieldDefaults.colors(
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = Color.Gray,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = Color.Gray,
                unfocusedTextColor = Color.Gray,
                focusedSupportingTextColor = if (isRequired) {
                    Color.Red
                } else {
                    Color.Unspecified
                },
                unfocusedSupportingTextColor = if (isRequired) {
                    Color.Red
                } else {
                    Color.Unspecified
                },
                disabledSupportingTextColor = if (isRequired) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        } else {
            OutlinedTextFieldDefaults.colors(
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                focusedSupportingTextColor = if (isRequired) {
                    Color.Red
                } else {
                    Color.Unspecified
                },
                unfocusedSupportingTextColor = if (isRequired) {
                    Color.Red
                } else {
                    Color.Unspecified
                },
                disabledSupportingTextColor = if (isRequired) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
        ) {
            OutlinedTextField(
                value = epochMillis?.formattedDateTime(state.shouldShowTime) ?: "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxSize()
                    .focusable(true, interactionSource),
                readOnly = true,
                enabled = true,
                label = {
                    val text = if (isRequired) {
                        "${state.label} *"
                    } else {
                        state.label
                    }
                    Text(text = text)
                },
                trailingIcon = {
                    if (epochMillis != null) {
                        IconButton(
                            onClick = { state.clearValue() },
                            modifier = Modifier.semantics {
                                contentDescription = "Clear text button"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Text"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { openDialog = true },
                            modifier = Modifier.semantics {
                                contentDescription = "Show datetime picker"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditCalendar,
                                contentDescription = "Edit calendar icon"
                            )
                        }
                    }
                },
                supportingText = {
                    if (epochMillis == null && isRequired) {
                        Text(text = stringResource(R.string.required))
                    } else {
                        Text(text = state.description)
                    }
                },
                visualTransformation = if (state.value.value == null)
                    PlaceholderTransformation(stringResource(id = R.string.no_value))
                else VisualTransformation.None,
                singleLine = true,
                colors = textFieldColors,
                interactionSource = interactionSource
            )
        }
    } else {
        ImmutableDate(
            valueString = epochMillis?.formattedDateTime(state.shouldShowTime) ?: "",
            label = state.label,
            supportingText = state.description
        )
    }

    if (openDialog) {
        val pickerState = rememberDateTimePickerState(
            pickerStyle,
            state.minEpochMillis,
            state.maxEpochMillis,
            epochMillis,
            state.label,
            state.description,
            DateTimePickerInput.Date
        )
        // the picker dialog
        DateTimePicker(
            state = pickerState,
            onDismissRequest = { openDialog = false },
            onCancelled = { openDialog = false },
            onConfirmed = {
                state.setValue(pickerState.selectedDateTimeMillis)
                openDialog = false
            })
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                // open the date picker dialog only when the touch is released
                openDialog = true
            }
        }
    }
}

@Stable
@Composable
private fun ImmutableDate(
    valueString: String,
    label: String,
    supportingText: String,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = valueString,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth(),
            enabled = false,
            readOnly = true,
            label = { Text(text = label) },
            supportingText = { Text(text = supportingText) },
            visualTransformation = if (valueString.isEmpty())
                PlaceholderTransformation(stringResource(id = R.string.no_value))
            else VisualTransformation.None,
            colors = colors
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImmutableDatePreview() {
    Column {
        ImmutableDate(label = "Date", valueString = "1234", supportingText = "supporting text")
    }
}
