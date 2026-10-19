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

import dev.romainguy.kotlin.math.Float3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class PanoramicFootprintUpdateTests {
    @Test
    fun `yaw is relative to image center rather than renderer north`() {
        val cases = listOf(
            Float3(1f, 0f, 0f) to 0.0,
            Float3(0f, 0f, 1f) to 90.0,
            Float3(-1f, 0f, 0f) to 180.0,
            Float3(0f, 0f, -1f) to 270.0
        )
        cases.forEach { (direction, expectedYaw) ->
            val update = requireNotNull(panoramicFootprintUpdate(direction, 75.0, 60.0))
            assertEquals(expectedYaw, update.yaw, 0.0001)
            assertEquals(90.0, update.pitch, 0.0001)
            assertEquals(75.0, update.horizontalFov, 0.0)
            assertEquals(60.0, update.verticalFov, 0.0)
        }
    }

    @Test
    fun `footprint angles match image coordinates across reflected sphere and seam`() {
        for (u in listOf(0.0, 0.01, 0.25, 0.49, 0.5, 0.51, 0.75, 0.99, 1.0)) {
            for (pitch in listOf(15.0, 45.0, 90.0, 135.0, 165.0)) {
                // SceneView's sphere uses U = 1 - theta/(2*PI), V = 1 - phi/PI.
                val theta = 2.0 * PI * (1.0 - u)
                val phi = PI * (1.0 - pitch / 180.0)
                val direction = Float3(
                    (-sin(phi) * cos(theta)).toFloat(),
                    cos(phi).toFloat(),
                    (sin(phi) * sin(theta)).toFloat()
                )
                val update = requireNotNull(panoramicFootprintUpdate(direction, 75.0, 60.0))
                // The SDK convention is relative to image U=0.5, not the renderer's +Z axis.
                val expectedYaw = (360.0 * (u - 0.5) + 360.0) % 360.0
                val yawDifference = (update.yaw - expectedYaw + 540.0) % 360.0 - 180.0
                assertEquals("u=$u, pitch=$pitch", 0.0, yawDifference, 0.0001)
                assertEquals(pitch, update.pitch, 0.0001)
            }
        }
    }

    @Test
    fun `yaw wraps continuously at image center`() {
        for (heading in listOf(-1.0, 0.0, 1.0, 359.0, 360.0)) {
            val radians = Math.toRadians(heading)
            val direction = Float3(cos(radians).toFloat(), 0f, sin(radians).toFloat())
            val update = requireNotNull(panoramicFootprintUpdate(direction, 75.0, 60.0))
            val expectedYaw = (heading + 360.0) % 360.0
            val yawDifference = (update.yaw - expectedYaw + 540.0) % 360.0 - 180.0
            assertEquals(0.0, yawDifference, 0.0001)
        }
    }

    @Test
    fun `pitch is zero straight down and 180 straight up`() {
        assertEquals(0.0, requireNotNull(panoramicFootprintUpdate(Float3(0f, -1f, 0f), 75.0, 60.0)).pitch, 0.0)
        assertEquals(180.0, requireNotNull(panoramicFootprintUpdate(Float3(0f, 1f, 0f), 75.0, 60.0)).pitch, 0.0)
    }

    @Test
    fun `3D viewing direction preserves image relative yaw and pitch from straight down`() {
        for (yaw in listOf(0.0, 45.0, 90.0, 180.0, 270.0, 359.0)) {
            for (pitch in listOf(1.0, 30.0, 45.0, 90.0, 135.0, 179.0)) {
                val heading = Math.toRadians(yaw)
                val inclination = Math.toRadians(pitch)
                // Construct a ray from image-relative yaw and inclination on the reflected sphere.
                val direction = Float3(
                    (sin(inclination) * cos(heading)).toFloat(),
                    (-cos(inclination)).toFloat(),
                    (sin(inclination) * sin(heading)).toFloat()
                )
                val update = requireNotNull(panoramicFootprintUpdate(direction, 100.0, 40.0))
                assertEquals("yaw=$yaw, pitch=$pitch", yaw, update.yaw, 0.0001)
                assertEquals("yaw=$yaw, pitch=$pitch", pitch, update.pitch, 0.0001)
                assertEquals(100.0, update.horizontalFov, 0.0)
                assertEquals(40.0, update.verticalFov, 0.0)
            }
        }
    }

    @Test
    fun `footprint agrees with dot net camera convention at equivalent image coordinates`() {
        for (cameraYaw in listOf(-270.0, -90.0, 0.0, 45.0, 180.0, 359.0)) {
            for (cameraPitch in listOf(-80.0, -45.0, 0.0, 45.0, 80.0)) {
                // .NET: u = 0.75 + cameraYaw/360, v = 0.5 + cameraPitch/180.
                // Its sphere uses theta=2*PI*u, phi=PI*v; SceneView's reflected sphere
                // maps the same image coordinates to the opposite X and Z components.
                val theta = Math.toRadians(270.0 + cameraYaw)
                val phi = Math.toRadians(90.0 + cameraPitch)
                val direction = Float3(
                    (-sin(phi) * cos(theta)).toFloat(),
                    cos(phi).toFloat(),
                    (-sin(phi) * sin(theta)).toFloat()
                )
                for (aspect in listOf(0.5, 1.0, 2.0)) {
                    val verticalFov = 60.0
                    val horizontalFov = Math.toDegrees(2.0 * atan(aspect * tan(Math.toRadians(verticalFov) / 2.0)))
                    val update = requireNotNull(panoramicFootprintUpdate(direction, horizontalFov, verticalFov))
                    val expectedYaw = (90.0 + cameraYaw + 360.0) % 360.0
                    val difference = (update.yaw - expectedYaw + 540.0) % 360.0 - 180.0
                    assertEquals(0.0, difference, 0.0001)
                    assertEquals(90.0 - cameraPitch, update.pitch, 0.0001)
                    assertEquals(horizontalFov, update.horizontalFov, 0.0)
                    assertEquals(verticalFov, update.verticalFov, 0.0)
                }
            }
        }
    }

    @Test
    fun `initial view points at selected image pixel with matching footprint`() {
        for (u in listOf(0.0, 0.25, 0.3, 0.5, 0.75, 1.0)) {
            for (v in listOf(0.1, 0.5, 0.7, 0.9)) {
                val direction = requireNotNull(panoramicImageDirection(u * 2048, v * 1024, 2048, 1024))
                val update = requireNotNull(panoramicFootprintUpdate(direction, 75.0, 90.0))
                val expectedYaw = (360.0 * (u - 0.5) + 360.0) % 360.0
                val difference = (update.yaw - expectedYaw + 540.0) % 360.0 - 180.0
                assertEquals(0.0, difference, 0.0001)
                assertEquals(180.0 * (1.0 - v), update.pitch, 0.0001)
            }
        }
    }

    @Test
    fun `initial downward target does not use opposite side or horizon`() {
        val direction = requireNotNull(panoramicImageDirection(0.3 * 2048, 0.7 * 1024, 2048, 1024))
        val update = requireNotNull(panoramicFootprintUpdate(direction, 75.0, 90.0))
        assertEquals(288.0, update.yaw, 0.0001)
        assertEquals(54.0, update.pitch, 0.0001)
    }

    @Test
    fun `initial image direction wraps longitude and avoids singular poles`() {
        val center = requireNotNull(panoramicImageDirection(1024.0, 512.0, 2048, 1024))
        val wrapped = requireNotNull(panoramicImageDirection(3072.0, 512.0, 2048, 1024))
        assertEquals(center.x, wrapped.x, 0.0001f)
        assertEquals(center.z, wrapped.z, 0.0001f)
        for (y in listOf(0.0, 1024.0)) {
            val direction = requireNotNull(panoramicImageDirection(1024.0, y, 2048, 1024))
            val update = requireNotNull(panoramicFootprintUpdate(direction, 75.0, 90.0))
            assertEquals(if (y == 0.0) 180.0 else 0.0, update.pitch, 0.01)
        }
        assertNull(panoramicImageDirection(Double.NaN, 512.0, 2048, 1024))
        assertNull(panoramicImageDirection(1024.0, Double.POSITIVE_INFINITY, 2048, 1024))
        assertNull(panoramicImageDirection(1024.0, -1.0, 2048, 1024))
        assertNull(panoramicImageDirection(1024.0, 1025.0, 2048, 1024))
        assertNull(panoramicImageDirection(1024.0, 512.0, 0, 1024))
    }

    @Test
    fun `geographic footprint uses calibrated heading not image longitude`() {
        val orientation = PanoramicFootprintOrientation(
            eastInRenderer = Float3(-1f, 0f, 0f),
            northInRenderer = Float3(0f, 0f, -1f),
            upInRenderer = Float3(0f, 1f, 0f)
        )
        val displayedRay = requireNotNull(panoramicImageDirection(512.0, 512.0, 2048, 1024))
        val imageAngles = requireNotNull(panoramicFootprintUpdate(displayedRay, 75.0, 90.0))
        val geographicAngles = requireNotNull(panoramicFootprintUpdate(
            orientation.toFootprintDirection(displayedRay), 75.0, 90.0
        ))
        assertEquals(270.0, imageAngles.yaw, 0.0001)
        assertEquals(0.0, geographicAngles.yaw, 0.0001)
        assertEquals(90.0, geographicAngles.pitch, 0.0001)
        assertEquals(75.0, geographicAngles.horizontalFov, 0.0)
        assertEquals(90.0, geographicAngles.verticalFov, 0.0)
    }

    @Test
    fun `calibrated footprint preserves geographic inclination for tilted imagery`() {
        val c = cos(Math.toRadians(30.0)).toFloat()
        val s = sin(Math.toRadians(30.0)).toFloat()
        val orientation = PanoramicFootprintOrientation(
            eastInRenderer = Float3(0f, 0f, 1f),
            northInRenderer = Float3(c, s, 0f),
            upInRenderer = Float3(-s, c, 0f)
        )
        for (heading in listOf(0.0, 45.0, 90.0, 180.0, 270.0, 359.0)) {
            for (pitch in listOf(15.0, 60.0, 90.0, 120.0, 165.0)) {
                val yawRad = Math.toRadians(heading)
                val pitchRad = Math.toRadians(pitch)
                val east = (sin(yawRad) * sin(pitchRad)).toFloat()
                val north = (cos(yawRad) * sin(pitchRad)).toFloat()
                val up = (-cos(pitchRad)).toFloat()
                val displayedRay = Float3(c * north - s * up, s * north + c * up, east)
                val update = requireNotNull(panoramicFootprintUpdate(
                    orientation.toFootprintDirection(displayedRay), 75.0, 90.0
                ))
                assertEquals(heading, update.yaw, 0.0001)
                assertEquals(pitch, update.pitch, 0.0001)
            }
        }
    }

    @Test
    fun `angle conversion is independent of ray length without float overflow or underflow`() {
        for (scale in listOf(Float.MIN_VALUE, 1f, Float.MAX_VALUE)) {
            val update = requireNotNull(panoramicFootprintUpdate(Float3(0f, -scale, scale), 75.0, 60.0))
            assertEquals(90.0, update.yaw, 0.0001)
            assertEquals(45.0, update.pitch, 0.0001)
        }
    }

    @Test
    fun `vertical poles use deterministic image center yaw`() {
        for (up in listOf(-1f, 1f)) {
            val update = requireNotNull(panoramicFootprintUpdate(Float3(-0.0f, up, -0.0f), 75.0, 60.0))
            assertEquals(0.0, update.yaw, 0.0)
        }
    }

    @Test
    fun `zero ray and invalid perspective fields of view do not produce updates`() {
        assertNull(panoramicFootprintUpdate(Float3(0f, 0f, 0f), 75.0, 60.0))
        for (invalidFov in listOf(-1.0, 0.0, 180.0, 360.0)) {
            assertNull(panoramicFootprintUpdate(Float3(0f, 0f, 1f), invalidFov, 60.0))
            assertNull(panoramicFootprintUpdate(Float3(0f, 0f, 1f), 75.0, invalidFov))
        }
    }

    @Test
    fun `non finite parameters do not produce footprint updates`() {
        assertNull(panoramicFootprintUpdate(Float3(Float.NaN, 0f, 1f), 75.0, 60.0))
        assertNull(panoramicFootprintUpdate(Float3(Float.POSITIVE_INFINITY, 0f, 1f), 75.0, 60.0))
        assertNull(panoramicFootprintUpdate(Float3(0f, Float.NEGATIVE_INFINITY, 1f), 75.0, 60.0))
        assertNull(panoramicFootprintUpdate(Float3(0f, 0f, Float.POSITIVE_INFINITY), 75.0, 60.0))
        assertNull(panoramicFootprintUpdate(Float3(1f, 0f, 0f), Double.NaN, 60.0))
        assertNull(panoramicFootprintUpdate(Float3(1f, 0f, 0f), 75.0, Double.POSITIVE_INFINITY))
        assertNotNull(panoramicFootprintUpdate(Float3(1f, 0f, 0f), 75.0, 60.0))
    }
}
