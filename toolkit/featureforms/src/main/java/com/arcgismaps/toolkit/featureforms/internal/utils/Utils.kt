/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureforms.internal.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.arcgismaps.data.CodedValue

/**
 * Changes the visual output of the placeholder and label properties of a TextField. Using this
 * transformation, the placeholder is always visible even if empty and puts the label above the
 * TextField as its default position.
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

/**
 * Utility function that returns true if the type is null or if it is an empty string.
 */
internal fun Any?.isNullOrEmptyString(): Boolean {
    return if (this is String?) {
        isNullOrEmpty()
    } else {
        false
    }
}

/**
 * Provide a format string for any numeric type.
 *
 * @param digits: If the number is floating point, restricts the decimal digits
 * @return a formatted string representing the number.
 */
internal fun Number?.format(digits: Int = 2): String =
    when (this) {
        is Double -> "%.${digits}f".format(this)
        is Float -> "%.${digits}f".format(this)
        else -> "$this"
    }

/**
 * Converts a list of [CodedValue]s to a map of ([CodedValue.code],[CodedValue.name]).
 */
internal fun List<CodedValue>.toMap(): Map<Any?, String> {
    return associateBy(CodedValue::code, CodedValue::name)
}
