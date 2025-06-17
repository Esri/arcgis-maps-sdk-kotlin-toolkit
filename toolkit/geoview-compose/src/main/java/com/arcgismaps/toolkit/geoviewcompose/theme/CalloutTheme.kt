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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

/**
 * Shape styling properties for the Callout container.
 *
 * @param cornerRadius The corner radius of the Callout container.
 * @param leaderSize The [DpSize] for the width and height of the leader.
 * @param borderWidth The width of the outline stroke around the Callout container.
 * @param calloutContentPadding Padding values used for the content inside the Callout container.
 * @param minSize The minimum size for the Callout container.
 *
 * @since 200.5.0
 */
@Immutable
@ExposedCopyVisibility
public data class CalloutShapes internal constructor(
    public val cornerRadius: Dp,
    public val leaderSize: DpSize,
    public val borderWidth: Dp,
    val calloutContentPadding: PaddingValues,
    val minSize: DpSize
)

/**
 * Color styling properties for the Callout container.
 *
 * @param backgroundColor The color used for the Callout container's background color.
 * @param borderColor The color used for the outline stroke around the Callout container.
 *
 * @since 200.5.0
 */
@Immutable
@ExposedCopyVisibility
public data class CalloutColors internal constructor(
    public val backgroundColor: Color,
    public val borderColor: Color
)

/**
 * Contains the default values used by Callout.
 *
 * @since 200.5.0
 */
public object CalloutDefaults {

    /**
     * Creates an instance of [CalloutColors] with default values from [MaterialTheme].
     *
     * @param backgroundColor The color used for the Callout container's background color.
     * @param borderColor The color used for the outline stroke around the Callout container.
     *
     * @since 200.5.0
     */
    @Composable
    public fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.background,
        borderColor: Color = MaterialTheme.colorScheme.outlineVariant
    ): CalloutColors {
        return CalloutColors(
            backgroundColor = backgroundColor,
            borderColor = borderColor
        )
    }

    /**
     * Creates an instance of [CalloutShapes] with default values.
     *
     * @param cornerRadius The corner radius of the Callout container.
     * @param borderWidth The width of the outline stroke around the Callout container.
     * @param leaderSize The [DpSize] for the width and height of the leader.
     * @param calloutContentPadding Padding values used for the content inside the Callout container.
     * @param minSize The minimum size for the Callout container.
     *
     * @since 200.5.0
     */
    @Composable
    public fun shapes(
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
    ): CalloutShapes {
        return CalloutShapes(
            cornerRadius = cornerRadius,
            leaderSize = leaderSize,
            borderWidth = borderWidth,
            calloutContentPadding = calloutContentPadding,
            minSize = minSize
        )
    }
}
