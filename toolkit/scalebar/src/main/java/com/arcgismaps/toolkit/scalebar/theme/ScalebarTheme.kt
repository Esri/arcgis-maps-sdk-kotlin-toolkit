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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.arcgismaps.toolkit.scalebar.ScalebarStyle

/**
 * Shape styling properties for the Scalebar.
 *
 * @param textShadowBlurRadius The text shadow blur radius, a value of 0f will result in no blur.
 * @param barCornerRadius The corner radius of the Scalebar of style [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar].
 * A value of 0f will result in a square corner.
 *
 * @since 200.7.0
 */
@Immutable
public data class ScalebarShapes internal constructor(
    public val textShadowBlurRadius: Float,
    public val barCornerRadius: Float,
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
 * Typography styling properties for the Scalebar.
 *
 * @param labelStyle The text style used for the Scalebar's text labels.
 *
 * @since 200.7.0
 */
@Immutable
public data class LabelTypography internal constructor(
    public val labelStyle: TextStyle
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
        fillColor: Color = Color.Gray,
        alternateFillColor: Color = Color.Black,
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
     * [textShadowBlurRadius] is set to 0f, meaning no shadow blur will be applied. [barCornerRadius] is set to 0f,
     * resulting in a square corner.
     * @param textShadowBlurRadius The blur radius on the shadow of all text.
     * @param barCornerRadius The corner radius of the Scalebar of style [ScalebarStyle.Bar] and [ScalebarStyle.AlternatingBar].
     *
     * @since 200.7.0
     */
    @Composable
    public fun shapes(
        textShadowBlurRadius: Float = 0f,
        barCornerRadius: Float = 0f
    ): ScalebarShapes {
        return ScalebarShapes(
            textShadowBlurRadius = textShadowBlurRadius,
            barCornerRadius = barCornerRadius
        )
    }

    /**
     * Creates an instance of [LabelTypography] with default values.
     *
     * @param labelStyle The text style used for the Scalebar's text labels.
     *
     * @since 200.7.0
     */
    @Composable
    public fun typography(
        labelStyle: TextStyle = MaterialTheme.typography.titleSmall
    ): LabelTypography {
        return LabelTypography(
            labelStyle = labelStyle
        )
    }
}
