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

package com.arcgismaps.toolkit.featureforms.components.codedvalue

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object RadioButtonFieldDefaults {

    private const val textDisabledAlpha = 0.38f
    private const val containerDisabledAlpha = 0.12f

    @Composable
    fun colors(): RadioButtonFieldColors = RadioButtonFieldColors(
        defaultLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(
            alpha = textDisabledAlpha
        ),
        defaultSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = textDisabledAlpha
        ),
        errorColor = MaterialTheme.colorScheme.error,
        defaultContainerBorderColor = MaterialTheme.colorScheme.outline,
        disabledContainerBorderColor = MaterialTheme.colorScheme.outline.copy(
            alpha = containerDisabledAlpha
        ),
        defaultTextColor = LocalContentColor.current,
        disabledTextColor = LocalContentColor.current.copy(alpha = textDisabledAlpha)
    )
}

internal data class RadioButtonFieldColors(
    val defaultLabelColor: Color,
    val disabledLabelColor: Color,
    val defaultSupportingTextColor: Color,
    val disabledSupportingTextColor: Color,
    val errorColor: Color,
    val defaultContainerBorderColor: Color,
    val disabledContainerBorderColor: Color,
    val defaultTextColor: Color,
    val disabledTextColor: Color
) {
    /**
     * Represents the color used for the label of this radio button field.
     *
     * @param enabled whether the field is enabled
     */
    @Composable
    fun labelColor(enabled: Boolean): Color {
        return if (enabled) {
            defaultLabelColor
        } else {
            disabledLabelColor
        }
    }

    /**
     * Represents the color used for the supporting text of this radio button field.
     *
     * @param enabled whether the field is enabled
     */
    @Composable
    fun supportingTextColor(enabled: Boolean): Color {
        return if (enabled) {
            defaultSupportingTextColor
        } else {
            disabledSupportingTextColor
        }
    }

    /**
     * Represents the color used for the container border of this radio button field.
     *
     * @param enabled whether the field is enabled
     */
    @Composable
    fun containerBorderColor(enabled: Boolean): Color {
        return if (enabled) {
            defaultContainerBorderColor
        } else {
            disabledContainerBorderColor
        }
    }

    /**
     * Represents the color used for the text of this radio button field options.
     *
     * @param enabled whether the field is enabled
     */
    @Composable
    fun textColor(enabled: Boolean): Color {
        return if (enabled) {
            defaultTextColor
        } else {
            disabledTextColor
        }
    }
}
