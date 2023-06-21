package com.arcgismaps.toolkit.composablemap

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MapFlowStateTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testStateInReadChannel() {
        val mapFlow = MutableMapFlow(0)
        val channel = Channel.Read

        composeTestRule.setContent {
            val state by mapFlow.collectAsState(channel = channel)
            Text(state.toString())
        }

        composeTestRule.onNodeWithText("0").assertExists()
        mapFlow.setValue(100, channel)
        // assertion triggers a recomposition
        composeTestRule.onNodeWithText("100").assertExists()
    }

    @Test
    fun testStateInWriteChannel() {
        val mapFlow = MutableMapFlow(0)
        val channel = Channel.Write

        composeTestRule.setContent {
            val state by mapFlow.collectAsState(channel = channel)
            Text(state.toString())
        }

        composeTestRule.onNodeWithText("0").assertExists()
        mapFlow.setValue(100, channel)
        // assertion triggers a recomposition
        composeTestRule.onNodeWithText("100").assertExists()
    }
}
