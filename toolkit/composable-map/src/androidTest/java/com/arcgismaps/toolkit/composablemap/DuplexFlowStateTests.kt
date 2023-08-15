/*
 *
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

package com.arcgismaps.toolkit.composablemap

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DuplexFlowStateTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testStateInReadFlow() {
        val duplexFlow = MutableDuplexFlow(0)
        val flowType = DuplexFlow.Type.Read

        composeTestRule.setContent {
            val state by duplexFlow.collectAsState(flowType = flowType)
            Text(state.toString())
        }

        composeTestRule.onNodeWithText("0").assertExists()
        duplexFlow.setValue(100, flowType)
        // assertion triggers a recomposition
        composeTestRule.onNodeWithText("100").assertExists()
    }

    @Test
    fun testStateInWriteFlow() {
        val duplexFlow = MutableDuplexFlow(0)
        val flowType = DuplexFlow.Type.Write

        composeTestRule.setContent {
            val state by duplexFlow.collectAsState(flowType = flowType)
            Text(state.toString())
        }

        composeTestRule.onNodeWithText("0").assertExists()
        duplexFlow.setValue(100, flowType)
        // assertion triggers a recomposition
        composeTestRule.onNodeWithText("100").assertExists()
    }
}
