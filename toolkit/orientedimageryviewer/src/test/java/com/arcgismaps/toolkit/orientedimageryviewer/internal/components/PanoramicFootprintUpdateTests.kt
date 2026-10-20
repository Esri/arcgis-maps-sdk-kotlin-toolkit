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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanoramicFootprintUpdateTests {
    private val orientation = PanoramaOrientation(
        east = PanoramaRay(1.0, 0.0, 0.0),
        north = PanoramaRay(0.0, 0.0, -1.0),
        up = PanoramaRay(0.0, 1.0, 0.0)
    )

    @Test
    fun `footprint follows panorama camera yaw and pitch conventions`() {
        for ((camera, expected) in listOf(
            PanoramaCamera(yaw = 0.0) to (0.0 to 90.0),
            PanoramaCamera(yaw = 90.0) to (90.0 to 90.0),
            PanoramaCamera(yaw = 180.0) to (180.0 to 90.0),
            PanoramaCamera(yaw = 270.0) to (270.0 to 90.0),
            PanoramaCamera(yaw = 359.0) to (359.0 to 90.0),
            PanoramaCamera(yaw = -1.0) to (359.0 to 90.0),
            PanoramaCamera(yaw = 90.0, pitch = 30.0) to (90.0 to 60.0),
            PanoramaCamera(yaw = 90.0, pitch = -89.9) to (90.0 to 179.9),
            PanoramaCamera(yaw = 90.0, pitch = 89.9) to (90.0 to 0.1)
        )) {
            val (expectedYaw, expectedPitch) = expected
            val footprint = orientation.footprint(camera, 1.0)
            assertEquals(expectedYaw, footprint.yaw, 0.0001)
            assertEquals(expectedPitch, footprint.pitch, 0.0001)
        }
    }

    @Test
    fun `seam and poles remain stable for wrapped yaw values`() {
        val left = orientation.footprint(PanoramaCamera(yaw = 359.0), 1.0)
        val right = orientation.footprint(PanoramaCamera(yaw = 0.0), 1.0)
        assertEquals(359.0, left.yaw, 0.0001)
        assertEquals(0.0, right.yaw, 0.0001)

        val nearTop = orientation.footprint(PanoramaCamera(yaw = 90.0, pitch = 89.9), 1.0)
        val nearBottom = orientation.footprint(PanoramaCamera(yaw = 90.0, pitch = -89.9), 1.0)
        assertEquals(90.0, nearTop.yaw, 0.0001)
        assertEquals(0.1, nearTop.pitch, 0.0001)
        assertEquals(90.0, nearBottom.yaw, 0.0001)
        assertEquals(179.9, nearBottom.pitch, 0.0001)
    }

    @Test
    fun `footprint deduplication uses circular heading difference`() {
        val update = PanoramaFootprint(359.98, 90.0, 90.0, 90.0)
        assertTrue(update.approximatelyEquals(update.copy(yaw = 0.02)))
        assertFalse(update.approximatelyEquals(update.copy(yaw = 0.5)))
        assertFalse(update.approximatelyEquals(update.copy(verticalFov = 89.0)))
        assertFalse(update.approximatelyEquals(null))
    }
}
