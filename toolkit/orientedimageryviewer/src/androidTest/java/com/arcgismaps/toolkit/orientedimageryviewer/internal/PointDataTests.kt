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

package com.arcgismaps.toolkit.orientedimageryviewer.internal

import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PointDataTests {
    @Test
    fun pointSnapshotsPreserveAllSpatialReferenceDefinitions() {
        val custom = SpatialReference(
            """PROJCS["Custom_Transverse_Mercator",GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",SPHEROID["WGS_1984",6378137.0,298.257223563]],PRIMEM["Greenwich",0.0],UNIT["Degree",0.0174532925199433]],PROJECTION["Transverse_Mercator"],PARAMETER["False_Easting",123456.0],PARAMETER["False_Northing",0.0],PARAMETER["Central_Meridian",12.34],PARAMETER["Scale_Factor",0.9996],PARAMETER["Latitude_Of_Origin",0.0],UNIT["Meter",1.0]]"""
        )
        assertEquals(0, custom.wkid)
        for (reference in listOf(null, SpatialReference.wgs84(), SpatialReference(3857, 5773), custom)) {
            for (point in listOf(Point(1.0, 2.0, reference), Point(1.0, 2.0, 3.0, reference))) {
                val restored = point.toPointData().toPoint()
                assertEquals(point.x, restored.x, 0.0)
                assertEquals(point.y, restored.y, 0.0)
                assertEquals(point.z, restored.z)
                assertEquals(reference, restored.spatialReference)
                assertEquals(reference?.toJson(), restored.spatialReference?.toJson())
            }
        }
    }

    @Test
    fun verticalReferencesProduceDistinctSnapshots() {
        val first = Point(1.0, 2.0, 3.0, SpatialReference(3857, 5773)).toPointData()
        val second = Point(1.0, 2.0, 3.0, SpatialReference(3857, 115700)).toPointData()
        assertNotEquals(first, second)
        assertEquals(5773, first.toPoint().spatialReference?.verticalWkid)
        assertEquals(115700, second.toPoint().spatialReference?.verticalWkid)
    }
}
