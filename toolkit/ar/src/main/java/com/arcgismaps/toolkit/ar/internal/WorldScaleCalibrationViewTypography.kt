package com.arcgismaps.toolkit.ar.internal

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

/**
 * A class that holds the typography for the [WorldScaleCalibrationView].
 *
 * @property titleTextStyle The text style for the title of the [WorldScaleCalibrationView].
 * @property subtitleTextStyle The text style for any subtitles of the [WorldScaleCalibrationView].
 * @property bodyTextStyle The text style for any body text of the [WorldScaleCalibrationView].
 *
 * @since 200.7.0
 */
@Immutable
public data class WorldScaleCalibrationViewTypography internal constructor(
    val titleTextStyle: TextStyle,
    val subtitleTextStyle: TextStyle,
    val bodyTextStyle: TextStyle
)
