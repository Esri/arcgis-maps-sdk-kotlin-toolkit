package com.arcgismaps.toolkit.featureforms.components.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.utils.ClearFocus
import com.arcgismaps.toolkit.featureforms.utils.PlaceholderTransformation

@Composable
internal fun FormTextField(
    state: FormTextFieldState,
    modifier: Modifier = Modifier,
) {
    val hasError by state.hasError
    val isFocused by state.isFocused
    var clearFocus by remember { mutableStateOf(false) }
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    val label = remember(isRequired) {
        if (isRequired) {
            "${state.label} *"
        } else {
            state.label
        }
    }
    val text by state.value.collectAsState()
    val supportingText by state.supportingText
    val contentLength =
        if (state.minLength > 0 || state.maxLength > 0) {
            text.length.toString()
        } else ""

    // if the keyboard is gone clear focus from the field as a side-effect
    ClearFocus(clearFocus) { clearFocus = false }

    Column(modifier = modifier
        .fillMaxSize()
        .onFocusChanged { state.onFocusChanged(it.hasFocus) }
        .pointerInput(Unit) {
            // any tap on a blank space will also dismiss the keyboard and clear focus
            detectTapGestures { clearFocus = true }
        }
        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                state.onValueChanged(it)
            },
            modifier = Modifier
                .fillMaxSize()
                .focusable(!isEditable)
                .semantics { contentDescription = "outlined text field" },
            readOnly = !isEditable,
            enabled = isEditable,
            label = {
                Text(
                    text = label,
                    modifier = Modifier.semantics { contentDescription = "label" })
            },
            trailingIcon = {
                if (isEditable && isFocused && !state.singleLine && text.isNotEmpty()) {
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
                } else if (isEditable && text.isNotEmpty()) {
                    IconButton(
                        onClick = { state.onValueChanged("") },
                        modifier = Modifier.semantics { contentDescription = "Clear text button" }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear Text"
                        )
                    }
                }
            },
            supportingText = {
                val textColor = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
                Row {
                    if (supportingText.isNotEmpty()) {
                        Text(
                            text = supportingText,
                            modifier = Modifier
                                .semantics { contentDescription = "helper" }
                                .clickable {
                                    clearFocus = true
                                },
                            color = textColor
                        )
                    }
                    if (isFocused) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = contentLength,
                            modifier = Modifier.semantics { contentDescription = "char count" },
                            color = textColor
                        )
                    }
                }
            },
            visualTransformation = if (text.isEmpty())
                PlaceholderTransformation(state.placeholder.ifEmpty { " " })
            else VisualTransformation.None,
            keyboardActions = KeyboardActions(
                onDone = { clearFocus = true }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (state.singleLine) ImeAction.Done else ImeAction.None
            ),
            singleLine = state.singleLine,
            colors = if (text.isEmpty() && state.placeholder.isNotEmpty())
                OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.Gray,
                    focusedTextColor = Color.Gray
                )
            else
                OutlinedTextFieldDefaults.colors()
        )
    }
}
