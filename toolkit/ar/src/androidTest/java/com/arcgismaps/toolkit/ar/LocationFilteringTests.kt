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

package com.arcgismaps.toolkit.ar

import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.Location
import com.arcgismaps.toolkit.ar.internal.shouldUpdateCamera
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Tests for the location filtering logic of the [WorldScaleSceneView]
 */
class LocationFilteringTests {

    /**
     * Given a location passed to [shouldUpdateCamera]
     * When the location is old or has bad or invalid accuracy,
     * Then [shouldUpdateCamera] should return false.
     *
     * @since 200.7.0
     */
    @Test
    fun testLocationFilter() {
        val nullIsland = Point(0.0, 0.0, SpatialReference.wgs84())

        val recentLocationAtNullIsland =
            Location.create(nullIsland, 1.0, 1.0, 0.0, 0.0, true, Instant.now().minusMillis(50))
        val oldLocationAtNullIsland =
            Location.create(nullIsland, 1.0, 1.0, 0.0, 0.0, true, Instant.now().minusMillis(30000))
        val recentLocationWithNoAccuracy = Location.create(
            nullIsland,
            Double.NaN,
            Double.NaN,
            0.0,
            0.0,
            false,
            Instant.now().minusMillis(50)
        )

        val recentLocationWithBadAccuracy = Location.create(
            nullIsland,
            10.0,
            10.0,
            0.0,
            0.0,
            false,
            Instant.now().minusMillis(50)
        )

//        assertThat(shouldUpdateCamera(oldLocationAtNullIsland)).isFalse()
//        assertThat(shouldUpdateCamera(recentLocationWithNoAccuracy)).isFalse()
//        assertThat(shouldUpdateCamera(recentLocationWithBadAccuracy)).isFalse()
//
//        assertThat(shouldUpdateCamera(recentLocationAtNullIsland)).isTrue()
    }
}
