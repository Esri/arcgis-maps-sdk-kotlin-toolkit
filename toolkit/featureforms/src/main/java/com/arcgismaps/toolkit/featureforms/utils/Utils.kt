package com.arcgismaps.toolkit.featureforms.utils

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal data class FocusInfo(
    var label: String
)

internal val LocalFocusedField = compositionLocalOf {
    FocusInfo("")
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
        return TransformedText(AnnotatedString(placeholder), mapping)
    }
}

internal fun Modifier.focusOnStart(label: String): Modifier = composed {
    val localFieldFocus = LocalFocusedField.current
    val config = LocalConfiguration.current
    var orientation by rememberSaveable {
        mutableStateOf(config.orientation)
    }
    val focusRequester = remember { FocusRequester() }
    this.focusRequester(focusRequester)
        .onGloballyPositioned {
            if (localFieldFocus.label == label) {
                focusRequester.requestFocus()
            }
        }
        .onFocusChanged {
            if (it.hasFocus) {
                localFieldFocus.label = label
            } else {
                if (orientation == config.orientation) {
                    localFieldFocus.label = ""
                } else {
                    orientation = config.orientation
                }
            }
        }
}
