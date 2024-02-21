package com.arcgismaps.toolkit.featureforms.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

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
 * Applies the specified [ifTrue] block modifier if the [condition] evaluates to true, else
 * [ifFalse] is applied.
 */
internal fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: (Modifier.() -> Modifier)? = null
): Modifier {
    return if (condition) {
        then(ifTrue(Modifier))
    } else {
        if (ifFalse != null) {
            then(ifFalse(Modifier))
        } else {
            this
        }
    }
}
