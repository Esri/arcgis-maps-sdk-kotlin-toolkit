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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [WorldScaleTrackingMode].
 */
class WorldScaleTrackingModeTests {

    /**
     * Given two instances of [WorldScaleTrackingMode.World],
     * When they are compared,
     * Then they should be equal.
     *
     * Given two instances of [WorldScaleTrackingMode.Geospatial],
     * When they are compared,
     * Then they should be equal.
     *
     * Given an instance of [WorldScaleTrackingMode.World] and an instance of [WorldScaleTrackingMode.Geospatial],
     * When they are compared,
     * Then they should not be equal.
     *
     * @since 200.7.0
     */
    @Test
    fun equalsHashCode() {
        val worldMode1 = WorldScaleTrackingMode.World()
        val worldMode2 = WorldScaleTrackingMode.World()

        val geospatialMode1 = WorldScaleTrackingMode.Geospatial()
        val geospatialMode2 = WorldScaleTrackingMode.Geospatial()

        assertThat(worldMode1).isEqualTo(worldMode2)
        assertThat(worldMode1.hashCode()).isEqualTo(worldMode2.hashCode())
        assertThat(worldMode1.toString()).isEqualTo("WorldScaleTrackingMode.World")
        assertThat(worldMode2.toString()).isEqualTo(worldMode1.toString())

        assertThat(geospatialMode1).isEqualTo(geospatialMode2)
        assertThat(geospatialMode1.hashCode()).isEqualTo(geospatialMode2.hashCode())
        assertThat(geospatialMode1.toString()).isEqualTo("WorldScaleTrackingMode.Geospatial")
        assertThat(geospatialMode2.toString()).isEqualTo(geospatialMode1.toString())

        assertThat(worldMode1).isNotEqualTo(geospatialMode1)
        assertThat(worldMode1.hashCode()).isNotEqualTo(geospatialMode1.hashCode())
        assertThat(worldMode1.toString()).isNotEqualTo(geospatialMode1.toString())
    }
}
