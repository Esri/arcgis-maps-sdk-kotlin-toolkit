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

@file:Suppress("DEPRECATION")

package com.arcgismaps.toolkit.indoors

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
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

    private val portal = Portal("https://arcgis.com/")
    private val portalItem = PortalItem(portal, "b4b599a43a474d33946cf0df526426f5")
    private val floorAwareWebMap = ArcGISMap(portalItem)
    private val mapViewModel = MapViewModel(floorAwareWebMap)

    /**
     * Sets up the [composeTestRule] content by adding the [FloorFilter] component.
     */
    @Before
    fun setUpFloorFilter() {
        composeTestRule.setContent {
            FloorFilter(floorFilterState = mapViewModel.floorFilterState)
        }
    }

    /**
     * Tests the [FloorFilter] component to see if it is displayed,
     * selects a site, then facility and verifies if a given floor level can be selected.
     * Then verify if the floor levels list can be collapsed.
     */
    @Test
    fun testFloorFilterInteractions() {
        // wait for the floor filter component to load and display in view
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithContentDescription("Sites and facilities button")
                .fetchSemanticsNodes().size == 1
        }

        // get the semantic node of the site facility dialog selector button
        val siteFacilityButton = composeTestRule.onNodeWithContentDescription(
            label = "Sites and facilities button"
        )

        // verify if the selector button is being displayed
        siteFacilityButton.assertIsDisplayed()

        // open the site facility dialog selector
        siteFacilityButton.performClick()
        composeTestRule.waitForIdle()

        // get the semantic node of the second site displayed in the list of sites ("Research Annex")
        val researchAnnexSiteItem = composeTestRule.onAllNodesWithContentDescription(
            label = "Site item"
        )[1]

        // verify if the expected site is being displayed
        researchAnnexSiteItem.assertIsDisplayed()

        // select the site "Research Annex"
        researchAnnexSiteItem.performClick()
        composeTestRule.waitForIdle()

        // get the semantic node of the first facility of the selected site ("Lattice")
        val latticeFacilityItem = composeTestRule.onAllNodesWithContentDescription(
            label = "Facility item"
        )[0]

        // verify if the expected facility is being displayed
        latticeFacilityItem.assertIsDisplayed()

        // select the facility "Lattice"
        latticeFacilityItem.performClick()
        composeTestRule.waitForIdle()

        // get the collection of semantic node of all floor levels of the selected facility
        val floorLevelButtons = composeTestRule.onAllNodesWithContentDescription(
            label = "Floor level select button"
        )

        // verify if the facility has 12 floor levels
        assert(floorLevelButtons.fetchSemanticsNodes().size == 12)

        // verify that the collapse button is being displayed
        val floorListCollapseButton = composeTestRule.onAllNodesWithContentDescription(
            label = "Collapse"
        )[0]

        // get semantic node of the 8th floor level select button
        val eighthFloorLevelButton = floorLevelButtons[7]

        // get semantic node of the 1st floor level select button
        val firstFloorLevelButton = floorLevelButtons[0]

        // verify if the 8th floor level select button is being displayed
        eighthFloorLevelButton.assertIsDisplayed()

        // verify if the 1st floor level select button is being displayed
        firstFloorLevelButton.assertIsDisplayed()

        // select the 8th floor of the facility
        eighthFloorLevelButton.performClick()
        composeTestRule.waitForIdle()

        // collapse the floor level list
        floorListCollapseButton.performClick()
        composeTestRule.waitForIdle()

        // verify the floor level selector is now collapsed
        floorListCollapseButton.assertExists()
        eighthFloorLevelButton.assertIsNotDisplayed()
        firstFloorLevelButton.assertIsDisplayed()
    }
}

class MapViewModel(
    arcGISMap: ArcGISMap
) : ViewModel(), MapInterface by MapInterfaceImpl(arcGISMap) {
    val floorFilterState: FloorFilterState =
        FloorFilterState(this.map.value, viewModelScope) { floorFilterSelection ->
            when (floorFilterSelection.type) {
                is FloorFilterSelection.Type.FloorSite -> {
                    val floorFilterSelectionType =
                        floorFilterSelection.type as FloorFilterSelection.Type.FloorSite
                    floorFilterSelectionType.site.geometry?.let {
                        this.setViewpoint(Viewpoint(it))
                    }
                }

                is FloorFilterSelection.Type.FloorFacility -> {
                    val floorFilterSelectionType =
                        floorFilterSelection.type as FloorFilterSelection.Type.FloorFacility
                    floorFilterSelectionType.facility.geometry?.let {
                        this.setViewpoint(Viewpoint(it))
                    }
                }

                else -> {}
            }
        }
}
