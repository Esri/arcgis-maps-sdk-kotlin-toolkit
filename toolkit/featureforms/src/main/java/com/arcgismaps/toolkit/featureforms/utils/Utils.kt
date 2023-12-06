package com.arcgismaps.toolkit.featureforms.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

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

/**
 * Applies the specified [modifier] only if the [condition] evaluates to true.
 *
 */
internal fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier) : Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
