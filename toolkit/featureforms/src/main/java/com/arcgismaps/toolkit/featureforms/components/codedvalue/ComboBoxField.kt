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

package com.arcgismaps.toolkit.featureforms.components.codedvalue

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.utils.DialogType
import com.arcgismaps.toolkit.featureforms.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.utils.computeWindowSizeClasses
import com.arcgismaps.toolkit.featureforms.utils.conditional
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun ComboBoxField(
    state: CodedValueFieldState,
    modifier: Modifier = Modifier
) {
    val dialogRequester = LocalDialogRequester.current
    val value by state.value
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val label = remember(isRequired) {
        if (isRequired) {
            "${state.label} *"
        } else {
            state.label
        }
    }
    val placeholder = if (isRequired) {
        stringResource(R.string.enter_value)
    } else if (state.showNoValueOption == FormInputNoValueOption.Show) {
        state.noValueLabel.ifEmpty { stringResource(R.string.no_value) }
    } else ""
    // show if any errors are present as the supporting text with the error color
    val (supportingText, supportingTextColor) = if (value.error is ValidationErrorState.NoError) {
        Pair(state.description, Color.Unspecified)
    } else {
        Pair(value.error.getString(), MaterialTheme.colorScheme.error)
    }

    BaseTextField(
        text = state.getCodedValueNameOrNull(value.data) ?: value.data,
        onValueChange = state::onValueChanged,
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        trailingIcon = Icons.Outlined.List,
        supportingText = {
            Text(
                text = supportingText,
                color = supportingTextColor,
                modifier = Modifier.semantics { contentDescription = "supporting text" }
            )
        },
        interactionSource = interactionSource,
        onFocusChange = state::onFocusChanged
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                if (isEditable) {
                    dialogRequester.requestDialog(DialogType.ComboBoxDialog(state))
                }
            }
        }
    }
}

@Composable
internal fun ComboBoxDialog(
    initialValue: String,
    values: Map<Any?, String>,
    label: String,
    description: String,
    isRequired: Boolean,
    noValueOption: FormInputNoValueOption,
    noValueLabel: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val windowSizeClass = computeWindowSizeClasses(LocalContext.current)
    var searchText by rememberSaveable { mutableStateOf("") }
    val codedValues = if (!isRequired) {
        if (noValueOption == FormInputNoValueOption.Show) {
            mapOf("" to noValueLabel) + values
        } else values
    } else values

    val filteredList by remember {
        derivedStateOf {
            codedValues.filter {
                it.value.contains(searchText, ignoreCase = true)
            }
        }
    }
    // show the dialog as fullscreen for devices which are classified as compact window size
    // like most phones, otherwise as a windowed dialog for expanded screens like tablets
    val showAsFullScreen = when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
        }

        Configuration.ORIENTATION_LANDSCAPE -> {
            windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
        }

        else -> {
            true
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.conditional(
                condition = showAsFullScreen,
                ifTrue = {
                    fillMaxSize()
                },
                ifFalse = {
                    width(600.dp)
                        .heightIn(max = (configuration.screenHeightDp * 0.8).dp)
                        .wrapContentHeight()
                }),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                                imeAction = ImeAction.Done,
                                keyboardType = keyboardType
                            ),
                            shape = RoundedCornerShape(15.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                        )
                        TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.semantics {
                                contentDescription = "combo box done selection"
                            }
                        ) {
                            Text(text = stringResource(R.string.done))
                        }
                    }
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Divider(modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "ComboBoxDialogLazyColumn"
                    }) {
                    items(filteredList.count()) {
                        val code = filteredList.keys.elementAt(it)
                        val name = filteredList.getValue(code)
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = name,
                                    style = if (name == noValueLabel) LocalTextStyle.current.copy(
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
                                    if (name == noValueLabel) {
                                        onValueChange("")
                                    } else {
                                        onValueChange(name)
                                    }
                                }
                                .semantics {
                                    contentDescription = if (name == noValueLabel) {
                                        "no value row"
                                    } else {
                                        "$name list item"
                                    }
                                },
                            trailingContent = {
                                if (name == initialValue || (name == noValueLabel && initialValue.isEmpty())) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "list item check"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ComboBoxDialogPreview() {
    ComboBoxDialog(
        initialValue = "x",
        values = mapOf(
            "Birch" to "Birch",
            "Maple" to "Maple",
            "Oak" to "Oak",
            "Spruce" to "Spruce",
            "Hickory" to "Hickory",
            "Hemlock" to "Hemlock"
        ),
        label = "Types",
        description = "Select the tree species",
        isRequired = false,
        noValueOption = FormInputNoValueOption.Show,
        noValueLabel = "No Value",
        onValueChange = {},
        onDismissRequest = {},
        keyboardType = KeyboardType.Ascii
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ComboBoxPreview() {
    val scope = rememberCoroutineScope()
    val state = CodedValueFieldState(
        properties = CodedValueFieldProperties(
            label = "Types",
            placeholder = "",
            description = "Select the tree species",
            value = MutableStateFlow(""),
            editable = MutableStateFlow(true),
            required = MutableStateFlow(false),
            visible = MutableStateFlow(true),
            fieldType = FieldType.Text,
            codedValues = listOf(),
            showNoValueOption = FormInputNoValueOption.Show,
            noValueLabel = "No value"
        ),
        scope = scope,
        onEditValue = {},
        defaultValidator = { emptyList() }
    )
    ComboBoxField(state = state)
}


