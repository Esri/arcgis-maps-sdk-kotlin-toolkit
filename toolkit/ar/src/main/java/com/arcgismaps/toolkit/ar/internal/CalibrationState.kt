package com.arcgismaps.toolkit.ar.internal

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue


/**
 * State holder class for calibration view
 *
 * @param onHeadingChange Lambda invoked when the user adjusts heading offset
 * @param onHeadingReset Lambda invoked when the user resets heading offset
 * @param onElevationChange Lambda invoked when the user adjusts elevation offset
 * @param onElevationReset Lambda invoked when the user resets elevation offset
 */

@Immutable
internal class CalibrationState(
    val onHeadingChange: (Float) -> Unit,
    val onElevationChange: (Float) -> Unit,
    val onHeadingReset: () -> Unit,
    val onElevationReset: () -> Unit,
) {
    var headingOffset by mutableFloatStateOf(0f)
    var elevationOffset by mutableFloatStateOf(0f)
}