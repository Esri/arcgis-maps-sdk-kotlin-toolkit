/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Modifications copyright (C) 2023 Esri Inc
 */

package com.arcgismaps.toolkit.featureforms.components.datetime.picker.time

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

internal object TimePickerTokens {
    val ClockDialColor = ColorSchemeKeyTokens.SurfaceVariant
    val ClockDialContainerSize = 256.0.dp
    val ClockDialLabelTextFont = TypographyKeyTokens.BodyLarge
    val ClockDialSelectedLabelTextColor = ColorSchemeKeyTokens.OnPrimary
    val ClockDialSelectorCenterContainerColor = ColorSchemeKeyTokens.Primary
    val ClockDialSelectorCenterContainerShape = ShapeKeyTokens.CornerFull
    val ClockDialSelectorCenterContainerSize = 8.0.dp
    val ClockDialSelectorHandleContainerColor = ColorSchemeKeyTokens.Primary
    val ClockDialSelectorHandleContainerShape = ShapeKeyTokens.CornerFull
    val ClockDialSelectorHandleContainerSize = 48.0.dp
    val ClockDialSelectorTrackContainerColor = ColorSchemeKeyTokens.Primary
    val ClockDialSelectorTrackContainerWidth = 2.0.dp
    val ClockDialShape = ShapeKeyTokens.CornerFull
    val ClockDialUnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val ContainerColor = ColorSchemeKeyTokens.Surface
    val ContainerElevation = ElevationTokens.Level3
    val ContainerShape = ShapeKeyTokens.CornerExtraLarge
    val HeadlineColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val HeadlineFont = TypographyKeyTokens.LabelMedium
    val PeriodSelectorContainerShape = ShapeKeyTokens.CornerSmall
    val PeriodSelectorHorizontalContainerHeight = 38.0.dp
    val PeriodSelectorHorizontalContainerWidth = 216.0.dp
    val PeriodSelectorLabelTextFont = TypographyKeyTokens.TitleMedium
    val PeriodSelectorOutlineColor = ColorSchemeKeyTokens.Outline
    val PeriodSelectorOutlineWidth = 1.0.dp
    val PeriodSelectorSelectedContainerColor = ColorSchemeKeyTokens.TertiaryContainer
    val PeriodSelectorSelectedFocusLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorSelectedHoverLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorSelectedLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorSelectedPressedLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorUnselectedFocusLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorUnselectedHoverLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorUnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorUnselectedPressedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorVerticalContainerHeight = 80.0.dp
    val PeriodSelectorVerticalContainerWidth = 52.0.dp
    val SurfaceTintLayerColor = ColorSchemeKeyTokens.SurfaceTint
    val TimeSelector24HVerticalContainerWidth = 114.0.dp
    val TimeSelectorContainerHeight = 80.0.dp
    val TimeSelectorContainerShape = ShapeKeyTokens.CornerSmall
    val TimeSelectorContainerWidth = 96.0.dp
    val TimeSelectorLabelTextFont = TypographyKeyTokens.DisplayLarge
    val TimeSelectorSelectedContainerColor = ColorSchemeKeyTokens.PrimaryContainer
    val TimeSelectorSelectedFocusLabelTextColor = ColorSchemeKeyTokens.OnPrimaryContainer
    val TimeSelectorSelectedHoverLabelTextColor = ColorSchemeKeyTokens.OnPrimaryContainer
    val TimeSelectorSelectedLabelTextColor = ColorSchemeKeyTokens.OnPrimaryContainer
    val TimeSelectorSelectedPressedLabelTextColor = ColorSchemeKeyTokens.OnPrimaryContainer
    val TimeSelectorSeparatorColor = ColorSchemeKeyTokens.OnSurface
    val TimeSelectorSeparatorFont = TypographyKeyTokens.DisplayLarge
    val TimeSelectorUnselectedContainerColor = ColorSchemeKeyTokens.SurfaceVariant
    val TimeSelectorUnselectedFocusLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val TimeSelectorUnselectedHoverLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val TimeSelectorUnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val TimeSelectorUnselectedPressedLabelTextColor = ColorSchemeKeyTokens.OnSurface
}

