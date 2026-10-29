/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arcgismaps.Color
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewer
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MarkerSelectionTests {
    @get:Rule
    val compose = createComposeRule()
    private lateinit var state: OrientedImageryViewerState

    private fun showViewer() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            state = remember {
                OrientedImageryViewerState(
                    OrientedImageryLayer("https://example.com/FeatureServer/0"), GraphicsOverlay(), scope
                )
            }
            MaterialTheme { OrientedImageryViewer(state) }
        }
    }

    private fun openSelection() {
        compose.onNodeWithText("Markers").performScrollTo().performClick()
        compose.onNodeWithText("Select Marker").performClick()
        compose.onNodeWithText("Marker Selection").assertIsDisplayed()
    }

    @Test
    fun selectingEachSymbolClosesPanelAndUpdatesCurrentSymbol() {
        showViewer()
        listOf("Violet square", "Yellow triangle", "Orange diamond").forEachIndexed { index, description ->
            openSelection()
            compose.onNodeWithContentDescription(description).performClick()
            compose.onNodeWithText("Marker Selection").assertDoesNotExist()
            compose.runOnIdle {
                val expected = MarkerSymbols.symbols[index]
                assertEquals(MarkerSymbols.options[index].id, state.selectedMarkerOptionId)
                assertNotSame(expected, state.currentMarkerSymbol)
                assertEquals(expected.style, state.currentMarkerSymbol.style)
                assertEquals(expected.color, state.currentMarkerSymbol.color)
                assertEquals(12f, state.currentMarkerSymbol.size)
                assertEquals(Color.black, state.currentMarkerSymbol.outline!!.color)
                assertEquals(1f, state.currentMarkerSymbol.outline!!.width)
                assertEquals(SimpleLineSymbolStyle.Solid, state.currentMarkerSymbol.outline!!.style)
                assertFalse(state.isMarkingLocation)
            }
            openSelection()
            compose.onNodeWithContentDescription(description).assertIsSelected()
            compose.onNodeWithContentDescription("Back to marker tools").performClick()
        }
    }

    @Test
    fun defaultAndCustomSymbolsHaveNoPaletteIdentity() {
        showViewer()
        compose.runOnIdle { assertNull(state.selectedMarkerOptionId) }
        openSelection()
        listOf("Violet square", "Yellow triangle", "Orange diamond").forEach {
            compose.onNodeWithContentDescription(it).assertIsNotSelected()
        }
        compose.runOnIdle {
            state.selectMarkerOption(MarkerOptionId.YellowTriangle)
            // A custom symbol with the same style must not inherit a palette option's identity.
            val customSymbol = MarkerSymbols.symbols.first().clone().apply { color = Color.black }
            state.selectMarkerSymbol(customSymbol)
            assertNull(state.selectedMarkerOptionId)
            assertEquals(customSymbol, state.currentMarkerSymbol)
        }
        openSelection()
        compose.onNodeWithContentDescription("Violet square").assertIsNotSelected()
        compose.onNodeWithContentDescription("Yellow triangle").assertIsNotSelected()
    }

    @Test
    fun selectionIdentityDoesNotDependOnSymbolStyle() {
        showViewer()
        compose.runOnIdle {
            state.selectMarkerOption(MarkerOptionId.OrangeDiamond)
            // Even when styles match, only the explicitly selected ID should be highlighted.
            state.currentMarkerSymbol.style = MarkerSymbols.symbols.first().style
        }
        openSelection()
        compose.onNodeWithContentDescription("Orange diamond").assertIsSelected()
        compose.onNodeWithContentDescription("Violet square").assertIsNotSelected()
    }

    @Test
    fun backAndCloseDismissWithoutChangingSymbol() {
        showViewer()
        val initialSymbol = compose.runOnIdle { state.currentMarkerSymbol }
        listOf("Back to marker tools", "Close").forEach { description ->
            openSelection()
            // The viewer also has a Close button; use the panel's navigation button for Back,
            // and the last Close node for the panel's Close button.
            if (description == "Close") {
                compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(description))[1].performClick()
            } else compose.onNodeWithContentDescription(description).performClick()
            compose.onNodeWithText("Marker Selection").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(initialSymbol, state.currentMarkerSymbol)
                assertNull(state.selectedMarkerOptionId)
            }
        }
    }

    @Test
    fun resetClosesPanelAndRetainsSelectedAppearance() {
        showViewer()
        compose.runOnIdle { state.selectMarkerSymbol(MarkerSymbols.symbols.last()) }
        openSelection()
        compose.runOnIdle { state.resetAll() }
        compose.onNodeWithText("Marker Selection").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(MarkerSymbols.symbols.last(), state.currentMarkerSymbol)
            assertEquals(MarkerOptionId.OrangeDiamond, state.selectedMarkerOptionId)
        }
    }
}
