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

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
internal fun baseTextFieldColors(
    isEditable: Boolean,
    isEmpty: Boolean,
    isPlaceholderEmpty: Boolean,
    interactionSource: InteractionSource
): TextFieldColors {
    val focused by interactionSource.collectIsFocusedAsState()
    val textColor = BaseTextFieldColors.textColor(
        isEditable = isEditable,
        isEmpty = isEmpty,
        isPlaceHolderEmpty = isPlaceholderEmpty,
        focused
    )

    val borderColor = BaseTextFieldColors.borderColor(
        isEditable = isEditable
    )
    val labelColor = BaseTextFieldColors.labelColor(
        isEditable = isEditable
    )
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (focused) textColor else textColor.copy(0.5f),
        unfocusedTextColor = textColor.copy(0.5f),
        focusedBorderColor = borderColor,
        focusedLabelColor = labelColor
    )
}

/**
 * Color properties of a base text field.
 */
internal object BaseTextFieldColors {
    @Composable
    fun borderColor(isEditable: Boolean) =
        if (isEditable)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)

    @Composable
    fun labelColor(isEditable: Boolean) =
        if (isEditable)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)

    @Composable
    fun textColor(isEditable: Boolean, isEmpty: Boolean, isPlaceHolderEmpty: Boolean, isFocused: Boolean): Color {
        val color = if (isEmpty && !isPlaceHolderEmpty) {
            Color.Gray
        } else {
            MaterialTheme.colorScheme.secondary
        }

        return if (isEditable) color else color.copy(alpha = 0.6f)
    }
}
