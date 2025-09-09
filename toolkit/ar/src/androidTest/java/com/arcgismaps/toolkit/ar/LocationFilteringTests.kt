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

import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.Location
import com.arcgismaps.toolkit.ar.internal.WorldScaleParameters
import com.arcgismaps.toolkit.ar.internal.shouldUpdateCamera
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Tests for the location filtering logic of the [WorldScaleSceneView]
 */
class LocationFilteringTests {

    /**
     * Given a location and a current camera,
     * When the location is old, close to the camera, or inaccurate,
     * Then the camera should not be updated
     *
     * When the location is recent and far away from the camera,
     * Then the camera should be updated
     *
     * @since 200.7.0
     */
    @Test
    fun testLocationFilter() {
        val nullIslandWgs84 = Point(
            0.0,
            0.0,
            0.0,
            SpatialReference(
                WorldScaleParameters.WKID_WGS84,
                WorldScaleParameters.WKID_WGS84_VERTICAL
            )
        )
        val nullIslandEGM96Vertical = GeometryEngine.projectOrNull(
            nullIslandWgs84,
            SpatialReference(
                WorldScaleParameters.WKID_WGS84,
                WorldScaleParameters.WKID_EGM96_VERTICAL
            )
        )
        val farFarAwayWgs84 = Point(
            30.0,
            30.0,
            0.0,
            SpatialReference(
                WorldScaleParameters.WKID_WGS84,
                WorldScaleParameters.WKID_WGS84_VERTICAL
            )
        )

        assertThat(nullIslandEGM96Vertical).isNotNull()

        val recentLocationAtNullIsland =
            Location.create(nullIslandWgs84, 1.0, 1.0, 0.0, 0.0, true, Instant.now())
        assertThat(shouldUpdateCamera(recentLocationAtNullIsland, nullIslandEGM96Vertical)).isFalse()

        val oldLocationAtNullIsland =
            Location.create(nullIslandWgs84, 1.0, 1.0, 0.0, 0.0, true, Instant.now().minusMillis(30000))
        assertThat(shouldUpdateCamera(oldLocationAtNullIsland, nullIslandEGM96Vertical)).isFalse()

        val oldLocationFarFarAway =
            Location.create(farFarAwayWgs84, 1.0, 1.0, 0.0, 0.0, true, Instant.now().minusMillis(30000))
        assertThat(shouldUpdateCamera(oldLocationFarFarAway, nullIslandEGM96Vertical)).isFalse()

        val recentLocationWithNoAccuracy = Location.create(
            nullIslandWgs84,
            Double.NaN,
            Double.NaN,
            0.0,
            0.0,
            false,
            Instant.now().minusMillis(50)
        )
        assertThat(shouldUpdateCamera(recentLocationWithNoAccuracy, nullIslandEGM96Vertical)).isFalse()

        val recentLocationWithBadAccuracy = Location.create(
            nullIslandWgs84,
            10.0,
            10.0,
            0.0,
            0.0,
            false,
            Instant.now().minusMillis(50)
        )
        assertThat(shouldUpdateCamera(recentLocationWithBadAccuracy, nullIslandEGM96Vertical)).isFalse()

        val recentLocationFarFarAway =
            Location.create(farFarAwayWgs84, 1.0, 1.0, 0.0, 0.0, true, Instant.now())
        assertThat(shouldUpdateCamera(recentLocationFarFarAway, nullIslandEGM96Vertical)).isTrue()
    }
}
