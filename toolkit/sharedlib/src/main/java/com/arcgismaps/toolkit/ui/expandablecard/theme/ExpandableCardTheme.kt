/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.ui.expandablecard.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard

/**
 * CompositionLocal used to pass a [ExpandableCardColorScheme] down the tree.
 */
internal val LocalColorScheme: ProvidableCompositionLocal<ExpandableCardColorScheme> =
    compositionLocalOf {
        DefaultThemeTokens.colorScheme
    }

/**
 * CompositionLocal used to pass a [ExpandableCardShapes] down the tree.
 */
internal val LocalShapes: ProvidableCompositionLocal<ExpandableCardShapes> =
    compositionLocalOf {
        DefaultThemeTokens.shapes
    }

/**
 * CompositionLocal used to pass a [ExpandableCardShapes] down the tree.
 */
internal val LocalTypography: ProvidableCompositionLocal<ExpandableCardTypography> =
    compositionLocalOf {
        DefaultThemeTokens.typography
    }

/**
 * Provides compose functions to access the current theme values.
 */
internal object ExpandableCardTheme {

    /**
     * Retrieves the current [ExpandableCardColorScheme].
     */
    val colorScheme: ExpandableCardColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current

    /**
     * Retrieves the current [ExpandableCardShapes].
     */
    val shapes: ExpandableCardShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current

    /**
     * Retrieves the current [ExpandableCardTypography].
     */
    val typography: ExpandableCardTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}

/**
 * Provides a default [ExpandableCardTheme] to the given [content] so that the ExpandableCard can be
 * customized.
 *
 * The default value for the [colorScheme] and [shapes] is based on the current [MaterialTheme].
 * See [ExpandableCardDefaults.colorScheme], and [ExpandableCardDefaults.shapes] for the
 * exact configuration used.
 *
 * @param colorScheme An [ExpandableCardColorScheme] to use for this compose hierarchy.
 * Default is [ExpandableCardDefaults.colorScheme].
 * @param shapes An [ExpandableCardShapes] to use for this compose hierarchy.
 * Default is [ExpandableCardDefaults.shapes].
 * @param typography An [ExpandableCardTypography] to use for this compose hierarchy.
 * Default is [ExpandableCardDefaults.typography]
 * @param content The content to which the theme should be applied.
 */
@Suppress("unused")
@Composable
internal fun ExpandableCardTheme(
    colorScheme: ExpandableCardColorScheme = ExpandableCardDefaults.colorScheme(),
    shapes: ExpandableCardShapes = ExpandableCardDefaults.shapes(),
    typography: ExpandableCardTypography = ExpandableCardDefaults.typography(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalShapes provides shapes,
        LocalTypography provides typography
    ) {
        content()
    }
}

/**
 * A color scheme that holds all the color parameters for a [ExpandableCard].
 *
 * The scheme provides default values for all colors as a starting point for customization. These
 * defaults are populated using [MaterialTheme].
 *
 * Use [ExpandableCardDefaults.colorScheme] to create a new instance with the default values.
 *
 * @property headerTextColor The color scheme for the ExpandableCard Header
 * @property containerColor The color scheme for the ExpandableCard Body
 * @since 200.6.0
 */
@Immutable
data class ExpandableCardColorScheme internal constructor(
    val headerTextColor: Color,
    val headerBackgroundColor: Color,
    val containerColor: Color,
    val borderColor: Color
)

/**
 * A Shapes specification for the [ExpandableCard] built on top of [MaterialTheme].
 *
 * Use [ExpandableCardDefaults.shapes] to create a new instance with the default values.
 *
 * @since 200.6.0
 */
@Immutable
class ExpandableCardShapes internal constructor(
    val containerShape: RoundedCornerShape,
    val borderThickness: Dp
)

/**
 * A typography specification for the [ExpandableCard] built on top of [MaterialTheme].
 *
 * Use [ExpandableCardDefaults.typography] to create a new instance with the default values.
 *
 * @since 200.6.0
 */
@Immutable
class ExpandableCardTypography internal constructor(
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle
)
