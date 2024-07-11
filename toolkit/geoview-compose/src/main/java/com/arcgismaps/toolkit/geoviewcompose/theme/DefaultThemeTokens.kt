/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.geoviewcompose.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.arcgismaps.toolkit.geoviewcompose.theme.DefaultThemeTokens.colorScheme

/**
 * Provides a default [CalloutColors] and [CalloutShapes] via [colorScheme] and [shapes].
 *
 * Creates a default theme from statically defined tokens. Do not use this at runtime, instead the
 * CalloutColorScheme must always be specified from the current MaterialTheme.
 *
 */
internal object DefaultThemeTokens {

    val colorScheme: CalloutColors = CalloutColors(
        backgroundColor = CalloutTokens.Background,
        borderColor = CalloutTokens.Outline
    )

    val shapes: CalloutShapes = CalloutShapes(
        cornerRadius = CalloutTokens.CornerRadius,
        borderWidth = CalloutTokens.OutlineWidth,
        leaderSize = CalloutTokens.LeaderSize,
        calloutContentPadding = CalloutTokens.CalloutContentPadding,
        minSize = CalloutTokens.MinContentSize
    )
}

private object CalloutTokens {
    val Background = Color.White
    val Outline = Color.LightGray
    val CornerRadius = 10.dp
    val OutlineWidth = 2.dp
    val LeaderSize = DpSize(width = 12.dp, height = 10.dp)
    val CalloutContentPadding = PaddingValues(all = 10.dp + (2.dp / 2))
    val MinContentSize = DpSize(width = 2.dp + (2 * 10.dp), height = 2.dp + (2 * 10.dp))
}
