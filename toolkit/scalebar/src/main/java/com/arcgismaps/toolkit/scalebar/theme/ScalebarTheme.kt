/*
 *
 *  Copyright 2025 Esri
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
package com.arcgismaps.toolkit.scalebar.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.scalebar.ScalebarStyle

/**
 * Shape styling properties for the Scalebar.
 *
 * @param shadowCornerRadius The corner radius of the shadow on the Scalebar.
 * @param barCornerRadius The corner radius of the Scalebar of style [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar].
 *
 * @since 200.7.0
 */
@Immutable
public data class ScalebarShapes internal constructor(
    public val shadowCornerRadius: Dp,
    public val barCornerRadius: Dp,
)

/**
 * Color styling properties for the Scalebar.
 *
 * @param fillColor The fill color used for [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar] Scalebar.
 * @param alternateFillColor The second color used for for [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar] Scalebar.
 * @param lineColor The color used for the Scalebar's line color.
 * @param shadowColor The color used for the Scalebar's shadow color.
 * @param textColor The color used for the Scalebar's text labels.
 * @param textShadowColor The color used for the Scalebar label's shadow color.
 *
 * @since 200.7.0
 */
@Immutable
public data class ScalebarColors internal constructor(
    public var fillColor: Color,
    public var alternateFillColor: Color,
    public var lineColor: Color,
    public var shadowColor: Color,
    public var textColor: Color,
    public var textShadowColor: Color
)

/**
 * Contains the default values used by Scalebar.
 *
 * @since 200.7.0
 */
public object ScalebarDefaults {

    /**
     * Creates an instance of [ScalebarColors] with default values.
     *
     * @param fillColor The fill color used for [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar] Scalebar.
     * @param alternateFillColor The second color used for for [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar] Scalebar.
     * @param lineColor The color used for the Scalebar's line color.
     * @param shadowColor The color used for the Scalebar's shadow color.
     * @param textColor The color used for the Scalebar's text labels.
     * @param textShadowColor The color used for the Scalebar label's shadow color.
     *
     * @since 200.7.0
     */
    @Composable
    public fun colors(
        fillColor: Color = Color.Black,
        alternateFillColor: Color = Color.Gray,
        lineColor: Color = Color.White,
        shadowColor: Color = Color.Black.copy(alpha = 0.65f),
        textColor: Color = Color.Black,
        textShadowColor: Color = Color.White
    ): ScalebarColors {
        return ScalebarColors(
            fillColor = fillColor,
            alternateFillColor = alternateFillColor,
            lineColor = lineColor,
            shadowColor = shadowColor,
            textColor = textColor,
            textShadowColor = textShadowColor
        )
    }

    /**
     * Creates an instance of [ScalebarShapes] with default values.
     *
     * @param shadowCornerRadius The corner radius of the shadow on the Scalebar.
     * @param barCornerRadius The corner radius of the Scalebar of style [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar].
     *
     * @since 200.7.0
     */
    @Composable
    public fun shapes(
        shadowCornerRadius: Dp = 10.dp,
        barCornerRadius: Dp = 10.dp,
    ): ScalebarShapes {
        return ScalebarShapes(
            shadowCornerRadius = shadowCornerRadius,
            barCornerRadius = barCornerRadius
        )
    }
}
