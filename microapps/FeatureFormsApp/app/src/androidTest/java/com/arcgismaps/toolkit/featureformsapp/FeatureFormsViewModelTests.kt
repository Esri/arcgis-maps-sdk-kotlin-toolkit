package com.arcgismaps.toolkit.featureformsapp

import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapData
import com.arcgismaps.toolkit.composablemap.MapInterface
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FeatureFormsViewModelTests {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Given a ComposableMap
     * When it is tapped
     * Then the MapView's onSingleTapConfirmed Flow can be collected upon in the model's viewLogic method.
     */
    @Test
    fun testSingleTapIsConsumed() = runTest {
        val map = MapData(ArcGISMap())
        val mockMapInterface = MockMapInterface(map)
        composeTestRule.setContent {
            ComposableMap(mapInterface = mockMapInterface) {
                Card(modifier = Modifier.semantics {  }) {}
            }
        }

        composeTestRule.onNode(hasContentDescription("MapContainer")).performClick()
        mockMapInterface.onClick.takeWhile {
            it == null
        }.collect()
        assertNotNull(mockMapInterface.onClick.value)
    }
}

class MockMapInterface(map : MapData) : MapInterface {
    private val _mapData = MutableStateFlow(map)
    override val mapData: StateFlow<MapData> = _mapData.asStateFlow()
    val onClick: MutableStateFlow<Unit?> = MutableStateFlow(null)
    context(MapView, CoroutineScope) override fun viewLogic() {
        launch {
            onSingleTapConfirmed.collect {
                onClick.emit(Unit)
            }
        }
    }
}