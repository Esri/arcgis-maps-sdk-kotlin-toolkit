package com.arcgismaps.toolkit.featureforms.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

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
