/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.offline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests for OfflineMapAreas.
 *
 * @since 200.8.0
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class OfflineMapAreasTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Test tags
    private val offlineMapAreasContainerTag = "OfflineMapAreasContainerLayout"
    private val downloadButtonTag = "DownloadButton"
    private val stopDownloadButtonTag = "StopDownloadButton"
    private val cancelButtonTag = "CancelButton"
    private val openButtonTag = "OpenButton"
    private val mapAreaListItemTag = "MapAreaListItem"
    private val removeButtonTag = "RemoveDownloadButton"
    private val addMapAreaButtonTag = "AddMapAreaButton"
    private val mapViewSelector = "OnDemandMapViewSelector"

    // Timeout for a download
    private val downloadTimeoutMillis = 60000L

    /**
     * Given the OfflineMapAreas UI is displayed and initialized in Preplanned mode,
     * When a download button for a preplanned map area is clicked, the download starts, and then finishes,
     * Then the downloaded map area can be opened and subsequently removed.
     *
     * @since 200.8.0
     */
    @Test
    fun testPreplannedWorkflow() = runTest {
        val mapViewModel = MapViewModel(OfflineMapMode.Preplanned)
        composeTestRule.setContent { OfflineScenario(mapViewModel) }
        // Verify that the OfflineMapAreas UI container is displayed
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(offlineMapAreasContainerTag))
        // Wait until the OfflineMapState is initialized
        composeTestRule.waitUntil(
            condition = {
                mapViewModel.offlineMapState.initializationStatus.value == InitializationStatus.Initialized
            },
            timeoutMillis = downloadTimeoutMillis
        )
        // Assert that the initialization status is Initialized
        assert(mapViewModel.offlineMapState.initializationStatus.value == InitializationStatus.Initialized)
        // Assert that the Offline Map Mode is set to Preplanned
        assert(mapViewModel.offlineMapState.mode == OfflineMapMode.Preplanned)
        advanceUntilIdle()
        // Wait until all 4 "DownloadButton" instances are visible
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(downloadButtonTag).fetchSemanticsNodes().size == 4
        }
        advanceUntilIdle()
        // Click the first available "DownloadButton"
        composeTestRule.onAllNodesWithTag(downloadButtonTag)[0].performClick()
        // Wait for the "CancelDownloadButton" to appear, indicating download started
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(stopDownloadButtonTag))
        advanceUntilIdle()
        // Wait for the "CancelDownloadButton" to transform into an "OpenButton"
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasTestTag(openButtonTag),
            timeoutMillis = downloadTimeoutMillis
        )
        // Click on the first Map Area list item to view details/options
        composeTestRule.onAllNodesWithTag(mapAreaListItemTag)[0].performClick()
        advanceUntilIdle()
        // Wait for the "RemoveDownloadButton" to become visible
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(removeButtonTag))
        // Click the "RemoveDownloadButton" to delete the downloaded map area
        composeTestRule.onNodeWithTag(removeButtonTag).performClick()
        advanceUntilIdle()
        // Verify that the "RemoveDownloadButton" is no longer present
        composeTestRule.onNodeWithTag(removeButtonTag).assertDoesNotExist()
    }

    /**
     * Given the OfflineMapAreas UI is displayed and initialized in On-Demand mode,
     * When the "add map area" button is clicked, and the map view is double-tapped multiple times to zoom,
     * Then the download button is clicked to initiate an on-demand download, and the download job is handled.
     *
     * @since 200.8.0
     */
    @Test
    fun testOnDemandWorkflow() = runTest {
        val mapViewModel = MapViewModel(OfflineMapMode.OnDemand)
        composeTestRule.setContent { OfflineScenario(mapViewModel) }
        // Verify that the OfflineMapAreas UI container is displayed
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(offlineMapAreasContainerTag))
        // Wait until the OfflineMapState is initialized
        composeTestRule.waitUntil(
            condition = {
                mapViewModel.offlineMapState.initializationStatus.value == InitializationStatus.Initialized
            },
            timeoutMillis = downloadTimeoutMillis
        )
        // Assert that the initialization status is Initialized
        assert(mapViewModel.offlineMapState.initializationStatus.value == InitializationStatus.Initialized)
        // Assert that the Offline Map Mode is set to OnDemand
        assert(mapViewModel.offlineMapState.mode == OfflineMapMode.OnDemand)
        advanceUntilIdle()
        // Wait until add map area button is visible
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(addMapAreaButtonTag))
        // Click the add map area button to open the selector sheet
        composeTestRule.onNodeWithTag(addMapAreaButtonTag).performClick()
        advanceUntilIdle()
        // Wait until download map area button is visible
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(downloadButtonTag))
        // Get the size of the MapView to calculate the tap coordinates
        val mapViewNode = composeTestRule.onNodeWithTag(mapViewSelector).fetchSemanticsNode()
        // Calculate the center
        val centerX = mapViewNode.positionOnScreen.x + (mapViewNode.size.width / 2)
        val centerY = mapViewNode.positionOnScreen.y + (mapViewNode.size.height / 2)
        // Perform 10 double taps
        repeat(10) {
            composeTestRule.onNodeWithTag(mapViewSelector)
                .performTouchInput {
                    // Ensure the doubleTap function is called within the performTouchInput scope
                    doubleClick(position = Offset(x = centerX, y = centerY))
                }
            // Advance the clock by 2 seconds to simulate real-time delay for animation
            composeTestRule.mainClock.advanceTimeBy(5000L)
            advanceUntilIdle()
        }
        // Click the download button
        composeTestRule.onNodeWithTag(downloadButtonTag).performClick()
        advanceUntilIdle()
        // Wait for the on-demand download job to fail, as no api key was provided
        composeTestRule.waitUntilExactlyOneExists(
            hasTestTag(cancelButtonTag),
            timeoutMillis = downloadTimeoutMillis
        )
        // Click the remove the failed to download map area button
        composeTestRule.onNodeWithTag(cancelButtonTag).performClick()
        advanceUntilIdle()
        // Verify that the area has been removed and the add map area button is visible
        composeTestRule.waitUntilDoesNotExist(hasTestTag(cancelButtonTag))
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(addMapAreaButtonTag))
    }
}
