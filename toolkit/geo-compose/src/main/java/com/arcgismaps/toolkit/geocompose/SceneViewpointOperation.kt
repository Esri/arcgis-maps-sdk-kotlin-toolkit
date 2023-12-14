package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SceneView
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.cancellation.CancellationException


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

    public class Set(public val viewpoint: Viewpoint) : SceneViewpointOperation()
    public class Animate(
        public val viewpoint: Viewpoint,
        public val durationSeconds: Float = 0.25f
    ) : SceneViewpointOperation()
    public class SetCamera(public val camera: Camera) : SceneViewpointOperation()
    public class AnimateCamera(
        public val camera: Camera,
        public val durationSeconds: Float = 0.25f
    ) : SceneViewpointOperation()
    public class SetBookmark(public val bookmark: Bookmark) : SceneViewpointOperation()
}

internal suspend fun SceneViewpointOperation.execute(sceneView: SceneView) {
    when (this) {
        is SceneViewpointOperation.Set -> {
            sceneView.setViewpoint(this.viewpoint)
            this.complete(Result.success(true))
        }
        is SceneViewpointOperation.Animate -> {
            try {
                val result = sceneView.setViewpointAnimated(this.viewpoint, this.durationSeconds)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
        is SceneViewpointOperation.AnimateCamera -> {
            try {
                val result = sceneView.setViewpointCameraAnimated(this.camera, this.durationSeconds)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
        is SceneViewpointOperation.SetBookmark -> {
            try {
                val result = sceneView.setBookmark(this.bookmark)
                this.complete(result)
            } catch (e: CancellationException) {
                this.complete(Result.success(false))
                throw e
            }
        }
        is SceneViewpointOperation.SetCamera -> {
            sceneView.setViewpointCamera(this.camera)
            this.complete(Result.success(true))
        }
    }
}