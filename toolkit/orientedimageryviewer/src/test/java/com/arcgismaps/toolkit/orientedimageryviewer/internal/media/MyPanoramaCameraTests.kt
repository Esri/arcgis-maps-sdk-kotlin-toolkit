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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPanoramaCameraTests {
    @Test
    fun imagePixelsAndCameraUseTheSameSphereMapping() {
        for (x in listOf(0.0, 123.0, 500.0, 750.0, 1000.0, 1250.0)) {
            for (y in listOf(1.0, 111.0, 250.0, 499.0)) {
                val camera = requireNotNull(MyPanoramaCamera.fromPixel(x, y, 1000, 500))
                val ray = requireNotNull(MyPanoramaCamera.imageRay(x, y, 1000, 500))
                assertEquals(ray.x, camera.forward.x, 1e-9)
                assertEquals(ray.y, camera.forward.y, 1e-9)
                assertEquals(ray.z, camera.forward.z, 1e-9)
            }
        }
    }

    @Test
    fun defaultCameraLooksAtImageCenter() {
        val center = requireNotNull(MyPanoramaCamera.fromPixel(500.0, 250.0, 1000, 500))
        assertEquals(center.forward.x, MyPanoramaCamera().forward.x, 1e-9)
        assertEquals(center.forward.z, MyPanoramaCamera().forward.z, 1e-9)
    }

    @Test
    fun seamWrapsAndPolesRemainStable() {
        val left = requireNotNull(MyPanoramaCamera.fromPixel(0.0, 250.0, 1000, 500))
        val right = requireNotNull(MyPanoramaCamera.fromPixel(1000.0, 250.0, 1000, 500))
        assertEquals(left, right)
        val top = requireNotNull(MyPanoramaCamera.fromPixel(0.0, 0.0, 1000, 500))
        assertEquals(-89.9, top.pitch, 0.0)
        assertTrue(top.forward.y > 0.999)
        assertEquals(1.0, requireNotNull(MyPanoramaCamera.imageRay(0.0, 0.0, 1000, 500)).y, 0.0)
    }

    @Test
    fun invalidImageCoordinatesAreRejected() {
        assertNull(MyPanoramaCamera.fromPixel(0.0, 1.0, 0, 500))
        assertNull(MyPanoramaCamera.fromPixel(Double.NaN, 1.0, 1000, 500))
        assertNull(MyPanoramaCamera.fromPixel(0.0, -1.0, 1000, 500))
        assertNull(MyPanoramaCamera.imageRay(0.0, 501.0, 1000, 500))
    }

    @Test
    fun draggingFollowsTheFingerAndClampsAtPoles() {
        val camera = MyPanoramaCamera(yaw = 180.0)
        val dragged = camera.drag(10.0, 10.0, 1000)
        assertTrue(dragged.yaw < camera.yaw)
        assertTrue(dragged.pitch < camera.pitch)
        assertEquals(-89.9, camera.drag(0.0, 100000.0, 1000).pitch, 0.0)
        assertEquals(camera, camera.drag(10.0, 10.0, 0))
        assertEquals(camera, camera.drag(Double.NaN, 1.0, 1000))
    }

    @Test
    fun zoomUsesStableVerticalFovAndRejectsInvalidScales() {
        val camera = MyPanoramaCamera()
        assertEquals(45.0, camera.zoom(2.0).verticalFov, 0.0)
        assertEquals(25.0, camera.zoom(100.0).verticalFov, 0.0)
        assertEquals(100.0, camera.zoom(0.001).verticalFov, 0.0)
        assertEquals(camera, camera.zoom(0.0))
        assertEquals(camera, camera.zoom(Double.NaN))
        assertEquals(camera, camera.zoom(Double.POSITIVE_INFINITY))
    }

    @Test
    fun projectionAccountsForViewportAspect() {
        val camera = MyPanoramaCamera()
        assertEquals(90.0, camera.horizontalFov(1.0), 1e-9)
        assertEquals(126.86989765, camera.horizontalFov(2.0), 1e-7)
        assertEquals(53.13010235, camera.horizontalFov(0.5), 1e-7)
    }

    @Test
    fun geographicCalibrationDoesNotConfuseImageYawWithHeading() {
        // In this synthetic image, north is -Z and east is +X.
        val orientation = MyPanoramaOrientation(
            east = MyPanoramaRay(1.0, 0.0, 0.0),
            north = MyPanoramaRay(0.0, 0.0, -1.0),
            up = MyPanoramaRay(0.0, 1.0, 0.0)
        )
        val north = orientation.footprint(MyPanoramaCamera(yaw = 0.0), 1.0)
        assertTrue(north.yaw < 1e-9 || north.yaw > 360.0 - 1e-9)
        assertEquals(90.0, north.pitch, 1e-9)
        val east = orientation.footprint(MyPanoramaCamera(yaw = 90.0, pitch = 30.0), 2.0)
        assertEquals(90.0, east.yaw, 1e-9)
        assertEquals(60.0, east.pitch, 1e-9)
        assertEquals(126.86989765, east.horizontalFov, 1e-7)
    }

    @Test
    fun footprintDeduplicationUsesCircularHeadingDifference() {
        val update = MyPanoramaFootprint(359.98, 90.0, 90.0, 90.0)
        assertTrue(update.approximatelyEquals(update.copy(yaw = 0.02)))
        assertFalse(update.approximatelyEquals(update.copy(yaw = 0.5)))
        assertFalse(update.approximatelyEquals(update.copy(verticalFov = 89.0)))
        assertFalse(update.approximatelyEquals(null))
    }
}
