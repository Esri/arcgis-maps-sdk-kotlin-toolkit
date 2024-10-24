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

package com.arcgismaps.toolkit.utilitynetworks

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TraceToolTests : TraceToolTestRunner(
    url = "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
    itemId = "471eb0bf37074b1fbb972b1da70fb310"
) {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setContent() = runTest {
        composeTestRule.setContent {
            Trace(
                traceState = traceState
            )
        }
    }

    /**
     * Given a Trace composable
     * When it is rendered
     * Then the top level Surface exists
     *
     * @since 200.6.0
     */
    @Test
    fun testTraceToolSurface() {
        val surface = composeTestRule.onNodeWithContentDescription("Trace component")
        surface.assertExists("the base surface of the Trace tool composable does not exist")
    }
}
