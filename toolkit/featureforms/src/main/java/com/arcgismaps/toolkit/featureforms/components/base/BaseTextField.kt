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

package com.arcgismaps.toolkit.featureforms.components.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.utils.ClearFocus
import com.arcgismaps.toolkit.featureforms.utils.PlaceholderTransformation

@Composable
private fun trailingIcon(
    text: String,
    isEditable: Boolean,
    singleLine: Boolean,
    isFocused: Boolean,
    trailingIcon: ImageVector?,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
): (@Composable () -> Unit)? {
    // single line field and is editable
    return if (singleLine && isEditable && text.isEmpty() && trailingIcon != null) {
        {
            // show a trailing icon if provided when the field is empty
            Icon(imageVector = trailingIcon, contentDescription = "field icon")
        }
    } else if (singleLine && isEditable && text.isNotEmpty()) {
        {
            // show a clear icon instead if the field is not empty
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.semantics {
                contentDescription = "Clear text button"
            }) {
                Icon(
                    imageVector = Icons.Rounded.Clear, contentDescription = "Clear Text"
                )
            }
        }
    } else if (singleLine && trailingIcon != null) {
        // single line field but not editable
        {
            // show a trailing icon to indicate field type
            Icon(imageVector = trailingIcon, contentDescription = "field icon")
        }
    } else if (!singleLine && isEditable && isFocused) {
        // multiline editable field
        {
            // show a done button only when focused
            IconButton(onClick = { onDone() }, modifier = Modifier.semantics {
                contentDescription = "Save local edit button"
            }) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle, contentDescription = "Done"
                )
            }
        }

    } else if (!singleLine && isEditable && text.isNotEmpty()) {
        {
            // show a clear icon instead if the multiline field is not empty
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.semantics {
                contentDescription = "Clear text button"
            }) {
                Icon(
                    imageVector = Icons.Rounded.Clear, contentDescription = "Clear Text"
                )
            }
        }
    } else {
        null
    }
}

/**
 * A base text field component built on top of an [OutlinedTextField] that provides a standard for
 * visual and behavioral properties. This can be used to build more customized composite components.
 *
 * The BaseTextField also takes care of clearing focus when the keyboard is dismissed or tapped
 * outside the input area.
 *
 * @param text the input text to be shown in the text field.
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback.
 * @param modifier a [Modifier] for this text field.
 * @param readOnly controls the editable state of the text field. When true, the text field cannot
 * be modified. However, a user can focus it and copy text from it. Read-only text fields are
 * usually used to display pre-filled forms that a user cannot edit.
 * @param isEditable controls if the text field can be edited. When false, this component will
 * not respond to user input, and it will appear visually disabled.
 * @param label the title to be displayed for the text field.
 * @param placeholder the text to be displayed when the text field input text is empty.
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines.
 * @param keyboardType the keyboard type to use depending on the FormFieldElement input type.
 * @param trailingIcon the icon to be displayed at the end of the text field container.
 * @param supportingText supporting text to be displayed below the text field.
 * @param onFocusChange callback that is triggered when the focus state for this text field changes.
 * @param interactionSource the MutableInteractionSource representing the stream of Interactions
 * for this text field.
 * @param trailingContent a widget to be displayed at the end of the text field container.
 */
