/*
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

package com.arcgismaps.toolkit.geoviewcompose.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

/**
 * CompositionLocal used to pass a [CalloutColorScheme] down the tree.
 */
internal val LocalColorScheme: ProvidableCompositionLocal<CalloutColorScheme> =
    compositionLocalOf {
        DefaultThemeTokens.colorScheme
    }

/**
 * Provides compose functions to access the current theme values.
 */
internal object CalloutTheme {

    /**
     * Retrieves the current [CalloutColorScheme].
     */
    val colorScheme: CalloutColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current
}

/**
 * Provides a default [CalloutTheme] to the given [content] so that the Callout can be
 * customized.
 *
 * The default value for the [colorScheme] is based on the current [MaterialTheme].
 * See [CalloutDefaults.colorScheme] for the exact configuration used.
 *
 * @param colorScheme A [CalloutColorScheme] to use for this compose hierarchy
 *
 * A complete definition for the [CalloutTheme] to use. A default is provided based
 * on the current [MaterialTheme].
 * @param content The content to which the theme should be applied.
 */
@Composable
internal fun CalloutTheme(
    colorScheme: CalloutColorScheme = CalloutDefaults.colorScheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalColorScheme provides colorScheme) {
        content()
    }
}


@Immutable
public data class CalloutColorScheme internal constructor(
    public val calloutShapeProperties: CalloutShapeProperties,
    public val calloutColorsProperties: CalloutColorsProperties
)

@Immutable
public data class CalloutShapeProperties internal constructor(
    public val cornerRadius: Dp,
    public val leaderSize: DpSize,
    public val borderWidth: Dp,
    val calloutContentPadding: PaddingValues,
    val minSize: DpSize
)


@Immutable
public data class CalloutColorsProperties internal constructor(
    public val backgroundColor: Color,
    public val borderColor: Color
)

/**
 * Contains the default values used by Callout.
 */
public object CalloutDefaults {

    /**
     * Creates a [CalloutColorScheme] with default values.

     * @since 200.5.0
     */
    @Composable
    public fun colorScheme(
        calloutColorsProperties: CalloutColorsProperties = colorProperties(),
        calloutShapeProperties: CalloutShapeProperties = shapeProperties()
    ): CalloutColorScheme {
        return CalloutColorScheme(
            calloutColorsProperties = calloutColorsProperties,
            calloutShapeProperties = calloutShapeProperties
        )
    }

    /**
     * Creates an instance of [CalloutColorsProperties] with default values from [MaterialTheme].

     * @since 200.5.0
     */
    @Composable
    public fun colorProperties(
        backgroundColor: Color = MaterialTheme.colorScheme.background,
        borderColor: Color = MaterialTheme.colorScheme.outlineVariant
    ): CalloutColorsProperties {
        return CalloutColorsProperties(
            backgroundColor = backgroundColor,
            borderColor = borderColor
        )
    }

    /**
     * Creates an instance of [CalloutColorsProperties] with default values from [MaterialTheme].

     * @since 200.5.0
     */
    @Composable
    public fun shapeProperties(
        cornerRadius: Dp = 10.dp,
        borderWidth: Dp = 2.dp,
        leaderSize: DpSize = DpSize(width = 12.dp, height = 10.dp),
        calloutContentPadding: PaddingValues = PaddingValues(
            all = cornerRadius + (borderWidth / 2)
        ),
        minSize: DpSize = DpSize(
            width = borderWidth + (2 * cornerRadius),
            height = borderWidth + (2 * cornerRadius)
        )
    ): CalloutShapeProperties {
        return CalloutShapeProperties(
            cornerRadius = cornerRadius,
            leaderSize = leaderSize,
            borderWidth = borderWidth,
            calloutContentPadding = calloutContentPadding,
            minSize = minSize
        )
    }
}
