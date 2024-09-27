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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Session

/**
 * Provides an ARCore [Session] and manages the session's lifecycle.
 *
 * @since 200.6.0
 */
internal class ArSessionWrapper(applicationContext: Context) : DefaultLifecycleObserver {
    val session: Session = Session(applicationContext)

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        session.close()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        session.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        session.resume()
    }
}
