package com.arcgismaps.toolkit.scalebar.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shape styling properties for the Scalebar.
 *
 * @param shadowCornerRadius The corner radius of the shadow on the Scalebar.
 * @param barCornerRadius The corner radius of the scalebar of type bar and alternating bar.
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
 * @param fillColor The fill color used for bar and alternating bar Scalebar.
 * @param alternateFillColor The second color used for for bar and alternating bar Scalebar.
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
     * @param fillColor The fill color used for bar and alternating bar Scalebar.
     * @param alternateFillColor The second color used for for bar and alternating bar Scalebar.
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
     * @param barCornerRadius The corner radius of the scalebar of type bar and alternating bar.
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