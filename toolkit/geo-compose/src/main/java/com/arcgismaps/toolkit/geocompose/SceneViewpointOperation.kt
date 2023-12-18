/*
 *
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

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SceneView
import kotlinx.coroutines.CancellationException
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

    /**
     * Changes the scene view to the new viewpoint. The viewpoint is updated instantaneously.
     *
     * @property viewpoint the new viewpoint
     * @since 200.4.0
     */
    public class Set(public val viewpoint: Viewpoint) : SceneViewpointOperation()

    /**
     * Animates the scene view to the new viewpoint, taking the given number of seconds to complete the
     * navigation.
     *
     * @property viewpoint the new viewpoint
     * @property durationSeconds the duration of the animation in seconds
     * @since 200.4.0
     */
    public class Animate(
        public val viewpoint: Viewpoint,
        public val durationSeconds: Float = 0.25f
    ) : SceneViewpointOperation()

    /**
     * Updates the display to the viewpoint specified by the given camera.
     *
     * @property camera the new camera
     * @since 200.4.0
     */
    public class SetCamera(public val camera: Camera) : SceneViewpointOperation()

    /**
     * Animates the display to the viewpoint specified by the given camera using the specified duration
     * to arrive.
     *
     * @property camera the new camera
     * @property duration the duration of the animation in seconds
     * @since 200.4.0
     */
    public class AnimateCamera(
        public val camera: Camera,
        public val durationSeconds: Float = 0.25f
    ) : SceneViewpointOperation()

    /**
     * Animates the scene view's viewpoint to the viewpoint of the bookmark.
     *
     * @property bookmark bookmark to set
     * @since 200.4.0
     */
    public class SetBookmark(public val bookmark: Bookmark) : SceneViewpointOperation()
}

/**
 * Executes the [SceneViewpointOperation] on the given view-based [SceneView]. The operation can be awaited using
 * [SceneViewpointOperation.await]. If the coroutine on which the viewpoint operation is executed is cancelled,
 * for example due to another viewpoint operation being executed, the [SceneViewpointOperation.await] call
 * will return a Result with a boolean set to `false`, indicating that the viewpoint operation failed
 * to complete.
 *
 * @param sceneView the view-based SceneView to execute this operation on
 * @since 200.4.0
 */
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
