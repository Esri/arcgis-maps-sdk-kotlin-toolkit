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
import androidx.compose.runtime.remember
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provides an ARCore [Session] and manages the session's lifecycle.
 *
 * @since 200.6.0
 */
internal class ArSessionWrapper(private val applicationContext: Context) :
    DefaultLifecycleObserver {

    private var session: Session? = null

    private val mutex: Mutex = Mutex()

    private var shouldInitializeDisplay = true

    override fun onDestroy(owner: LifecycleOwner) {
        session?.close()
        session = null
    }

    override fun onPause(owner: LifecycleOwner) {
        session?.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        val newSession = this.session ?: Session(applicationContext)
        configureSession()
        shouldInitializeDisplay = true
        newSession.resume()
        session = newSession
    }

    private fun configureSession() {
        session?.configure(
            session?.config?.apply {
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                // We only want to detect horizontal planes.
                setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL)
            }
        )
    }

    internal fun withLock(block: (Session, shouldInitializeDisplay: Boolean) -> Unit) {
        val locked = mutex.tryLock()
        if (!locked) return
        try {
            block(session ?: return, shouldInitializeDisplay)
        } finally {
            mutex.unlock()
        }
    }

    suspend fun resetSession() {
        mutex.withLock {
            session?.let {
                it.pause()
                it.close()
            }
            session = null
            val newSession = Session(applicationContext)
            configureSession()
            shouldInitializeDisplay = true
            newSession.resume()
            session = newSession
        }
    }
}

/**
 * Remembers an [ArSessionWrapper] that provides a configured ARCore [Session].
 *
 * @since 200.6.0
 */
@Composable
internal fun rememberArSessionWrapper(applicationContext: Context): ArSessionWrapper {
    val lifecycleOwner = LocalLifecycleOwner.current
    val arSessionWrapper = remember { ArSessionWrapper(applicationContext) }
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(arSessionWrapper)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(arSessionWrapper)
            arSessionWrapper.onDestroy(lifecycleOwner)
        }
    }
    return arSessionWrapper
}
