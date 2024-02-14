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

package com.arcgismaps.toolkit.featureforms.components.datetime.picker.date

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.material3.ColorSchemeKeyTokens
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.material3.ElevationTokens
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.material3.ShapeKeyTokens
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.material3.TypographyKeyTokens

internal object DatePickerModalTokens {
    val ContainerColor = ColorSchemeKeyTokens.Surface
    val ContainerElevation = ElevationTokens.Level3
    val ContainerHeight = 568.0.dp
    val ContainerShape = ShapeKeyTokens.CornerExtraLarge
    val ContainerSurfaceTintLayerColor = ColorSchemeKeyTokens.SurfaceTint
    val ContainerWidth = 360.0.dp
    val DateContainerHeight = 40.0.dp
    val DateContainerShape = ShapeKeyTokens.CornerFull
    val DateContainerWidth = 40.0.dp
    val DateLabelTextFont = TypographyKeyTokens.BodyLarge
    val DateSelectedContainerColor = ColorSchemeKeyTokens.Primary
    val DateSelectedLabelTextColor = ColorSchemeKeyTokens.OnPrimary
    val DateStateLayerHeight = 40.0.dp
    val DateStateLayerShape = ShapeKeyTokens.CornerFull
    val DateStateLayerWidth = 40.0.dp
    val DateTodayContainerOutlineColor = ColorSchemeKeyTokens.Primary
    val DateTodayContainerOutlineWidth = 1.0.dp
    val DateTodayLabelTextColor = ColorSchemeKeyTokens.Primary
    val DateUnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val HeaderContainerHeight = 120.0.dp
    val HeaderContainerWidth = 360.0.dp
    val HeaderHeadlineColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val HeaderHeadlineFont = TypographyKeyTokens.HeadlineLarge
    val HeaderSupportingTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val HeaderSupportingTextFont = TypographyKeyTokens.LabelLarge
    val RangeSelectionActiveIndicatorContainerColor = ColorSchemeKeyTokens.SecondaryContainer
    val RangeSelectionActiveIndicatorContainerHeight = 40.0.dp
    val RangeSelectionActiveIndicatorContainerShape = ShapeKeyTokens.CornerFull
    val RangeSelectionContainerElevation = ElevationTokens.Level0
    val RangeSelectionContainerShape = ShapeKeyTokens.CornerNone
    val SelectionDateInRangeLabelTextColor = ColorSchemeKeyTokens.OnSecondaryContainer
    val RangeSelectionHeaderContainerHeight = 128.0.dp
    val RangeSelectionHeaderHeadlineFont = TypographyKeyTokens.TitleLarge
    val RangeSelectionMonthSubheadColor = ColorSchemeKeyTokens.OnSurfaceVariant
    val RangeSelectionMonthSubheadFont = TypographyKeyTokens.TitleSmall
    val WeekdaysLabelTextColor = ColorSchemeKeyTokens.OnSurface
    val WeekdaysLabelTextFont = TypographyKeyTokens.BodyLarge
    val SelectionYearContainerHeight = 36.0.dp
    val SelectionYearContainerWidth = 72.0.dp
    val SelectionYearLabelTextFont = TypographyKeyTokens.BodyLarge
    val SelectionYearSelectedContainerColor = ColorSchemeKeyTokens.Primary
    val SelectionYearSelectedLabelTextColor = ColorSchemeKeyTokens.OnPrimary
    val SelectionYearStateLayerHeight = 36.0.dp
    val SelectionYearStateLayerShape = ShapeKeyTokens.CornerFull
    val SelectionYearStateLayerWidth = 72.0.dp
    val SelectionYearUnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
}

internal object MotionTokens {
    const val DurationExtraLong1 = 700.0
    const val DurationExtraLong2 = 800.0
    const val DurationExtraLong3 = 900.0
    const val DurationExtraLong4 = 1000.0
    const val DurationLong1 = 450.0
    const val DurationLong2 = 500.0
    const val DurationLong3 = 550.0
    const val DurationLong4 = 600.0
    const val DurationMedium1 = 250.0
    const val DurationMedium2 = 300.0
    const val DurationMedium3 = 350.0
    const val DurationMedium4 = 400.0
    const val DurationShort1 = 50.0
    const val DurationShort2 = 100.0
    const val DurationShort3 = 150.0
    const val DurationShort4 = 200.0
    val EasingEmphasizedCubicBezier = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingEmphasizedAccelerateCubicBezier = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EasingEmphasizedDecelerateCubicBezier = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingLegacyCubicBezier = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EasingLegacyAccelerateCubicBezier = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val EasingLegacyDecelerateCubicBezier = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EasingLinearCubicBezier = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)
    val EasingStandardCubicBezier = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingStandardAccelerateCubicBezier = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
    val EasingStandardDecelerateCubicBezier = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
}
