package com.arcgismaps.toolkit.composablemap

import android.view.View
import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.espresso.Espresso
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.BuildConfig
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hamcrest.Matcher
import org.junit.Rule

import org.junit.Test

class ComposableMapTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposableMapLayout() {
        val map = MapData(ArcGISMap())
        val mockMapInterface = MockMapInterface(map)
        composeTestRule.setContent {
            ComposableMap(mapInterface = mockMapInterface) {
                Card(modifier = Modifier.semantics {  }) {}
            }
        }
        composeTestRule.onRoot().printToLog("TAG")
        composeTestRule.onNode(hasContentDescription("MapContainer")).performClick()
    }

    @Test
    fun testComposableMapInterface() {

    }
}

class MockMapInterface(map : MapData) : MapInterface {
    private val _mapData = MutableStateFlow(map)
    override val mapData: StateFlow<MapData> = _mapData.asStateFlow()

    override fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) { }
}