package com.arcgismaps.toolkit.geocompose

import android.os.Parcel
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ViewpointPersistenceTests {

    @Test
    fun testParcelable() {
        // Create instances of each class
        val none = ViewpointPersistence.None
        val byCenterAndScale = ViewpointPersistence.ByCenterAndScale()
        val byBoundingGeometry = ViewpointPersistence.ByBoundingGeometry()

        // Write them to a Parcel
        val parcel = Parcel.obtain()
        none.writeToParcel(parcel, 0)
        byCenterAndScale.writeToParcel(parcel, 0)
        byBoundingGeometry.writeToParcel(parcel, 0)

        // Reset the parcel for reading
        parcel.setDataPosition(0)

        // Read them back from the Parcel
        val fromParcelNone = ViewpointPersistence.None.CREATOR.createFromParcel(parcel)
        val fromParcelByCenterAndScale = ViewpointPersistence.ByCenterAndScale.CREATOR.createFromParcel(parcel)
        val fromParcelByBoundingGeometry = ViewpointPersistence.ByBoundingGeometry.CREATOR.createFromParcel(parcel)

        // Assert that the read back instances are equal to the original ones
        assertEquals(none, fromParcelNone)
        assertEquals(byCenterAndScale, fromParcelByCenterAndScale)
        assertEquals(byBoundingGeometry, fromParcelByBoundingGeometry)
    }
}