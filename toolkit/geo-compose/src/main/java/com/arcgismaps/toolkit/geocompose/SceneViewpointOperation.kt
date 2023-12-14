package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.view.SceneView
import kotlinx.coroutines.CompletableDeferred


/**
 * Defines operations for setting the viewpoint of a composable [SceneView].
 *
 * @since 200.4.0
 */
@Stable
public sealed class SceneViewpointOperation {
    private val deferred = CompletableDeferred<Result<Boolean>>()

    /**
     * Awaits the completion of this SceneViewpointOperation.
     *
     * @return a Result returning a boolean used to indicate if the operation completed successfully or not
     * @since 200.4.0
     */
    public suspend fun await(): Result<Boolean> = deferred.await()

    internal fun complete(result: Result<Boolean>) {
        deferred.complete(result)
    }
}

internal suspend fun SceneViewpointOperation.execute(sceneView: SceneView) {
    TODO()
}