internal object TimeInputTokens {
    val ContainerColor = ColorSchemeKeyTokens.Surface
    val ContainerElevation = ElevationTokens.Level3
    val ContainerShape = ShapeKeyTokens.CornerExtraLarge
    val HeadlineColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val HeadlineFont = TypographyKeyTokens.LabelMedium
    val PeriodSelectorContainerHeight = 72.0.dp
    val PeriodSelectorContainerShape = ShapeKeyTokens.CornerSmall
    val PeriodSelectorContainerWidth = 52.0.dp
    val PeriodSelectorLabelTextFont = TypographyKeyTokens.TitleMedium
    val PeriodSelectorOutlineColor = ColorSchemeKeyTokens.Outline
    val PeriodSelectorOutlineWidth = 1.0.dp
    val PeriodSelectorSelectedContainerColor = ColorSchemeKeyTokens.TertiaryContainer
    val PeriodSelectorSelectedFocusLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorSelectedHoverLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorSelectedLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorSelectedPressedLabelTextColor = ColorSchemeKeyTokens.OnTertiaryContainer
    val PeriodSelectorUnselectedFocusLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorUnselectedHoverLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorUnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val PeriodSelectorUnselectedPressedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val SurfaceTintLayerColor = ColorSchemeKeyTokens.SurfaceTint
    val TimeFieldContainerColor = ColorSchemeKeyTokens.SurfaceVariant
    val TimeFieldContainerHeight = 72.0.dp
    val TimeFieldContainerShape = ShapeKeyTokens.CornerSmall
    val TimeFieldContainerWidth = 96.0.dp
    val TimeFieldFocusContainerColor = ColorSchemeKeyTokens.PrimaryContainer
    val TimeFieldFocusLabelTextColor = ColorSchemeKeyTokens.OnPrimaryContainer
    val TimeFieldFocusOutlineColor = ColorSchemeKeyTokens.Primary
    val TimeFieldFocusOutlineWidth = 2.0.dp
    val TimeFieldHoverLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val TimeFieldLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val TimeFieldLabelTextFont = TypographyKeyTokens.DisplayMedium
    val TimeFieldSeparatorColor = ColorSchemeKeyTokens.OnSurface
    val TimeFieldSeparatorFont = TypographyKeyTokens.DisplayLarge
    val TimeFieldSupportingTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val TimeFieldSupportingTextFont = TypographyKeyTokens.BodySmall
}

internal enum class TypographyKeyTokens {
    BodyLarge,
    BodyMedium,
    BodySmall,
    DisplayLarge,
    DisplayMedium,
    DisplaySmall,
    HeadlineLarge,
    HeadlineMedium,
    HeadlineSmall,
    LabelLarge,
    LabelMedium,
    LabelSmall,
    TitleLarge,
    TitleMedium,
    TitleSmall,
}

internal enum class ColorSchemeKeyTokens {
    Background,
    Error,
    ErrorContainer,
    InverseOnSurface,
    InversePrimary,
    InverseSurface,
    OnBackground,
    OnError,
    OnErrorContainer,
    OnPrimary,
    OnPrimaryContainer,
    OnSecondary,
    OnSecondaryContainer,
    OnSurface,
    OnSurfaceVariant,
    OnTertiary,
    OnTertiaryContainer,
    Outline,
    OutlineVariant,
    Primary,
    PrimaryContainer,
    Scrim,
    Secondary,
    SecondaryContainer,
    Surface,
    SurfaceTint,
    SurfaceVariant,
    Tertiary,
    TertiaryContainer,
}

internal object ElevationTokens {
    val Level0 = 0.0.dp
    val Level1 = 1.0.dp
    val Level2 = 3.0.dp
    val Level3 = 6.0.dp
    val Level4 = 8.0.dp
    val Level5 = 12.0.dp
}

internal enum class ShapeKeyTokens {
    CornerExtraLarge,
    CornerExtraLargeTop,
    CornerExtraSmall,
    CornerExtraSmallTop,
    CornerFull,
    CornerLarge,
    CornerLargeEnd,
    CornerLargeTop,
    CornerMedium,
    CornerNone,
    CornerSmall,
}

/**
 * Helper function for component typography tokens.
 */