@Composable
internal fun BaseTextFieldOld(
    text: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = !isEditable,
    keyboardType: KeyboardType = KeyboardType.Ascii,
    trailingIcon: ImageVector? = null,
    supportingText: @Composable (ColumnScope.() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    var clearFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // if the keyboard is gone clear focus from the field as a side-effect
    ClearFocus(clearFocus) { clearFocus = false }

    Column(modifier = modifier
        .onFocusChanged {
            isFocused = it.hasFocus
            onFocusChange?.invoke(it.hasFocus)
        }
        .pointerInput(Unit) {
            // any tap on a blank space will also dismiss the keyboard and clear focus
            detectTapGestures { clearFocus = true }
        }
        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "outlined text field" },
            readOnly = readOnly,
            label = {
                Text(
                    text = label,
                    modifier = Modifier.semantics { contentDescription = "label" },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            },
            trailingIcon = trailingContent
                ?: trailingIcon(
                    text,
                    isEditable,
                    singleLine,
                    isFocused,
                    trailingIcon,
                    onValueChange = onValueChange,
                    onDone = { clearFocus = true }
                ),
            supportingText = {
                Column(
                    modifier = Modifier
                        .clickable {
                            clearFocus = true
                        }
                        .semantics { contentDescription = "supporting text" }
                ) {
                    supportingText?.invoke(this)
                }
            },
            visualTransformation = if (text.isEmpty())
                PlaceholderTransformation(placeholder.ifEmpty { " " })
            else VisualTransformation.None,
            keyboardActions = KeyboardActions(
                onDone = { clearFocus = true }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                keyboardType = keyboardType
            ),
            singleLine = singleLine,
            interactionSource = interactionSource,
            colors = baseTextFieldColors(
                isEditable = isEditable,
                isEmpty = text.isEmpty(),
                isPlaceholderEmpty = placeholder.isEmpty(),
                interactionSource
            )
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BaseTextFieldPreview() {
    MaterialTheme {
        BaseTextFieldOld(
            text = "",
            onValueChange = {},
            isEditable = true,
            label = "Title",
            placeholder = "Enter Value",
            singleLine = true,
            trailingIcon = Icons.Rounded.TextFields,
            supportingText = {
                Text(text = "A Description")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BaseTextField(
    text: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = !isEditable,
    keyboardType: KeyboardType = KeyboardType.Ascii,
    textStyle: TextStyle = LocalTextStyle.current,
    trailingIcon: ImageVector? = null,
    supportingText: @Composable (ColumnScope.() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    var clearFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val visualTransformation = if (text.isEmpty())
        PlaceholderTransformation(placeholder.ifEmpty { " " })
    else VisualTransformation.None
    // if the keyboard is gone clear focus from the field as a side-effect
    ClearFocus(clearFocus) { clearFocus = false }
    val colors = baseTextFieldColors(
        isEditable = isEditable,
        isEmpty = text.isEmpty(),
        isPlaceholderEmpty = placeholder.isEmpty(),
        interactionSource
    )
    val focused by interactionSource.collectIsFocusedAsState()
    // If color is not provided via the text style, use content color as a default
    //val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    //val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    Column(modifier = modifier
        .onFocusChanged {
            isFocused = it.hasFocus
            onFocusChange?.invoke(it.hasFocus)
        }
        .pointerInput(Unit) {
            // any tap on a blank space will also dismiss the keyboard and clear focus
            detectTapGestures { clearFocus = true }
        }
        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        LocalView.current
        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            modifier = Modifier
                // Merge semantics at the beginning of the modifier chain to ensure padding is
                // considered part of the text field.
                .semantics(mergeDescendants = true) {}
                .padding(top = 8.dp)
                .defaultMinSize(
                    minWidth = OutlinedTextFieldDefaults.MinWidth,
                    minHeight = OutlinedTextFieldDefaults.MinHeight
                )
                .fillMaxWidth()
                .semantics { contentDescription = "outlined text field" },
            enabled = true,
            readOnly = readOnly,
            textStyle = textStyle,
            visualTransformation = visualTransformation,
            keyboardActions = KeyboardActions(
                onDone = { clearFocus = true }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                keyboardType = keyboardType
            ),
            interactionSource = interactionSource,
            singleLine = singleLine,
            decorationBox = @Composable { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = text,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    label = {
                        Text(
                            text = label,
                            modifier = Modifier.semantics { contentDescription = "label" },
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    trailingIcon = trailingContent
                        ?: trailingIcon(
                            text,
                            isEditable,
                            singleLine,
                            isFocused,
                            trailingIcon,
                            onValueChange = onValueChange,
                            onDone = { clearFocus = true }
                        ),
                    supportingText = {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    clearFocus = true
                                }
                                .semantics { contentDescription = "supporting text" }
                        ) {
                            supportingText?.invoke(this)
                        }
                    },
                    singleLine = singleLine,
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    container = {
                        OutlinedTextFieldDefaults.ContainerBox(
                            enabled = true,
                            isError = false,
                            interactionSource,
                            colors,
                            OutlinedTextFieldDefaults.shape,
                            focusedBorderThickness = if (isEditable) 2.dp else 1.dp
                        )
                    }
                )
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BaseTextFieldV2Preview() {
    BaseTextField(
        text = "",
        onValueChange = {},
        isEditable = true,
        label = "Title",
        placeholder = "Enter Value",
        singleLine = true,
        trailingIcon = Icons.Rounded.TextFields,
        supportingText = {
            Text(text = "A Description")
        }
    )
}
