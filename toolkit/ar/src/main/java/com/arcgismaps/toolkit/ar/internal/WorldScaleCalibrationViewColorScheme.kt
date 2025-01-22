package com.arcgismaps.toolkit.ar.internal

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A class that holds the color scheme for the [WorldScaleCalibrationView].
 *
 * @property backgroundColor The background color of the [WorldScaleCalibrationView].
 * @property containerColor The color of the containers of the [WorldScaleCalibrationView], such as
 * the area for heading controls as well as elevation controls.
 * @property buttonContainerColor The color of the container for the buttons in the [WorldScaleCalibrationView].
 * @property buttonContentColor The color of the content of the buttons in the [WorldScaleCalibrationView].
 * @property sliderTrackColor The color of the track of the sliders in the [WorldScaleCalibrationView].
 * @property sliderThumbColor The color of the thumb of the sliders in the [WorldScaleCalibrationView].
 *
 * @since 200.7.0
 */
@Immutable
public data class WorldScaleCalibrationViewColorScheme internal constructor(
    val backgroundColor: Color,
    val containerColor: Color,
    val closeButtonContainerColor: Color,
    val closeButtonContentColor: Color,
    val buttonContainerColor: Color,
    val buttonContentColor: Color,
    val sliderTrackColor: Color,
    val sliderThumbColor: Color
)
