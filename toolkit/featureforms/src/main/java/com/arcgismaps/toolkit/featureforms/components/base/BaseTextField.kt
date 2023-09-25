package com.arcgismaps.toolkit.featureforms.components.base

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.utils.ClearFocus
import com.arcgismaps.toolkit.featureforms.utils.PlaceholderTransformation

@Composable
internal fun BaseTextField(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean,
    isEditable: Boolean,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    trailingIcon: ImageVector? = null,
    supportingText: @Composable (ColumnScope.() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
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
                .fillMaxSize()
                .focusable(isEditable, interactionSource)
                .semantics { contentDescription = "outlined text field" },
            readOnly = readOnly,
            enabled = isEditable,
            label = {
                Text(
                    text = label,
                    modifier = Modifier.semantics { contentDescription = "label" })
            },
            trailingIcon = {
                // single line field and is editable
                if (singleLine && isEditable) {
                    // show a trailing icon if provided when the field is empty
                    if (text.isEmpty() && trailingIcon != null) {
                        Icon(imageVector = trailingIcon, contentDescription = "field icon")
                    } else if (text.isNotEmpty()) {
                        // show a clear icon instead if the field is not empty
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.semantics {
                                contentDescription = "Clear text button"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Text"
                            )
                        }
                    }
                    // single line field but not editable
                } else if (singleLine && trailingIcon != null) {
                    // show a trailing icon to indicate field type
                    Icon(imageVector = trailingIcon, contentDescription = "field icon")
                    // multiline editable field
                } else if (!singleLine && isEditable && text.isNotEmpty()) {
                    if (isFocused) {
                        // show a done button only when focused
                        IconButton(
                            onClick = { clearFocus = true },
                            modifier = Modifier.semantics {
                                contentDescription = "Save local edit button"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Done"
                            )
                        }
                    } else {
                        // show a clear icon instead if the multiline field is not empty
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.semantics {
                                contentDescription = "Clear text button"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Text"
                            )
                        }
                    }
                }
            },
            supportingText = {
                Column(
                    modifier = Modifier.clickable {
                        clearFocus = true
                    }
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
                imeAction = if (singleLine) ImeAction.Done else ImeAction.None
            ),
            singleLine = singleLine,
            interactionSource = interactionSource,
            colors = if (text.isEmpty() && placeholder.isNotEmpty())
                OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.Gray,
                    focusedTextColor = Color.Gray
                )
            else
                OutlinedTextFieldDefaults.colors()
        )
    }
}
