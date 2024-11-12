/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.barcode

import android.graphics.Matrix
import android.graphics.RectF

/**
 * Adapted from:
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/camera/camera-core/src/main/java/androidx/camera/core/impl/utils/TransformUtils.java
 */
internal object TransformUtils {

    /**
     * Rotates the given [rect] by the given number of degrees [rotationDegrees].
     */
    fun rotateRect(rect: RectF, rotationDegrees: Int): RectF {
        val clampedRotation = (rotationDegrees % 360 + 360) % 360
        return if (clampedRotation == 90 || clampedRotation == 270) {
            RectF(0f, 0f, rect.height(), rect.width())
        } else {
            rect
        }
    }

    /**
     * Returns a transformation matrix that maps a source rectangle to a target rectangle
     * with the given rotation [rotationDegrees].
     */
    fun getRectToRect(
        source: RectF,
        target: RectF,
        rotationDegrees: Int
    ): Matrix {
        // Map source to normalized space.
        val matrix = Matrix()
        matrix.setRectToRect(source, RectF(-1f, -1f, 1f, 1f), Matrix.ScaleToFit.FILL)
        // Add rotation.
        matrix.postRotate(rotationDegrees.toFloat())
        // Restore the normalized space to target's coordinates.
        matrix.postConcat(getNormalizedToBuffer(target))
        return matrix
    }

    /**
     *  Returns a transformation matrix that maps a normalized rectangle to a target rectangle.
     */
    private fun getNormalizedToBuffer(viewPortRect: RectF): Matrix {
        val normalizedToBuffer = Matrix()
        normalizedToBuffer.setRectToRect(
            RectF(-1f, -1f, 1f, 1f),
            viewPortRect,
            Matrix.ScaleToFit.FILL
        )
        return normalizedToBuffer
    }
}
