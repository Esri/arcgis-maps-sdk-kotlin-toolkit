package com.arcgismaps.toolkit.featureformsapp

import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the FeatureForms ViewModel's correctness.
 */
class FeatureFormsViewModelTests {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Given a ComposableMap
     * When it is tapped
     * Then the MapView's onSingleTapConfirmed Flow can be collected upon in the model's viewLogic method.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSingleTapIsConsumed() = runTest {
        val map = ArcGISMap()
        val mockMapInterface = MockMapInterface(map)
        composeTestRule.setContent {
            ComposableMap(mapInterface = mockMapInterface) {
                Card(modifier = Modifier.semantics {  }) {}
            }
        }

        composeTestRule.onNode(hasContentDescription("MapContainer")).performClick()
        // if this exits, the assertion is met.
        mockMapInterface.onClick.first {
            it != null
        }
    }
}

class MockMapInterface(map : ArcGISMap) : MapInterface {
    private val _mapData = MutableStateFlow(map)
    override val map: StateFlow<ArcGISMap> = _mapData.asStateFlow()
    
    override val insets: StateFlow<MapInsets> = MutableStateFlow(MapInsets()).asStateFlow()

    override val currentViewpoint: StateFlow<Viewpoint?> = MutableStateFlow(null).asStateFlow()

    val onClick: MutableStateFlow<Unit?> = MutableStateFlow(null)
    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            onClick.emit(Unit)
        }
    }
}
