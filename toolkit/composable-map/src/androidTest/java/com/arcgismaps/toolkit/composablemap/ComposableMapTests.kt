package com.arcgismaps.toolkit.composablemap

import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import com.arcgismaps.mapping.ArcGISMap
import org.junit.Rule
import org.junit.Test

class ComposableMapTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposableMapLayout() {
        val mockMapInterface = MapInterface(ArcGISMap())

        composeTestRule.setContent {
            ComposableMap(mapInterface = mockMapInterface) {
                Card(modifier = Modifier.semantics { contentDescription = "Card" }) {}
            }
        }
        val mapContainerSemanticLabel = "MapContainer"
        val mapViewSemanticLabel = "MapView"
        val contentSemanticLabel = "Content"

        val mapContainer = composeTestRule.onNodeWithContentDescription(mapContainerSemanticLabel)
        mapContainer.assertIsDisplayed()
        mapContainer.onChildAt(0).assert(hasContentDescription(mapViewSemanticLabel))
        mapContainer.onChildAt(1).assert(hasContentDescription(contentSemanticLabel))

        composeTestRule.onNodeWithContentDescription("Card")
            .assert(hasParent(hasContentDescription(contentSemanticLabel)))
    }
}
