/*
 *
 *  Copyright 2024 Esri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Session
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Provides an ARCore [Session] and manages the session's lifecycle.
 *
 * @since 200.6.0
 */
internal class ArSessionWrapper(
    private val applicationContext: Context,
    private val onError: (Throwable) -> Unit,
    private var useGeospatial: Boolean,
    private val planeFindingMode: PlaneFindingMode = PlaneFindingMode.DISABLED
) :
    DefaultLifecycleObserver {

    private var session: Session? = null

    private val lock: Lock = ReentrantLock()

    private var shouldInitializeDisplay = true

    override fun onDestroy(owner: LifecycleOwner) {
        withLock { session, _ ->
            this.session = null
            session?.close()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        withLock { session, _ ->
            session?.pause()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        withLock { session, _ ->
            val newSession = session ?: Session(applicationContext).also {
                this.session = it
                configureSession(useGeospatial, planeFindingMode)
            }
            shouldInitializeDisplay = true
            newSession.resume()
        }
    }

    private fun configureSession(useGeospatial: Boolean, planeFindingMode: PlaneFindingMode) {
        session?.configure(
            session?.config?.apply {
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (useGeospatial) {
                    if (session?.isGeospatialModeSupported(Config.GeospatialMode.ENABLED) == false) {
                        onError(IllegalStateException("Geospatial mode is not supported on this device."))
                        return
                    }
                    geospatialMode = Config.GeospatialMode.ENABLED
                }
                if (planeFindingMode != PlaneFindingMode.DISABLED) {
                    if (session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true) {
                        depthMode = Config.DepthMode.AUTOMATIC
                    }
                }
                // We only want to detect horizontal planes.
                setPlaneFindingMode(planeFindingMode)
            }
        )
        this.useGeospatial = useGeospatial
    }

    internal fun withLock(block: (Session?, shouldInitializeDisplay: Boolean) -> Unit) {
        try {
            lock.lock()
            block(session, shouldInitializeDisplay)
        } finally {
            lock.unlock()
        }
    }

    fun resetSession(useGeospatial: Boolean = false, planeFindingMode: PlaneFindingMode) {
        withLock { session, _ ->
            session?.let {
                it.pause()
                it.close()
            }
            this.session = null
            val newSession = Session(applicationContext)
            shouldInitializeDisplay = true
            newSession.resume()
            this.session = newSession
            configureSession(useGeospatial, planeFindingMode)
        }
    }
}

/**
 * Remembers an [ArSessionWrapper] that provides a configured ARCore [Session].
 *
 * @since 200.6.0
 */
@Composable
internal fun rememberArSessionWrapper(
    applicationContext: Context,
    onError: (Throwable) -> Unit = {},
    useGeospatial: Boolean = false,
    planeFindingMode: Config.PlaneFindingMode
): ArSessionWrapper {
    val lifecycleOwner = LocalLifecycleOwner.current
    val arSessionWrapper = remember {
        ArSessionWrapper(
            applicationContext,
            onError,
            useGeospatial
        )
    }
    LaunchedEffect(useGeospatial, planeFindingMode) {
        arSessionWrapper.resetSession(useGeospatial, planeFindingMode)
    }
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(arSessionWrapper)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(arSessionWrapper)
            arSessionWrapper.onDestroy(lifecycleOwner)
        }
    }
    return arSessionWrapper
}
