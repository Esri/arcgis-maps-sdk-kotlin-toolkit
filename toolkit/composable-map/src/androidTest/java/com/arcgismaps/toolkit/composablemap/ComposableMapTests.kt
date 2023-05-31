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
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

class ComposableMapTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposableMapLayout() {
        val mockMapInterface = MockMapInterface(ArcGISMap())

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

class MockMapInterface(
    map: ArcGISMap,
    insets: MapInsets = MapInsets()
) : MapInterface {
    private val _map: MutableStateFlow<ArcGISMap> = MutableStateFlow(map)
    override val map: StateFlow<ArcGISMap> = _map.asStateFlow()

    private val _insets: MutableStateFlow<MapInsets> = MutableStateFlow(insets)
    override val insets: StateFlow<MapInsets> = _insets.asStateFlow()

    private val _currentViewpoint: MutableStateFlow<Viewpoint?> = MutableStateFlow(null)
    override val currentViewpoint: StateFlow<Viewpoint?> = _currentViewpoint.asStateFlow()

    override fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {}

    fun setViewpoint(viewpoint: Viewpoint) {
        _currentViewpoint.update { viewpoint }
    }
}
