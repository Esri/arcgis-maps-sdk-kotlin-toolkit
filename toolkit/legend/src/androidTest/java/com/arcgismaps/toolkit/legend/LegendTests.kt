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

package com.arcgismaps.toolkit.legend

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for Legend.
 *
 * @since 200.7.0
 */
class LegendTests {
    private val sanDiegoShortlist = "1966ef409a344d089b001df85332608f"
    private val map: ArcGISMap = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            sanDiegoShortlist
        )
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setContent() = runTest {
        val legendUsageScenarios = LegendUsageScenarios()
        composeTestRule.setContent {
            val basemap = map.basemap.value
            legendUsageScenarios.MapViewWithLegendInBottomSheet(
                map
            ) {
                Legend(
                    map.operationalLayers,
                    basemap,
                    296326.0565949112)
            }
        }
    }

    /**
     * Given a Legend composable
     * When it is rendered
     * Then the top level Column exists
     *
     * @since 200.7.0
     */
    @Test
    fun testLegendColumn() {
        val legend = composeTestRule.onNodeWithContentDescription(context.getString(R.string.legend_component))
        legend.assertIsDisplayed()
    }
}
