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
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Provides an ARCore [Session] and manages the session's lifecycle.
 *
 * @since 200.6.0
 */
internal class ArSessionWrapper(private val applicationContext: Context, private val scope: CoroutineScope, useGeospatial: Boolean) :
    DefaultLifecycleObserver {

    private var session: Session? = null

    private val lock: Lock = ReentrantLock()

    private var shouldInitializeDisplay = true

    private var wasSetToUseGeospatial = useGeospatial

    override fun onDestroy(owner: LifecycleOwner) {
        withLock { session, _ ->
            this.session = null
            scope.launch(Dispatchers.Unconfined) {
                session?.close()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        withLock { session, _ ->
            session?.pause()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        // TODO: clean this up
        withLock { session, _ ->
            val sessionExists = session != null
            val newSession = session ?: Session(applicationContext)
            shouldInitializeDisplay = true
            newSession.resume()
            this.session = newSession
            if (!sessionExists) configureSession(wasSetToUseGeospatial)
        }
    }

    internal fun configureSession(useGeospatial: Boolean) {
        // TODO: add a callback if geospatial not supported and useGeospatial is true
       if (session?.isGeospatialModeSupported(Config.GeospatialMode.ENABLED) == false) return
        session?.configure(
            session?.config?.apply {
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (useGeospatial) {
                    geospatialMode = Config.GeospatialMode.ENABLED
                }
                // We only want to detect horizontal planes.
                setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL)
            }
        )
        this.wasSetToUseGeospatial = useGeospatial
    }

    internal fun withLock(block: (Session?, shouldInitializeDisplay: Boolean) -> Unit) {
        try {
            lock.lock()
            block(session, shouldInitializeDisplay)
        } finally {
            lock.unlock()
        }
    }

    fun resetSession(useGeospatial: Boolean = false) {
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
            configureSession(useGeospatial)
        }
    }
}

/**
 * Remembers an [ArSessionWrapper] that provides a configured ARCore [Session].
 *
 * @since 200.6.0
 */
@Composable
internal fun rememberArSessionWrapper(applicationContext: Context, useGeospatial: Boolean = false): ArSessionWrapper {
    val lifecycleOwner = LocalLifecycleOwner.current
    val arSessionWrapper = remember { ArSessionWrapper(applicationContext, lifecycleOwner.lifecycleScope, useGeospatial) }
    LaunchedEffect(useGeospatial) {
        arSessionWrapper.resetSession(useGeospatial)
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
