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

import com.arcgismaps.mapping.view.DoubleXY
import com.google.android.filament.Camera
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CameraNode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Maps a renderer-space viewing ray to top-left-origin equirectangular image pixels. */
internal fun panoramicImagePoint(direction: Float3, imageWidth: Int, imageHeight: Int): DoubleXY? {
    val x = direction.x.toDouble()
    val y = direction.y.toDouble()
    val z = direction.z.toDouble()
    if (imageWidth <= 0 || imageHeight <= 0 || !x.isFinite() || !y.isFinite() ||
        !z.isFinite() || (x == 0.0 && y == 0.0 && z == 0.0)
    ) return null
    val u = (0.5 + atan2(z, x) / (2.0 * Math.PI) + 1.0) % 1.0
    val v = atan2(hypot(x, z), y) / Math.PI
    return DoubleXY(u * imageWidth, v * imageHeight)
}

/** Inverse of the reflected sphere/image-material UV mapping; image pixels have a top-left origin. */
internal fun panoramicImageDirection(imageX: Double, imageY: Double, imageWidth: Int, imageHeight: Int): Float3? {
    if (imageWidth <= 0 || imageHeight <= 0 || !imageX.isFinite() || !imageY.isFinite() ||
        imageY < 0.0 || imageY > imageHeight
    ) return null
    val yaw = Math.toRadians(360.0 * (imageX / imageWidth - 0.5))
    // Avoid a singular orbit look-at at the exact poles while keeping the selected pixel visible.
    val pitch = Math.toRadians(180.0 * (1.0 - imageY / imageHeight)).coerceIn(0.0001, Math.PI - 0.0001)
    return Float3((sin(pitch) * cos(yaw)).toFloat(), (-cos(pitch)).toFloat(), (sin(pitch) * sin(yaw)).toFloat())
}

internal data class PanoramicFootprintOrientation(
    val eastInRenderer: Float3,
    val northInRenderer: Float3,
    val upInRenderer: Float3
) {
    // Output uses +X north, +Z east, +Y up, yielding geographic heading and inclination.
    fun toFootprintDirection(direction: Float3): Float3 {
        fun dot(axis: Float3): Float = direction.x * axis.x + direction.y * axis.y + direction.z * axis.z
        return Float3(dot(northInRenderer), dot(upInRenderer), dot(eastInRenderer))
    }
}

internal data class PanoramicFootprintUpdate(
    val yaw: Double,
    val pitch: Double,
    val horizontalFov: Double,
    val verticalFov: Double
)

internal fun CameraNode.toFootprintUpdate(orientation: PanoramicFootprintOrientation): PanoramicFootprintUpdate? =
    panoramicFootprintUpdate(
        orientation.toFootprintDirection(forwardDirection),
        camera.getFieldOfViewInDegrees(Camera.Fov.HORIZONTAL),
        camera.getFieldOfViewInDegrees(Camera.Fov.VERTICAL)
    )

internal fun panoramicFootprintUpdate(
    forwardDirection: Float3, horizontalFov: Double, verticalFov: Double
): PanoramicFootprintUpdate? {
    val x = forwardDirection.x.toDouble()
    val up = forwardDirection.y.toDouble()
    val z = forwardDirection.z.toDouble()
    if (!x.isFinite() || !up.isFinite() || !z.isFinite() ||
        !horizontalFov.isFinite() || horizontalFov <= 0.0 || horizontalFov >= 180.0 ||
        !verticalFov.isFinite() || verticalFov <= 0.0 || verticalFov >= 180.0
    ) return null
    val horizontalMagnitude = hypot(x, z)
    if (horizontalMagnitude == 0.0 && up == 0.0) return null
    // Heading is undefined at a pole; choose north deterministically.
    val yaw = if (horizontalMagnitude == 0.0) 0.0 else
        ((Math.toDegrees(atan2(z, x)) % 360.0) + 360.0) % 360.0
    val pitch = Math.toDegrees(atan2(horizontalMagnitude, -up))
    return PanoramicFootprintUpdate(yaw, pitch, horizontalFov, verticalFov)
}

internal fun PanoramicFootprintUpdate.isApproximatelyEqualTo(other: PanoramicFootprintUpdate?): Boolean {
    if (other == null) return false
    val yawDifference = abs((yaw - other.yaw + 540.0) % 360.0 - 180.0)
    return yawDifference <= FOOTPRINT_UPDATE_EPSILON_DEGREES &&
            abs(pitch - other.pitch) <= FOOTPRINT_UPDATE_EPSILON_DEGREES &&
            abs(horizontalFov - other.horizontalFov) <= FOOTPRINT_UPDATE_EPSILON_DEGREES &&
            abs(verticalFov - other.verticalFov) <= FOOTPRINT_UPDATE_EPSILON_DEGREES
}

private const val FOOTPRINT_UPDATE_EPSILON_DEGREES = 0.1
