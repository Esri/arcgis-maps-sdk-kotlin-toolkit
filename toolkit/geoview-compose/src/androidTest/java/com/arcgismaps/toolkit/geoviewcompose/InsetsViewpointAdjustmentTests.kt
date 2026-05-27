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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.InsetsViewpointAdjustmentType
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.sharedtestutilities.assertIsLoaded
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the MapView.insetsViewpointAdjustment API on the composable MapView.
 *
 * @since 300.1.0
 */
class InsetsViewpointAdjustmentTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() = runTest {
        ArcGISEnvironment.applicationContext = InstrumentationRegistry.getInstrumentation().context
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
    }

    /**
     * Given a [MapView] with location display inactive
     * When the `insets` are set and `insetsViewpointAdjustment` is set to preserve center
     * Then the map position is maintained before and after the insets are applied
     *
     * @since 300.1.0
     */
    @Test
    fun testInsetsViewpointAdjustment() = runBlocking {
        // Create and load a map
        val mapViewProxy = MapViewProxy()
        val map = ArcGISMap(SpatialReference.wgs84())
        map.assertIsLoaded()

        // State to hold the insets adjustment type
        var insetsViewpointAdjustmentType by mutableStateOf<InsetsViewpointAdjustmentType>(
            InsetsViewpointAdjustmentType.NoAdjustment
        )
        // State to hold the insets
        var insets by mutableStateOf(PaddingValues(0.dp, 0.dp, 0.dp, 0.dp))

        // Initial viewpoint to set on the map
        val viewpoint = Viewpoint(
            Point(
                -117.9190,
                33.8121,
                SpatialReference.wgs84()
            ), 170000.0
        )
        // Create a LocationDisplay
        val locationDisplay = LocationDisplay()
        // Store the viewpoint received from the MapView
        var adjustedViewpoint: Viewpoint? = null
        // Store the draw status of the MapView
        var drawStatus: DrawStatus? = null

        // Set the content of the MapView composable
        composeTestRule.setContent {
            MapView(
                arcGISMap = map,
                modifier = Modifier.fillMaxSize(),
                mapViewProxy = mapViewProxy,
                insets = insets,
                locationDisplay = locationDisplay,
                insetsViewpointAdjustment = insetsViewpointAdjustmentType,
                onViewpointChangedForCenterAndScale = {
                    adjustedViewpoint = it
                },
                onDrawStatusChanged = {
                    drawStatus = it
                }
            )
        }

        // Wait for the map to draw
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            drawStatus == DrawStatus.Completed
        }
        // Assert that the location display is stopped
        assertThat(locationDisplay.dataSource.status.value).isEqualTo(LocationDataSourceStatus.Stopped)

        // Set the initial viewpoint on the map
        mapViewProxy.setViewpoint(viewpoint)
        composeTestRule.waitForIdle()

        // Set the insets viewpoint adjustment type to PreserveCenter and wait for recomposition
        composeTestRule.runOnIdle {
            insetsViewpointAdjustmentType = InsetsViewpointAdjustmentType.PreserveCenter
        }
        composeTestRule.waitForIdle()
        // Set the insets to have a left padding and wait for recomposition
        composeTestRule.runOnIdle {
            insets = PaddingValues(50.dp, 0.dp, 0.dp, 0.dp)
        }
        composeTestRule.waitForIdle()

        // Wait until the adjusted viewpoint is updated
        composeTestRule.waitUntil {
            adjustedViewpoint != null
        }
        assertThat(adjustedViewpoint).isNotNull()
        // Verify that the target geometry of the viewpoint remains the same before and after
        // applying insets with preserve center adjustment
        assertThat(
            viewpoint.targetGeometry.equals(
                adjustedViewpoint!!.targetGeometry,
                tolerance = 1e-8
            )
        ).isTrue()
    }
}
