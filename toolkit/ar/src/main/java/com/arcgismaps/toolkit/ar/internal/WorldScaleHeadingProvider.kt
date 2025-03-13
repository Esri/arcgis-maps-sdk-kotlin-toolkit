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

import android.content.Context
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executor

/**
 * Provides headings for [WorldScaleNmeaLocationProvider] using the Fused Orientation Provider API.
 *
 * @param context the context the app is running in
 *
 * @since 200.7.0
 */
internal class WorldScaleHeadingProvider(context: Context) {

    private val fusedOrientationProviderClient = LocationServices.getFusedOrientationProviderClient(context)
    private val listener = DeviceOrientationListener { deviceOrientation ->
        _headings.tryEmit(deviceOrientation.headingDegrees)
    }

    private val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build()
    private val executor: Executor = Dispatchers.IO.asExecutor()

    private val _headings = MutableSharedFlow<Float>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val headings = _headings.asSharedFlow()

    fun start(){
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
    }

    fun stop(){
        fusedOrientationProviderClient.removeOrientationUpdates(listener)
    }
}
