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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun DateTimeField(
    state: DateTimeFieldState,
    modifier: Modifier = Modifier
) {
    val isEditable by state.isEditable
    val isRequired by state.isRequired
    val epochMillis by state.value
    var showDatePickerDialog by remember { mutableStateOf(false) }
    
    val textFieldColors = if (epochMillis == null) {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Gray,
            unfocusedTextColor = Color.Gray,
            disabledTextColor = Color.Gray,
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
                Color.Unspecified
            }
        )
    } else {
        OutlinedTextFieldDefaults.colors(
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
                Color.Unspecified
            }
        )
    }
    
    if (showDatePickerDialog) {
        DateTimePicker(state)
    } else if (isEditable) {
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
                    .clickable { showDatePickerDialog = true },
                readOnly = true,
                label = {
                    val text = if (isRequired) {
                        "${state.label} *"
                    } else {
                        state.label
                    }
                    Text(text = text)
                },
                placeholder = {
                    Text(
                        text = if (epochMillis == null) {
                            stringResource(id = R.string.novalue)
                        } else {
                            state.placeholderText
                        }
                    )
                },
                trailingIcon = {
                    if (epochMillis != null) {
                        IconButton(
                            onClick = { state.resetValue() },
                            modifier = Modifier.semantics { contentDescription = "Clear text button" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Text"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showDatePickerDialog = true },
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
        println("${state.label} is immutable")
        ImmutableDate(
            valueString = epochMillis?.formattedDateTime() ?: stringResource(id = R.string.novalue),
            label = state.label,
            supportingText = state.description
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePicker(state: DateTimeFieldState) {
    
    val initialValue by state.value
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialValue
    )
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DatePickerDialog(
            onDismissRequest = {},
            confirmButton = {},
        ) {
            DatePicker(
                state = datePickerState
            )
            
        }
    }
}


@Stable
@Composable
private fun ImmutableDate(
    valueString: String,
    label: String,
    supportingText: String
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



