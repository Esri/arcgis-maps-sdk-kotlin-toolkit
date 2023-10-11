package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith

/**
 * Represents the [GetCurrentViewpoint] message to be sent to the channel.
 *
 * @since 200.3.0
 */
internal sealed class GetCurrentViewpointOperation {
    private val deferred = CompletableDeferred<Viewpoint>()

    data class GetCurrentViewpoint(val viewpointType: ViewpointType) : GetCurrentViewpointOperation()

    suspend fun await(): Result<Viewpoint> = runCatchingCancellable {
        deferred.await()
    }

    fun completeWith(result: Result<Viewpoint>) = deferred.completeWith(result)
}
