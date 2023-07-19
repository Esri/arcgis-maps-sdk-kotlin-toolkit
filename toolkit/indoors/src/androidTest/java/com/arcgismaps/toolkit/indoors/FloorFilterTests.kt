/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.indoors

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.composablemap.MapInterfaceImpl
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FloorFilterTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val floorFilterTestTag = "FloorFilterComponent"
    private val portal = Portal("https://arcgis.com/")
    private val portalItem = PortalItem(portal, "b4b599a43a474d33946cf0df526426f5")
    private val floorAwareWebMap = ArcGISMap(portalItem)
    private val mapViewModel = MapViewModel(floorAwareWebMap)

    @Before
    fun setUpFloorFilter(){
        composeTestRule.setContent {
            FloorFilter(floorFilterState = mapViewModel.floorFilterState)
        }
    }

    /**
     * Given a [FloorFilterState], display the floor filter component
     */
    @Test
    fun testFloorFilterDisplay() {
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithContentDescription("FloorFilterComponent")
                .fetchSemanticsNodes().size == 1
        }

        val floorFilter = composeTestRule.onNodeWithContentDescription(
            label = floorFilterTestTag
        )

        floorFilter.assertIsDisplayed()
    }
}


class MapViewModel(
    arcGISMap: ArcGISMap
) : ViewModel(), MapInterface by MapInterfaceImpl(arcGISMap) {
    val floorFilterState: FloorFilterState = FloorFilterState(this, viewModelScope)
}
