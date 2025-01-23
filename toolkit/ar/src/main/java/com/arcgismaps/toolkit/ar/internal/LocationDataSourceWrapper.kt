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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.location.LocationDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Wrapper class to manage the lifecycle of a [LocationDataSource].
 * The [LocationDataSource] will be started when the lifecycle owner is resumed and stopped when the
 * lifecycle owner is paused or destroyed.
 *
 * @since 200.7.0
 */
internal class LocationDataSourceWrapper(val locationDataSource: LocationDataSource) : DefaultLifecycleObserver {

    // This coroutine scope is tied to the lifecycle of this [LocationDataSourceWrapper]
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
            scope.cancel()
        }
        super.onDestroy(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
        }
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        scope.launch {
            locationDataSource.start()
        }
    }
}
