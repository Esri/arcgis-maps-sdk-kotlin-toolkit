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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Provides an ARCore [Session] and manages the session's lifecycle.
 *
 * @since 200.6.0
 */
internal class ArSessionWrapper(private val applicationContext: Context) :
    DefaultLifecycleObserver {

    private val _session = MutableStateFlow<Session?>(null)
    private val session: StateFlow<Session?> = _session.asStateFlow()

    private val mutex: Mutex = Mutex()

    private var shouldInitializeDisplay = true

    override fun onDestroy(owner: LifecycleOwner) {
        session.value?.close()
        _session.value = null
    }

    override fun onPause(owner: LifecycleOwner) {
        session.value?.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        val session = this.session.value ?: Session(applicationContext)
        configureSession()
        session.resume()
        _session.value = session
        shouldInitializeDisplay = true
    }

    private fun configureSession() {
        session.value?.configure(
            session.value?.config?.apply {
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
            block(session.value ?: return, shouldInitializeDisplay)
        } finally {
            mutex.unlock()
        }
    }

    internal fun acquireLock() {
        mutex.tryLock()
    }

    internal fun releaseLock() {
        mutex.unlock()
    }

    suspend fun resetSession(lifecycleOwner: LifecycleOwner) = withContext(Dispatchers.IO) {
        mutex.withLock {
            Log.e("ArSessionWrapper", "resetSession")
            session.value?.let {
                it.pause()
                it.close()
            }
            _session.value = null
            val session = Session(applicationContext)
            configureSession()
            session.resume()
            _session.value = session
            shouldInitializeDisplay = true
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
