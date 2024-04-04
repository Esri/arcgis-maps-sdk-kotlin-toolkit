/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.geoviewcompose

import android.os.Parcel
import junit.framework.TestCase.assertEquals
import org.junit.Test

/**
 * Tests for the [ViewpointPersistence] class.
 *
 * @since 200.4.0
 */
class ViewpointPersistenceTests {

    /**
     * GIVEN a [ViewpointPersistence] object
     * WHEN it is created from a [Parcel]
     * THEN the read back object is equal to the original one
     *
     * @since 200.4.0
     */
    @Test
    fun testParcelable() {
        // Create instances of each class
        val none = ViewpointPersistence.None
        val byCenterAndScale = ViewpointPersistence.ByCenterAndScale()
        val byBoundingGeometry = ViewpointPersistence.ByBoundingGeometry()


        // Create the classes from a parcel
        // Note that none of these classes actually need data from the parcel, so we never need to write
        // to it
        val parcel = Parcel.obtain()
        val fromParcelNone = ViewpointPersistence.None.CREATOR.createFromParcel(parcel)
        val fromParcelByCenterAndScale = ViewpointPersistence.ByCenterAndScale.CREATOR.createFromParcel(parcel)
        val fromParcelByBoundingGeometry = ViewpointPersistence.ByBoundingGeometry.CREATOR.createFromParcel(parcel)

        // Assert that the read back instances are equal to the original ones
        assertEquals(none, fromParcelNone)
        assertEquals(byCenterAndScale, fromParcelByCenterAndScale)
        assertEquals(byBoundingGeometry, fromParcelByBoundingGeometry)
    }
}
