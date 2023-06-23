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
        val duplex = Duplex.Read

        composeTestRule.setContent {
            val state by mapFlow.collectAsState(duplex = duplex)
            Text(state.toString())
        }

        composeTestRule.onNodeWithText("0").assertExists()
        mapFlow.setValue(100, duplex)
        // assertion triggers a recomposition
        composeTestRule.onNodeWithText("100").assertExists()
    }

    @Test
    fun testStateInWriteChannel() {
        val mapFlow = MutableMapFlow(0)
        val duplex = Duplex.Write

        composeTestRule.setContent {
            val state by mapFlow.collectAsState(duplex = duplex)
            Text(state.toString())
        }

        composeTestRule.onNodeWithText("0").assertExists()
        mapFlow.setValue(100, duplex)
        // assertion triggers a recomposition
        composeTestRule.onNodeWithText("100").assertExists()
    }
}
