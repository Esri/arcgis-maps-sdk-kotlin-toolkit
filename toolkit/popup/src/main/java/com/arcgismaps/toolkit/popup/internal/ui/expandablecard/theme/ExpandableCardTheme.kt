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

package com.arcgismaps.toolkit.popup.internal.ui.expandablecard.theme

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
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.ExpandableCard

/**
 * CompositionLocal used to pass a [ExpandableCardColorScheme] down the tree.
 */
internal val LocalExpandableCardColorScheme: ProvidableCompositionLocal<ExpandableCardColorScheme> =
    compositionLocalOf {
        DefaultThemeTokens.colorScheme
    }

/**
 * CompositionLocal used to pass a [ExpandableCardShapes] down the tree.
 */
internal val LocalExpandableCardShapes: ProvidableCompositionLocal<ExpandableCardShapes> =
    compositionLocalOf {
        DefaultThemeTokens.shapes
    }

/**
 * CompositionLocal used to pass a [ExpandableCardShapes] down the tree.
 */
internal val LocalExpandableCardTypography: ProvidableCompositionLocal<ExpandableCardTypography> =
    compositionLocalOf {
        DefaultThemeTokens.typography
    }

/**
 * Provides compose functions to access the current theme values.
 */
@Suppress("unused")
internal object ExpandableCardTheme {

    /**
     * Retrieves the current [ExpandableCardColorScheme].
     */
    val colorScheme: ExpandableCardColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalExpandableCardColorScheme.current

    /**
     * Retrieves the current [ExpandableCardShapes].
     */
    val shapes: ExpandableCardShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalExpandableCardShapes.current

    /**
     * Retrieves the current [ExpandableCardTypography].
     */
    val typography: ExpandableCardTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalExpandableCardTypography.current
}

/**
 * Provides a default [ExpandableCardTheme] to the given [content] so that the ExpandableCard can be
 * customized.
 *
 * The default value for the [colorScheme], [shapes], and [typography] is based on the current
 * [MaterialTheme]. See [ExpandableCardDefaults.colorScheme], [ExpandableCardDefaults.shapes], and
 * [ExpandableCardDefaults.shapes] for the exact configuration used.
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
        LocalExpandableCardColorScheme provides colorScheme,
        LocalExpandableCardShapes provides shapes,
        LocalExpandableCardTypography provides typography
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
internal data class ExpandableCardColorScheme internal constructor(
    val headerTextColor: Color,
    val headerButtonTextColor: Color,
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
internal class ExpandableCardShapes internal constructor(
    val containerShape: RoundedCornerShape,
    val headerInternalPadding: Dp,
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
internal class ExpandableCardTypography internal constructor(
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle
)

