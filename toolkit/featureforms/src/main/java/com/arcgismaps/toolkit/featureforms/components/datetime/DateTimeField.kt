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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R


@Suppress("unused")
internal enum class DateTimePickerStyle {
    Date,
    Time,
    DateTime
}

@Composable
internal fun DateTimeField(
    state: DateTimeFieldState,
    modifier: Modifier = Modifier
) {
    val isEditable by state.isEditable
    val isRequired by state.isRequired
    val epochMillis by state.value
    val pickerStyle = if (state.shouldShowTime) {
        DateTimePickerStyle.DateTime
    } else {
        DateTimePickerStyle.Date
    }
    val pickerState = DateTimePickerState(
        pickerStyle,
        state.minEpochMillis,
        state.maxEpochMillis,
        state.value.value,
        state.label,
        state.description
    ) {
        state.setValue(it)
    }
    
    // the picker dialog
    DateTimePicker(pickerState)
    
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
                value = epochMillis?.formattedDateTime() ?: stringResource(id = R.string.novalue),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxSize()
                    .focusable(false)
                    .clickable {
                        pickerState.setVisibility(true)
                    },
                readOnly = true,
                enabled = false, // disabled to support clickability
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
                            modifier = Modifier.semantics { contentDescription = "Clear text button" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Text"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { pickerState.setVisibility(true) },
                            modifier = Modifier.semantics { contentDescription = "Show datetime picker" }
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
                singleLine = true,
                colors = textFieldColors
            )
        }
    } else {
        ImmutableDate(
            valueString = epochMillis?.formattedDateTime() ?: stringResource(id = R.string.novalue),
            label = state.label,
            supportingText = state.description
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePicker(state: DateTimePickerState) {
    val initialValue = state.value
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialValue
    )
    if (state.visible.value) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DatePickerDialog(
                onDismissRequest = {},
                dismissButton = {
                    Text(text = "Dismiss", modifier = Modifier.clickable {
                        state.setVisibility(false)
                    })
                },
                confirmButton = {
                    Text(text = "Confirm", modifier = Modifier.clickable {
                        datePickerState.selectedDateMillis?.let {
                            state.onValueSet(it)
                        }
                    })
                },
            ) {
                DatePicker(
                    state = datePickerState
                )
                
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
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.onSurface,
        disabledSupportingTextColor = MaterialTheme.colorScheme.onSurface
    )
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



