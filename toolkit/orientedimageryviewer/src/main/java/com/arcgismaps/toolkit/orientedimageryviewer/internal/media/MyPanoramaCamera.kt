/*
 * Copyright 2026 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.media

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Image-space ray; independent of SceneView and geographic orientation. */
internal data class MyPanoramaRay(val x: Double, val y: Double, val z: Double) {
    fun dot(other: MyPanoramaRay): Double = x * other.x + y * other.y + z * other.z
}

/** Yaw zero faces -Z; positive pitch looks down. The camera never leaves the origin. */
internal data class MyPanoramaCamera(
    val yaw: Double = -90.0,
    val pitch: Double = 0.0,
    val verticalFov: Double = 90.0
) {
    val forward: MyPanoramaRay
        get() {
            val longitude = Math.toRadians(yaw + 270.0)
            val latitude = Math.toRadians(pitch)
            return MyPanoramaRay(cos(latitude) * cos(longitude), -sin(latitude), cos(latitude) * sin(longitude))
        }

    fun horizontalFov(aspect: Double): Double =
        Math.toDegrees(2.0 * atan(tan(Math.toRadians(verticalFov) / 2.0) * aspect))

    fun drag(dx: Double, dy: Double, viewportHeight: Int): MyPanoramaCamera {
        if (viewportHeight <= 0 || !dx.isFinite() || !dy.isFinite()) return this
        val degreesPerPixel = Math.toDegrees(2.0 * tan(Math.toRadians(verticalFov) / 2.0) / viewportHeight)
        return copy(
            yaw = wrap(yaw - dx * degreesPerPixel),
            pitch = (pitch - dy * degreesPerPixel).coerceIn(-89.9, 89.9)
        )
    }

    fun zoom(scale: Double): MyPanoramaCamera =
        if (scale.isFinite() && scale > 0.0) copy(verticalFov = (verticalFov / scale).coerceIn(25.0, 100.0)) else this

    companion object {
        fun fromPixel(x: Double, y: Double, width: Int, height: Int): MyPanoramaCamera? {
            if (width <= 0 || height <= 0 || !x.isFinite() || !y.isFinite() || y !in 0.0..height.toDouble()) return null
            return MyPanoramaCamera(wrap(360.0 * (x / width - 0.75)), (180.0 * (y / height - 0.5)).coerceIn(-89.9, 89.9))
        }

        // Unlike the camera, calibration rays may point exactly at a pole.
        fun imageRay(x: Double, y: Double, width: Int, height: Int): MyPanoramaRay? {
            if (width <= 0 || height <= 0 || !x.isFinite() || !y.isFinite() || y !in 0.0..height.toDouble()) return null
            val theta = 2.0 * Math.PI * x / width
            val phi = Math.PI * y / height
            return MyPanoramaRay(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta))
        }

        private fun wrap(value: Double): Double = ((value % 360.0) + 360.0) % 360.0
    }
}
