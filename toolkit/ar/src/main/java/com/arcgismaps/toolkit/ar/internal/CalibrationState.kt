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