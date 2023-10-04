/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.components.combo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField

@Composable
internal fun ComboBoxField(state: ComboBoxFieldState, modifier: Modifier = Modifier) {
    val value by state.value.collectAsState()
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // to check if the field was ever focused by the user
    var wasFocused by rememberSaveable { mutableStateOf(false) }
    val label = remember(isRequired) {
        if (isRequired) {
            "${state.label} *"
        } else {
            state.label
        }
    }

    BaseTextField(
        text = value,
        onValueChange = { state.onValueChanged(it) },
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = label,
        placeholder = state.placeholder,
        singleLine = true,
        trailingIcon = Icons.Outlined.List,
        supportingText = {
            // if the field was focused and is required, validate the current value
            if (wasFocused && isRequired && value.isEmpty()) {
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
        },
        interactionSource = interactionSource
    )

    if (showDialog) {
        ComboBoxDialog(
            initialValue = value,
            values = state.codedValues.map { it.code.toString() },
            label = state.label,
            description = state.description,
            isRequired = isRequired,
            noValueOption = state.showNoValueOption,
            noValueLabel = state.noValueLabel.ifEmpty { stringResource(R.string.no_value) },
            onValueChange = {
                state.onValueChanged(it)
            }
        ) {
            showDialog = false
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                wasFocused = true
                showDialog = true
            }
        }
    }
}

@Composable
internal fun ComboBoxDialog(
    initialValue: String,
    values: List<String>,
    label: String,
    description: String,
    isRequired: Boolean,
    noValueOption: FormInputNoValueOption,
    noValueLabel: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val codedValues = if (!isRequired) {
        if (noValueOption == FormInputNoValueOption.Show) {
            listOf(noValueLabel) + values
        } else values
    } else values

    val filteredList by remember {
        derivedStateOf {
            codedValues.filter {
                it.contains(searchText, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .padding(start = 15.dp, top = 15.dp, bottom = 10.dp, end = 10.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(text = stringResource(R.string.filter, label))
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(15.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                        )
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(R.string.done))
                        }
                    }
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredList) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = it,
                                    style = if (it == noValueLabel) LocalTextStyle.current.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Light
                                    )
                                    else LocalTextStyle.current
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // if the no value label was selected, set the value to be empty
                                    onValueChange(if (it == noValueLabel) "" else it)
                                },
                            trailingContent = {
                                if (it == initialValue || (it == noValueLabel && initialValue.isEmpty())) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview
@Composable
private fun ComboBoxDialogPreview() {
    ComboBoxDialog(
        initialValue = "Birch",
        values = listOf("Birch", "Maple", "Oak", "Spruce", "Hickory", "Hemlock"),
        label = "Types",
        description = "Select the tree species",
        isRequired = false,
        noValueOption = FormInputNoValueOption.Show,
        noValueLabel = "No Value",
        onValueChange = {},
        onDismissRequest = {}
    )
}
