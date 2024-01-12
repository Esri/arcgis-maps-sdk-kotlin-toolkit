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

package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.components.codedvalue.RadioButtonFieldColors

internal object GroupElementDefaults {

    val borderThickness = 1.dp
    val containerShape = RoundedCornerShape(5.dp)

    @Composable
    fun colors() : GroupElementColors = GroupElementColors(
        containerColor = MaterialTheme.colorScheme.background,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}

internal data class GroupElementColors(
    val containerColor : Color,
    val borderColor : Color,
)