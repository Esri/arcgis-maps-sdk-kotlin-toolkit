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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

internal object RadioButtonFieldDefaults {

    val textStyle : TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val labelTextStyle : TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val supportingTextStyle : TextStyle @Composable get() = MaterialTheme.typography.bodySmall

    @Composable
    fun colors(): RadioButtonFieldColors = RadioButtonFieldColors(
        errorColor = MaterialTheme.colorScheme.error,
        containerBorderColor = MaterialTheme.colorScheme.outline,
    )
}

/**
 * Color properties of a radio button field.
 *
 * @property errorColor The color used for the supporting text of this radio button field when the value is considered
 * invalid.
 * @property containerBorderColor The color used for the container border of this radio button field.
 */
internal data class RadioButtonFieldColors(
    val errorColor: Color,
    val containerBorderColor: Color,
)
