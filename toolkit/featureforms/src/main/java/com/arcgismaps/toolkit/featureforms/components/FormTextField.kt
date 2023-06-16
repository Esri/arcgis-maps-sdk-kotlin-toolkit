package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun FormTextField(
    label: String,
    modifier: Modifier = Modifier,
    description: String = "",
    minLength: Int = 0,
    maxLength: Int = 0,
    singleLine: Boolean = true,
) {
    val helperText = remember {
        buildString {
            if (minLength > 0) {
                append("Enter $minLength to ")
            } else {
                append("Maximum ")
            }
            append("$maxLength characters")
        }
    }

    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
        .onFocusChanged {
            isFocused = it.hasFocus
        }
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
            placeholder = {
                // TO DO - add placeholder
                //Text(text = "Hint")
            },
            singleLine = singleLine,
            supportingText = {
                Text(
                    text = supportingText,
                    color = if (isError) Color.Red else Color.Unspecified
                )
            },
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (!singleLine) ImeAction.Done else ImeAction.Default
            )
        )
    }
}
