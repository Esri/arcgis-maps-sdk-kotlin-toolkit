package com.arcgismaps.toolkit.featureforms.components

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FormTextField(
    label: String,
    description: String,
    minLength: Int,
    maxLength: Int,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val helperText = remember {
        buildString {
            if (minLength > 0)
                append("Enter $minLength to ")
            if (maxLength > 0)
                append("Maximum $maxLength characters")
        }
    }

    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val supportingText = if (isError) {
        errorMessage
    } else {
        description.ifEmpty {
            if (isFocused) {
                helperText
            } else {
                ""
            }
        }
    }

    Column(modifier = modifier
        .fillMaxSize()
        .onFocusChanged { isFocused = it.hasFocus }
        .pointerInput(Unit) {
            detectTapGestures {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                if (it.length !in minLength..maxLength) {
                    isError = true
                    errorMessage = helperText
                } else {
                    isError = false
                    errorMessage = ""
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = {
                Text(text = label)
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear Text"
                        )
                    }
                }
            },
            supportingText = {
                if (supportingText.isNotEmpty()) {
                    Text(
                        text = supportingText,
                        color = if (isError) Color.Red else Color.Unspecified
                    )
                }
            },
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (singleLine) ImeAction.Done else ImeAction.None
            ),
            singleLine = singleLine,
            visualTransformation = PlaceholderTransformation(placeholder)
        )
    }
}

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


