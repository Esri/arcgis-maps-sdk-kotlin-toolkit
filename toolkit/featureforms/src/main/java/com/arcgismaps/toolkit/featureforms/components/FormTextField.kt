package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun FormTextField(
    label: String,
    description: String,
    minLength: Int,
    maxLength: Int,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyle(),
    textFieldColors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var shouldClearFocus by remember { mutableStateOf(false) }
    val helperText = remember {
        buildString {
            if (minLength > 0)
                append("Enter $minLength to ")
            if (maxLength > 0)
                append("Maximum $maxLength characters")
        }
    }

    // if the keyboard is gone clear focus from the field as a side-effect
    ClearFocus(shouldClearFocus) {
        shouldClearFocus = false
    }

    val validateInputLength = {
        errorMessage = if (text.length !in minLength..maxLength) {
            helperText
        } else ""
    }

    val supportingText = errorMessage.ifEmpty {
        description.ifEmpty {
            if (isFocused) helperText else ""
        }
    }

    Column(modifier = modifier
        .fillMaxSize()
        .onFocusChanged {
            isFocused = it.hasFocus
        }
        .pointerInput(Unit) {
            // any tap on a blank space outside the text field will also dismiss the keyboard
            // and clear focus
            detectTapGestures { shouldClearFocus = true }
        }
        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                validateInputLength()
            },
            modifier = Modifier.fillMaxSize(),
            label = {
                Text(text = label)
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            text = ""
                            validateInputLength()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear Text"
                        )
                    }
                }
            },
            supportingText = {
                val textColor = if (errorMessage.isEmpty()) Color.Unspecified else Color.Red
                Row {
                    if (supportingText.isNotEmpty()) {
                        Text(text = supportingText, color = textColor)
                    }
                    if (isFocused && helperText.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "${text.length}", color = textColor)
                    }
                }
            },
            visualTransformation = if (text.isEmpty())
                PlaceholderTransformation(placeholder)
            else VisualTransformation.None,
            keyboardActions = KeyboardActions(
                onDone = { shouldClearFocus = true }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (singleLine) ImeAction.Done else ImeAction.None
            ),
            singleLine = singleLine
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ClearFocus(key: Boolean, onComplete: () -> Unit = {}) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    DisposableEffect(key) {
        if (key) {
            // hides the keyboard only if visible
            keyboardController?.hide()
            focusManager.clearFocus()
        }
        onDispose {
            onComplete()
        }
    }
}

/**
 * Changes the visual output of the placeholder and label properties of a TextField. Using this
 * transformation, the placeholder is always visible even if empty and puts the label above the
 * TextField as it's default position.
 */
internal class PlaceholderTransformation(private val placeholder: String) : VisualTransformation {

    private val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = 0
        override fun transformedToOriginal(offset: Int): Int = 0
    }

    override fun filter(text: AnnotatedString): TransformedText {
        // add a blank space if place holder is empty so that the label always stays above
        return TransformedText(AnnotatedString(placeholder.ifEmpty { " " }), mapping)
    }
}
