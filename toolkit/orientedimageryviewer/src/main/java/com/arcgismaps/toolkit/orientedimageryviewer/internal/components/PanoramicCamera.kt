/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import com.google.android.filament.Camera
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.node.CameraNode

/**
 * Returns a [CameraGestureDetector.CameraManipulator] that is suitable for panoramic cameras.
 *
 * @param cameraNode The camera node to manipulate.
 * @param initialDirection The initial direction of the camera, in world coordinates.
 */
internal fun panoramicCameraManipulator(
    cameraNode: CameraNode,
    initialDirection: Float3
): CameraGestureDetector.CameraManipulator {
    val rotation = object : CameraGestureDetector.DefaultCameraManipulator(
        orbitHomePosition = -initialDirection,
        targetPosition = Float3(0f, 0f, 0f)
    ) {
        override fun getTransform() = super.getTransform().apply { w = Float4(0f, 0f, 0f, 1f) }

        // A panorama camera must remain at the sphere center, including during two-finger gestures.
        override fun grabBegin(x: Int, y: Int, strafe: Boolean) { super.grabBegin(x, y, false) }
    }
    return object : CameraGestureDetector.CameraManipulator by rotation {
        private var verticalFov = 90.0
        private var aspect = 1.0

        private fun updateProjection() {
            cameraNode.setProjection(
                verticalFov, aspect, cameraNode.near.toDouble(), cameraNode.far.toDouble(), Camera.Fov.VERTICAL
            )
        }

        override fun setViewport(width: Int, height: Int) {
            rotation.setViewport(width, height)
            if (width > 0 && height > 0) {
                aspect = width.toDouble() / height
                updateProjection()
            }
        }

        override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
            if (prevSeparation > 0f && currSeparation > 0f && prevSeparation.isFinite() && currSeparation.isFinite()) {
                verticalFov = (verticalFov * prevSeparation / currSeparation).coerceIn(25.0, 100.0)
                updateProjection()
            }
        }

        override fun update(deltaTime: Float) {
            rotation.update(deltaTime)
            updateProjection()
        }
    }
}
