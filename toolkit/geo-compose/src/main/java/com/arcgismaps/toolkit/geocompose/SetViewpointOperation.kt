/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.Viewpoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith

/**
 * Represents the [SetViewpoint] message to be sent to the channel.
 * Change the Map to the new viewpoint. The viewpoint is updated instantaneously.
 *
 * @since 200.3.0
 */
internal sealed class SetViewpointOperation {

    private val deferred = CompletableDeferred<Unit>()

    data class SetViewpoint(val viewpoint: Viewpoint) : SetViewpointOperation()

    suspend fun await(): Result<Unit> = runCatchingCancellable {
        deferred.await()
    }

    fun completeWith(result: Result<Unit>) = deferred.completeWith(result)
}
