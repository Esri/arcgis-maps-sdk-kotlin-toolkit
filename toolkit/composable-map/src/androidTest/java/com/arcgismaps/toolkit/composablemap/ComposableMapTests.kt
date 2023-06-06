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

    lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposableMapLayout() {
        val mockMapInterface = MockMapInterface(ArcGISMap(), scope = scope)

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
    insets: MapInsets = MapInsets(),
    private val scope: CoroutineScope
) : MapInterface {
    private val flowProducer = UUID.randomUUID()

    override val map: MutableStateFlow<ArcGISMap> = MutableStateFlow(map)
    override val insets: MutableStateFlow<MapInsets> = MutableStateFlow(insets)

    override val viewpoint: MutableStateFlow<FlowData<Viewpoint?>> =  MutableStateFlow(FlowData(null, flowProducer))
    override val mapRotation: MutableStateFlow<FlowData<Double>> =  MutableStateFlow(FlowData(0.0, flowProducer))

    override suspend fun onMapRotationChanged(rotation: Double, flowProducer: UUID?) {
        mapRotation.emit(FlowData(rotation, flowProducer))
    }

    override suspend fun onMapViewpointChanged(viewpoint: Viewpoint, flowProducer: UUID?) {
        scope.launch {
            this@MockMapInterface.viewpoint.emit(FlowData(viewpoint, flowProducer))
        }
    }

    override fun setViewpoint(viewpoint: Viewpoint) {
        scope.launch {
            this@MockMapInterface.viewpoint.emit(FlowData(viewpoint, flowProducer))
        }
    }
}
