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

package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.orderByAssetGroup
import com.arcgismaps.utilitynetworks.UtilityAssetGroup
import com.arcgismaps.utilitynetworks.UtilityAssetType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderByAssetGroupTests {

    /**
     * Given a list of UtilityAssetTypes with duplicate names,
     * When the list is ordered by asset group name,
     * Then the trailing run of duplicate names is sorted by asset group name, in ascending order.
     */
    @Test
    fun `sorts trailing run of duplicate names by asset group name`() {
        val a = assetType(name = "Single Pole", groupName = "Electric Medium Voltage Pole")
        val b = assetType(name = "Single Pole", groupName = "Electric Low Voltage Pole")
        val c = assetType(name = "Single Pole", groupName = "Electric High Voltage Pole")

        val result = listOf(a, b, c).orderByAssetGroup()

        assertEquals(
            listOf(
                "Electric High Voltage Pole",
                "Electric Low Voltage Pole",
                "Electric Medium Voltage Pole"
            ),
            result.map { it.assetGroup.name }
        )
    }

    /**
     * Given a list of UtilityAssetTypes with multiple runs of duplicate names,
     * When the list is ordered by asset group name,
     * Then each run of duplicate names is sorted by asset group name, in ascending order, while
     * unique items and the relative positions of the runs remain unchanged.
     */
    @Test
    fun `sorts multiple duplicate runs`() {
        val first = assetType(name = "Arrestor", groupName = "Electric Device")

        // Run 1 (duplicate name): should be sorted by assetGroup.name within this run.
        val poleRun1 = assetType(name = "Single Pole", groupName = "Electric Medium Voltage Pole")
        val poleRun2 = assetType(name = "Single Pole", groupName = "Electric Low Voltage Pole")

        val middle = assetType(name = "Switch", groupName = "Electric Device")

        // Run 2 (duplicate name): should be sorted independently from Run 1.
        val transformerRun1 = assetType(name = "Transformer", groupName = "Electric Distribution Device")
        val transformerRun2 = assetType(name = "Transformer", groupName = "Electric SubTransmission Device")
        val transformerRun3 = assetType(name = "Transformer", groupName = "Electric Transmission Device")

        val last = assetType(name = "Triple Circuit Lattice", groupName = "Electric High Voltage Pole")

        val result = listOf(
            first,
            poleRun1,
            poleRun2,
            middle,
            transformerRun1,
            transformerRun2,
            transformerRun3,
            last
        ).orderByAssetGroup()

        // Names remain in the same overall structure (runs and unique elements keep their positions).
        assertEquals(
            listOf(
                "Arrestor",
                "Single Pole",
                "Single Pole",
                "Switch",
                "Transformer",
                "Transformer",
                "Transformer",
                "Triple Circuit Lattice"
            ),
            result.map { it.name }
        )

        // Each run is sorted by asset group name; unique items remain where they were.
        assertEquals(
            listOf(
                "Electric Device", // Arrestor
                "Electric Low Voltage Pole", // Single Pole run sorted
                "Electric Medium Voltage Pole",
                "Electric Device", // Switch
                "Electric Distribution Device", // Transformer run sorted
                "Electric SubTransmission Device",
                "Electric Transmission Device",
                "Electric High Voltage Pole" // Triple Circuit Lattice
            ),
            result.map { it.assetGroup.name }
        )
    }

    /**
     * Given a list of UtilityAssetTypes with duplicate names that differ only by case,
     * When the list is ordered by asset group name,
     * Then the run of duplicate names is sorted by asset group name, in ascending order, treating
     * the names as case-insensitive.
     */
    @Test
    fun `treats name as case-insensitive when forming runs`() {
        val a = assetType(name = "Pole", groupName = "Electric Medium Voltage Pole")
        val b = assetType(name = "POLE", groupName = "eLeCtRiC Low Voltage pole")

        val result = listOf(a, b).orderByAssetGroup()

        assertEquals(
            listOf("eLeCtRiC Low Voltage pole", "Electric Medium Voltage Pole"),
            result.map { it.assetGroup.name }
        )
    }

    /**
     * Helper to create a mock UtilityAssetType with the specified name and asset group name.
     *
     * @param name the name of the UtilityAssetType
     * @param groupName the name of the UtilityAssetGroup associated with the UtilityAssetType
     */
    private fun assetType(name: String, groupName: String): UtilityAssetType {
        val group = mockk<UtilityAssetGroup> {
            every { this@mockk.name } returns groupName
        }

        return mockk {
            every { this@mockk.name } returns name
            every { this@mockk.assetGroup } returns group
        }
    }
}
