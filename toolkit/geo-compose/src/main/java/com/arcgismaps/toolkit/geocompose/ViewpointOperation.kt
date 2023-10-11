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

import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AnimationCurve
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith

/**
 * Represents the [ViewpointOperation] message to be sent to the channel.
 * The class captures the parameters of multiple viewpoint functions and provides operations
 * to await a result as well as to complete the operation once a result is available.
 *
 * @since 200.3.0
 */
internal sealed class ViewpointOperation {
    private val deferred = CompletableDeferred<Boolean>()

    class ViewpointAnimated(
        val viewpoint: Viewpoint,
        val durationSeconds: Float? = null,
        val curve: AnimationCurve? = null
    ) : ViewpointOperation()


    class ViewpointCenter(val center: Point, val scale: Double? = null) : ViewpointOperation()

    class ViewpointGeometry(val boundingGeometry: Geometry, val paddingInDips: Double? = null) :
        ViewpointOperation()

    class ViewpointRotation(val angleDegrees: Double) : ViewpointOperation()

    class ViewpointScale(val scale: Double) : ViewpointOperation()

    suspend fun await(): Result<Boolean> = runCatchingCancellable {
        deferred.await()
    }

    fun completeWith(result: Result<Boolean>) = deferred.completeWith(result)
}
