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

package com.arcgismaps.toolkit.ar.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arcgismaps.toolkit.ar.WorldScaleCalibrationViewColorScheme
import com.arcgismaps.toolkit.ar.WorldScaleCalibrationViewTypography

internal object DefaultThemeTokens {
    val calibrationViewColorScheme = WorldScaleCalibrationViewColorScheme(
        backgroundColor = ColorTokens.Surface,
        containerColor = ColorTokens.SurfaceVariant,
        buttonContainerColor = ColorTokens.Primary,
        buttonContentColor = ColorTokens.OnPrimary,
        sliderTrackColor = ColorTokens.Primary,
        sliderThumbColor = ColorTokens.OnPrimary
    )

    val calibrationViewTypography = WorldScaleCalibrationViewTypography(
        titleTextStyle = TypographyTokens.bodyLarge,
        subtitleTextStyle = TypographyTokens.bodyMedium,
        bodyTextStyle = TypographyTokens.bodyMedium
    )
}

private object ColorTokens {
    val Background = PaletteTokens.Neutral99
    val Error = PaletteTokens.Error40
    val ErrorContainer = PaletteTokens.Error90
    val InverseOnSurface = PaletteTokens.Neutral95
    val InversePrimary = PaletteTokens.Primary80
    val InverseSurface = PaletteTokens.Neutral20
    val OnBackground = PaletteTokens.Neutral10
    val OnError = PaletteTokens.Error100
    val OnErrorContainer = PaletteTokens.Error10
    val OnPrimary = PaletteTokens.Primary100
    val OnPrimaryContainer = PaletteTokens.Primary10
    val OnSecondary = PaletteTokens.Secondary100
    val OnSecondaryContainer = PaletteTokens.Secondary10
    val OnSurface = PaletteTokens.Neutral10
    val OnSurfaceVariant = PaletteTokens.NeutralVariant30
    val OnTertiary = PaletteTokens.Tertiary100
    val OnTertiaryContainer = PaletteTokens.Tertiary10
    val Outline = PaletteTokens.NeutralVariant50
    val OutlineVariant = PaletteTokens.NeutralVariant80
    val Primary = PaletteTokens.Primary40
    val PrimaryContainer = PaletteTokens.Primary90
    val Scrim = PaletteTokens.Neutral0
    val Secondary = PaletteTokens.Secondary40
    val SecondaryContainer = PaletteTokens.Secondary90
    val Surface = PaletteTokens.Neutral99
    val SurfaceTint = Primary
    val SurfaceVariant = PaletteTokens.NeutralVariant90
    val Tertiary = PaletteTokens.Tertiary40
    val TertiaryContainer = PaletteTokens.Tertiary90
}

private object PaletteTokens {
    val Error10 = Color(red = 65, green = 14, blue = 11)
    val Error100 = Color(red = 255, green = 255, blue = 255)
    val Error40 = Color(red = 179, green = 38, blue = 30)
    val Error90 = Color(red = 249, green = 222, blue = 220)
    val Neutral0 = Color(red = 0, green = 0, blue = 0)
    val Neutral10 = Color(red = 28, green = 27, blue = 31)
    val Neutral20 = Color(red = 49, green = 48, blue = 51)
    val Neutral95 = Color(red = 244, green = 239, blue = 244)
    val Neutral99 = Color(red = 255, green = 251, blue = 254)
    val NeutralVariant30 = Color(red = 73, green = 69, blue = 79)
    val NeutralVariant50 = Color(red = 121, green = 116, blue = 126)
    val NeutralVariant80 = Color(red = 202, green = 196, blue = 208)
    val NeutralVariant90 = Color(red = 231, green = 224, blue = 236)
    val Primary10 = Color(red = 33, green = 0, blue = 93)
    val Primary100 = Color(red = 255, green = 255, blue = 255)
    val Primary40 = Color(red = 103, green = 80, blue = 164)
    val Primary80 = Color(red = 208, green = 188, blue = 255)
    val Primary90 = Color(red = 234, green = 221, blue = 255)
    val Secondary10 = Color(red = 29, green = 25, blue = 43)
    val Secondary100 = Color(red = 255, green = 255, blue = 255)
    val Secondary40 = Color(red = 98, green = 91, blue = 113)
    val Secondary90 = Color(red = 232, green = 222, blue = 248)
    val Tertiary10 = Color(red = 49, green = 17, blue = 29)
    val Tertiary100 = Color(red = 255, green = 255, blue = 255)
    val Tertiary40 = Color(red = 125, green = 82, blue = 96)
    val Tertiary90 = Color(red = 255, green = 216, blue = 228)
}


private val DefaultTextStyle =
    TextStyle.Default.copy(platformStyle = PlatformTextStyle(includeFontPadding = true))

private object TypographyTokens {
    val bodyLarge = DefaultTextStyle.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    val bodyMedium = DefaultTextStyle.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    )
    val bodySmall = DefaultTextStyle.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
}
