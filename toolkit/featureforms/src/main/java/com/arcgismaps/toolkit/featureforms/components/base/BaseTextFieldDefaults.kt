/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.components.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 *  Color properties of a base text field. Since non editable fields are rendered as read-only
 *  text fields, the colors actually match disabled fields.
 *
 *  Taken from the material 3 design :https://m3.material.io/components/text-fields/specs.
 */
@Composable
internal fun baseTextFieldColors(
    isEmpty: Boolean,
    isPlaceholderEmpty: Boolean,
): TextFieldColors {
    val textColor = defaultTextColor(isEmpty = isEmpty, isPlaceholderEmpty = isPlaceholderEmpty)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor
    )
}

/**
 * Specifies the text color based on if the field is editable or a placeholder is being shown.
 */
@Composable
internal fun defaultTextColor(
    isEmpty: Boolean,
    isPlaceholderEmpty: Boolean,
): Color {
    val baseColor = MaterialTheme.colorScheme.onSurface
    return if ((isEmpty && !isPlaceholderEmpty)) {
        // if placeholder is visible, make it lighter than the actual input text color
        baseColor.copy(alpha = 0.6f)
    } else {
        // input text color
        baseColor
    }
}
