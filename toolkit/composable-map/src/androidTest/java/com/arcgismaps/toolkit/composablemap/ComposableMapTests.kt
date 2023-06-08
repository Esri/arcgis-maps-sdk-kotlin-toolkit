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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

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
    private val flowProducer = UUID.randomUUID()

    override val map: MutableStateFlow<ArcGISMap> = MutableStateFlow(map)
    override val insets: MutableStateFlow<MapInsets> = MutableStateFlow(insets)

    override val viewpoint: MutableStateFlow<FlowData<Viewpoint?>> =  MutableStateFlow(FlowData(null, flowProducer))
    override val mapRotation: MutableStateFlow<FlowData<Double>> =  MutableStateFlow(FlowData(0.0, flowProducer))

    override fun onMapRotationChanged(rotation: Double, flowProducer: UUID?) {
        mapRotation.value = FlowData(rotation, flowProducer)
    }

    override fun onMapViewpointChanged(viewpoint: Viewpoint, flowProducer: UUID?) {
        this.viewpoint.value = FlowData(viewpoint, flowProducer)
    }

    override fun setViewpoint(viewpoint: Viewpoint) {
        this.viewpoint.value = FlowData(viewpoint, flowProducer)
    }
}
