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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * State holder class for calibration view
 *
 * @since 200.7.0
 */
@Stable
internal class CalibrationState {
    private val _headingDeltas: MutableSharedFlow<Double> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val headingDeltas = _headingDeltas.asSharedFlow()

    private val _elevationDeltas: MutableSharedFlow<Double> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val elevationDeltas = _elevationDeltas.asSharedFlow()

    var totalHeadingOffset by mutableDoubleStateOf(0.0)
        private set
    var totalElevationOffset by mutableDoubleStateOf(0.0)
        private set

    fun onHeadingChange(value: Double) {
        _headingDeltas.tryEmit(value)
        totalHeadingOffset += value
    }

    fun onElevationChange(value: Double) {
        _elevationDeltas.tryEmit(value)
        totalElevationOffset += value

    }

    fun onHeadingReset() {
        _headingDeltas.tryEmit(-totalHeadingOffset)
        totalHeadingOffset = 0.0
    }

    fun onElevationReset() {
        _elevationDeltas.tryEmit(-totalElevationOffset)
        totalElevationOffset = 0.0
    }
}
