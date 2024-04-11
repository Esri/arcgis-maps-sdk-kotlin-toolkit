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

package com.arcgismaps.toolkit.featureforms.internal.components.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.internal.utils.PlaceholderTransformation


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
 * @param isEditable controls if the text field can be edited. When false, this component will
 * not respond to user input, and will be rendered differently without an outline.
 * @param label the title to be displayed for the text field.
 * @param placeholder the text to be displayed when the text field input text is empty.
 * @param supportingText supporting text to be displayed below the text field.
 * @param isError indicates if the text field's current value is in error. If set to true, the
 * label, bottom indicator and trailing icon by default will be displayed in error color.
 * @param isRequired if true, the [label] will be suffixed with a "*".
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 * field instead of wrapping onto multiple lines.
 * @param modifier a [Modifier] for this text field.
 * @param readOnly controls the editable state of the text field. When true, the text field cannot
 * be modified. However, a user can focus it and copy text from it.
 * @param showCharacterCount if true shows the current character count of the [text].
 * @param keyboardType the keyboard type to use depending on the FormFieldElement input type.
 * @param trailingIcon the icon to be displayed at the end of the text field container.
 * @param onFocusChange callback that is triggered when the focus state for this text field changes.
 * @param interactionSource the MutableInteractionSource representing the stream of Interactions
 * for this text field.
 * @param trailingContent a widget to be displayed at the end of the text field container.
 */
@Composable
internal fun BaseTextField(
    text: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    label: String,
    placeholder: String,
    supportingText: String,
    isError: Boolean,
    isRequired: Boolean,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = !isEditable,
    showCharacterCount: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Ascii,
    trailingIcon: ImageVector? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val visualTransformation = if (text.isEmpty())
        PlaceholderTransformation(placeholder.ifEmpty { " " })
    else VisualTransformation.None
    val title = remember(isRequired, isEditable) {
        if (isRequired && isEditable) {
            "$label *"
        } else {
            label
        }
    }
    val contentLength = "${text.length}"
    val isSupportingTextAvailable = supportingText.isNotEmpty() || (showCharacterCount && isFocused)
    val colors = baseTextFieldColors(text.isEmpty(), placeholder.isEmpty())
    Column(modifier = modifier
        .onFocusChanged {
            isFocused = it.hasFocus
            onFocusChange?.invoke(it.hasFocus)
        }
        .pointerInput(Unit) {
            // any tap on a blank space will also dismiss the keyboard and clear focus
            detectTapGestures { focusManager.clearFocus() }
        }
        .padding(15.dp)
    ) {
        if (isEditable) {
            OutlinedTextField(
                value = text,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "outlined text field" },
                enabled = true,
                readOnly = readOnly,
                isError = isError,
                textStyle = MaterialTheme.typography.bodyLarge,
                label = {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { contentDescription = "label" },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                trailingIcon = trailingContent
                    ?: trailingIcon(
                        text.isEmpty(),
                        singleLine,
                        isFocused,
                        trailingIcon,
                        onValueChange = onValueChange,
                        onDone = { focusManager.clearFocus() }
                    ),
                supportingText = if (isSupportingTextAvailable) {
                    {
                        Row(modifier = Modifier.clickable { focusManager.clearFocus() }) {
                            if (supportingText.isNotEmpty()) {
                                Text(
                                    text = supportingText,
                                    modifier = Modifier.semantics {
                                        contentDescription = "supporting text"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (showCharacterCount && isFocused) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = contentLength,
                                    modifier = Modifier.semantics {
                                        contentDescription = "char count"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else null,
                visualTransformation = visualTransformation,
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                    keyboardType = keyboardType
                ),
                interactionSource = interactionSource,
                singleLine = singleLine,
                colors = colors
            )
        } else {
            ReadOnlyTextField(
                label = label,
                text = text,
                supportingText = supportingText
            )
        }
    }
}

@Composable
private fun trailingIcon(
    isEmpty: Boolean,
    singleLine: Boolean,
    isFocused: Boolean,
    trailingIcon: ImageVector?,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
): @Composable () -> Unit = {
    if (!singleLine && isFocused) {
        // show a done button only when focused for a multi line text field
        IconButton(
            onClick = onDone,
            modifier = Modifier.semantics { contentDescription = "Save local edit button" }
        ) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Done")
        }
    } else if (singleLine && trailingIcon != null && isEmpty) {
        // show a trailing icon if provided when the single line field is empty
        Icon(imageVector = trailingIcon, contentDescription = "field icon")
    } else if (!isEmpty) {
        // show a clear icon if the field is not empty
        IconButton(
            onClick = { onValueChange("") },
            modifier = Modifier.semantics { contentDescription = "Clear text button" }
        ) {
            Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear Text")
        }
    }
}

@Composable
private fun ReadOnlyTextField(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium
        )
        SelectionContainer(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(text = text.ifEmpty { "--" }, style = MaterialTheme.typography.bodyLarge)
        }
        if (supportingText.isNotEmpty()) {
            Text(text = supportingText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ReadOnlyTextFieldPreview() {
    MaterialTheme {
        BaseTextField(
            text = "This is a read-only text field",
            onValueChange = {},
            isEditable = false,
            label = "Title",
            placeholder = "Enter Value",
            supportingText = "A Description",
            isError = false,
            isRequired = true,
            singleLine = false,
            trailingIcon = Icons.Rounded.TextFields,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BaseTextFieldPreview() {
    MaterialTheme {
        BaseTextField(
            text = "This is a text",
            onValueChange = {},
            isEditable = true,
            label = "Title",
            placeholder = "Enter Value",
            supportingText = "A Description",
            isError = false,
            isRequired = true,
            singleLine = false,
            trailingIcon = Icons.Rounded.TextFields,
        )
    }
}
