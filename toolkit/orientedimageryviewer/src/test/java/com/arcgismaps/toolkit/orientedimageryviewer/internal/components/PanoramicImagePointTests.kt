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
import org.junit.Assert.assertNull
import org.junit.Test

class PanoramicImagePointTests {
    @Test
    fun `tap rays round trip to image pixels including both sides of seam`() {
        for (x in listOf(1.0, 250.0, 500.0, 750.0, 999.0)) {
            for (y in listOf(1.0, 125.0, 250.0, 375.0, 499.0)) {
                val direction = requireNotNull(panoramicImageDirection(x, y, 1000, 500))
                val point = requireNotNull(panoramicImagePoint(direction, 1000, 500))
                assertEquals(x, point.x, 0.0001)
                assertEquals(y, point.y, 0.0001)
            }
        }
    }

    @Test
    fun `image center and poles follow reflected sphere orientation`() {
        val center = requireNotNull(panoramicImagePoint(Float3(1f, 0f, 0f), 1000, 500))
        assertEquals(500.0, center.x, 0.0)
        assertEquals(250.0, center.y, 0.0)
        assertEquals(0.0, requireNotNull(panoramicImagePoint(Float3(0f, 1f, 0f), 1000, 500)).y, 0.0)
        assertEquals(500.0, requireNotNull(panoramicImagePoint(Float3(0f, -1f, 0f), 1000, 500)).y, 0.0)
        assertEquals(0.0, requireNotNull(panoramicImagePoint(Float3(-1f, 0f, 0f), 1000, 500)).x, 0.0)
    }

    @Test
    fun `ray length does not affect image pixels`() {
        val unit = requireNotNull(panoramicImagePoint(Float3(1f, 1f, -1f), 1000, 500))
        val scaled = requireNotNull(panoramicImagePoint(Float3(10f, 10f, -10f), 1000, 500))
        assertEquals(unit.x, scaled.x, 0.0001)
        assertEquals(unit.y, scaled.y, 0.0001)
    }

    @Test
    fun `invalid dimensions and rays are rejected`() {
        assertNull(panoramicImagePoint(Float3(1f, 0f, 0f), 0, 500))
        assertNull(panoramicImagePoint(Float3(1f, 0f, 0f), 1000, -1))
        assertNull(panoramicImagePoint(Float3(0f, 0f, 0f), 1000, 500))
        assertNull(panoramicImagePoint(Float3(Float.NaN, 0f, 0f), 1000, 500))
        assertNull(panoramicImagePoint(Float3(0f, Float.POSITIVE_INFINITY, 0f), 1000, 500))
    }
}
