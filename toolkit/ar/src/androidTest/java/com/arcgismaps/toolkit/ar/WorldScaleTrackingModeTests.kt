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

        assertThat(geospatialMode1).isEqualTo(geospatialMode2)
        assertThat(geospatialMode1.hashCode()).isEqualTo(geospatialMode2.hashCode())

        assertThat(worldMode1).isNotEqualTo(geospatialMode1)
        assertThat(worldMode1.hashCode()).isNotEqualTo(geospatialMode1.hashCode())
    }
}