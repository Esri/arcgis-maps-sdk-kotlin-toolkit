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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.arcgismaps.toolkit.featureforms.theme.EditableTextFieldColors
import com.arcgismaps.toolkit.featureforms.theme.LocalColorScheme
import com.arcgismaps.toolkit.featureforms.theme.LocalTypography
import kotlin.math.abs


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
 * @param hasValueExpression if true, a special icon will be displayed at the end of the text field
 * given that [readOnly] is also set to true.
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
    hasValueExpression: Boolean,
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
    val colors = LocalColorScheme.current.editableTextFieldColors.toTextFieldColors(
        textIsEmpty = text.isEmpty(),
        placeHolderIsEmpty = placeholder.isEmpty()
    )
    val typography = LocalTypography.current.editableTextFieldTypography
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
                    .semantics { contentDescription = "outlined text field" }
                    .preventParentScrollWhenAtBounds(),
                enabled = true,
                readOnly = readOnly,
                isError = isError,
                textStyle = typography.textStyle,
                label = {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { contentDescription = "label" },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = typography.labelStyle
                    )
                },
                trailingIcon = trailingContent
                    ?: trailingIcon(
                        text.isEmpty(),
                        singleLine,
                        trailingIcon,
                        onValueChange = onValueChange,
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
                                    style = typography.supportingTextStyle
                                )
                            }
                            if (showCharacterCount && isFocused) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = contentLength,
                                    modifier = Modifier.semantics {
                                        contentDescription = "char count"
                                    },
                                    style = typography.supportingTextStyle
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
                minLines = if (singleLine) 1 else BaseTextFieldDefaults.MIN_LINES_MULTI_LINE,
                maxLines = if (singleLine) 1 else BaseTextFieldDefaults.MAX_LINES_MULTI_LINE,
                colors = colors
            )
        } else {
            ReadOnlyTextField(
                label = label,
                text = text,
                supportingText = supportingText,
                isError = isError,
                hasValueExpression = hasValueExpression
            )
        }
    }
}

@Composable
private fun trailingIcon(
    isEmpty: Boolean,
    singleLine: Boolean,
    trailingIcon: ImageVector?,
    onValueChange: (String) -> Unit,
): @Composable () -> Unit = {
    if (trailingIcon != null && isEmpty) {
        // show a trailing icon if provided when the field is empty
        Icon(imageVector = trailingIcon, contentDescription = "field icon")
    } else if (!isEmpty && singleLine) {
        // show a clear icon if the field is not empty and is single line
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
    isError: Boolean,
    hasValueExpression: Boolean
) {
    val colors = LocalColorScheme.current.readOnlyFieldColors
    val typography = LocalTypography.current.readOnlyFieldTypography
    Row(
        modifier = modifier
            .fillMaxWidth()
            // merge descendants semantics to make them part of the parent node
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = label,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = colors.labelColor,
                style = typography.labelStyle
            )
            SelectionContainer(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = text.ifEmpty { "--" },
                    color = colors.textColor,
                    style = typography.textStyle
                )
            }
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    color = if (!isError) {
                        colors.supportingTextColor
                    } else {
                        colors.errorSupportingTextColor
                    },
                    style = typography.supportingTextStyle
                )
            }
        }
        if (hasValueExpression) {
            Icon(
                imageVector = Icons.Rounded.Code,
                contentDescription = "calculated field",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Prevents the parent scroll from scrolling when the child is at the bounds of the scrollable area.
 * See [doc](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll#nested-scroll-participation)
 * on nested scrolling for more information.
 */
internal fun Modifier.preventParentScrollWhenAtBounds(): Modifier {

    // Create a nested scroll connection to prevent the parent from scrolling when the child is at the bounds
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // if there is any vertical scroll available, consume it by returning the available offset
            return if (abs(available.y) > 0) {
                available
            } else {
                Offset.Zero
            }
        }
    }

    return this.nestedScroll(nestedScrollConnection)
}

@Composable
internal fun EditableTextFieldColors.toTextFieldColors(
    textIsEmpty: Boolean,
    placeHolderIsEmpty: Boolean
): TextFieldColors {
    // transform placeholder colors into text colors
    val transformPlaceHolderColors = textIsEmpty && !placeHolderIsEmpty
    val transformedFocusedTextColor = if (transformPlaceHolderColors) {
        // if placeholder is visible, make it lighter than the actual input text color
        focusedPlaceholderColor
    } else {
        focusedTextColor
    }
    val transformedUnFocusedTextColor = if (transformPlaceHolderColors) {
        unfocusedPlaceholderColor
    } else {
        unfocusedTextColor
    }
    val transformedErrorTextColor = if (transformPlaceHolderColors) {
        errorPlaceholderColor
    } else {
        errorTextColor
    }
    return TextFieldDefaults.colors(
        focusedTextColor = transformedFocusedTextColor,
        unfocusedTextColor = transformedUnFocusedTextColor,
        errorTextColor = transformedErrorTextColor,
        focusedContainerColor = focusedContainerColor,
        unfocusedContainerColor = unfocusedContainerColor,
        errorContainerColor = errorContainerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = textSelectionColors,
        focusedIndicatorColor = focusedIndicatorColor,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        errorIndicatorColor = errorIndicatorColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        errorLabelColor = errorLabelColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        errorSuffixColor = errorSuffixColor
    )
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
            hasValueExpression = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SingleLineTextFieldPreview() {
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
            singleLine = true,
            trailingIcon = Icons.Rounded.TextFields,
            hasValueExpression = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MultiLineTextFieldPreview() {
    MaterialTheme {
        BaseTextField(
            text = "This is a multi-line text",
            onValueChange = {},
            isEditable = true,
            label = "Title",
            placeholder = "Enter Value",
            supportingText = "A Description",
            isError = false,
            isRequired = true,
            singleLine = false,
            trailingIcon = Icons.Rounded.TextFields,
            hasValueExpression = false
        )
    }
}

