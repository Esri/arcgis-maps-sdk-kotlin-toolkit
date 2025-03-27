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
import com.arcgismaps.mapping.view.CameraController
import com.google.ar.core.Frame
import com.google.ar.core.Session

/**
 * Provides a common interface for classes that update the camera's position in world scale AR.
 *
 * @since 200.7.0
 */
internal interface WorldScaleCameraController : DefaultLifecycleObserver {
    /**
     * The [CameraController] that will be passed to a scene view.
     *
     * @since 200.7.0
     */
    val cameraController: CameraController

    /**
     * Whether the origin camera has been set, used to determine if the an initial location has been
     * established.
     *
     * @since 200.7.0
     */
    val hasSetOriginCamera: Boolean

    /**
     * Called every frame to update the camera's position.
     *
     * @since 200.7.0
     */
    fun updateCamera(frame: Frame, session: Session)
}