internal fun Typography.fromToken(value: TypographyKeyTokens): TextStyle {
    return when (value) {
        TypographyKeyTokens.DisplayLarge -> displayLarge
        TypographyKeyTokens.DisplayMedium -> displayMedium
        TypographyKeyTokens.DisplaySmall -> displaySmall
        TypographyKeyTokens.HeadlineLarge -> headlineLarge
        TypographyKeyTokens.HeadlineMedium -> headlineMedium
        TypographyKeyTokens.HeadlineSmall -> headlineSmall
        TypographyKeyTokens.TitleLarge -> titleLarge
        TypographyKeyTokens.TitleMedium -> titleMedium
        TypographyKeyTokens.TitleSmall -> titleSmall
        TypographyKeyTokens.BodyLarge -> bodyLarge
        TypographyKeyTokens.BodyMedium -> bodyMedium
        TypographyKeyTokens.BodySmall -> bodySmall
        TypographyKeyTokens.LabelLarge -> labelLarge
        TypographyKeyTokens.LabelMedium -> labelMedium
        TypographyKeyTokens.LabelSmall -> labelSmall
    }
}

/** Helper function for component shape tokens. Used to grab the top values of a shape parameter. */
internal fun CornerBasedShape.top(): CornerBasedShape {
    return copy(bottomStart = CornerSize(0.0.dp), bottomEnd = CornerSize(0.0.dp))
}

/**
 * Helper function for component shape tokens. Used to grab the bottom values of a shape parameter.
 */
internal fun CornerBasedShape.bottom(): CornerBasedShape {
    return copy(topStart = CornerSize(0.0.dp), topEnd = CornerSize(0.0.dp))
}

/** Helper function for component shape tokens. Used to grab the start values of a shape parameter. */
internal fun CornerBasedShape.start(): CornerBasedShape {
    return copy(topEnd = CornerSize(0.0.dp), bottomEnd = CornerSize(0.0.dp))
}

/** Helper function for component shape tokens. Used to grab the end values of a shape parameter. */
internal fun CornerBasedShape.end(): CornerBasedShape {
    return copy(topStart = CornerSize(0.0.dp), bottomStart = CornerSize(0.0.dp))
}

/**
 * Helper function for component shape tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.shapes.fromToken(FabPrimarySmallTokens.ContainerShape)``
 */
internal fun Shapes.fromToken(value: ShapeKeyTokens): Shape {
    return when (value) {
        ShapeKeyTokens.CornerExtraLarge -> extraLarge
        ShapeKeyTokens.CornerExtraLargeTop -> extraLarge.top()
        ShapeKeyTokens.CornerExtraSmall -> extraSmall
        ShapeKeyTokens.CornerExtraSmallTop -> extraSmall.top()
        ShapeKeyTokens.CornerFull -> CircleShape
        ShapeKeyTokens.CornerLarge -> large
        ShapeKeyTokens.CornerLargeEnd -> large.end()
        ShapeKeyTokens.CornerLargeTop -> large.top()
        ShapeKeyTokens.CornerMedium -> medium
        ShapeKeyTokens.CornerNone -> RectangleShape
        ShapeKeyTokens.CornerSmall -> small
    }
}

@Composable
@ReadOnlyComposable
internal fun ShapeKeyTokens.toShape(): Shape {
    return MaterialTheme.shapes.fromToken(this)
}

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.colorScheme.fromToken(ExtendedFabBranded.BrandedContainerColor)``
 */
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
        ColorSchemeKeyTokens.Background -> background
        ColorSchemeKeyTokens.Error -> error
        ColorSchemeKeyTokens.ErrorContainer -> errorContainer
        ColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
        ColorSchemeKeyTokens.InversePrimary -> inversePrimary
        ColorSchemeKeyTokens.InverseSurface -> inverseSurface
        ColorSchemeKeyTokens.OnBackground -> onBackground
        ColorSchemeKeyTokens.OnError -> onError
        ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ColorSchemeKeyTokens.OnSecondary -> onSecondary
        ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.SurfaceTint -> surfaceTint
        ColorSchemeKeyTokens.OnTertiary -> onTertiary
        ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
        ColorSchemeKeyTokens.Scrim -> scrim
        ColorSchemeKeyTokens.Secondary -> secondary
        ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
        ColorSchemeKeyTokens.Tertiary -> tertiary
        ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
    }
}

@ReadOnlyComposable
@Composable
internal fun ColorSchemeKeyTokens.toColor(): Color {
    return MaterialTheme.colorScheme.fromToken(this)
}

@Suppress("DEPRECATION")
internal fun copyAndSetFontPadding(
    style: TextStyle,
    includeFontPadding: Boolean
): TextStyle =
    style.copy(platformStyle = PlatformTextStyle(includeFontPadding = includeFontPadding))
