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

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MarkerToolsButtonTests {
    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<String>()
    private val isMarkingLocation = mutableStateOf(false)
    private val isSelecting = mutableStateOf(false)
    private val selectedOption = mutableStateOf<MarkerOptionId?>(null)

    private fun showButton(fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        MarkerToolsButton(
                            isMarkingLocation = isMarkingLocation.value,
                            onAddMarker = {
                                actions.add("add")
                                isMarkingLocation.value = !isMarkingLocation.value
                            },
                            onSelectMarker = {
                                actions.add("select")
                                isSelecting.value = true
                            },
                            onClearAllMarkers = { actions.add("clear") },
                            isMarkerSelectionVisible = isSelecting.value,
                            selectedOptionId = selectedOption.value,
                            onSelectOption = { selectedOption.value = it },
                            onDismissSelection = { isSelecting.value = false }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(64.dp)
                                .size(48.dp)
                                .testTag("outside")
                        )
                    }
                }
            }
        }
    }

    // Route through Android's window manager to exercise modal scrim handling.
    private fun SemanticsNodeInteraction.tapOnScreen() {
        val node = fetchSemanticsNode()
        val center = compose.runOnIdle {
            val coordinates = node.layoutInfo.coordinates
            coordinates.localToScreen(
                Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
            )
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), action, center.x, center.y, 0
            )
            try {
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
        }
        compose.waitForIdle()
    }

    private fun assertSheetClosed() {
        compose.onNodeWithText("Marker Tools").assertDoesNotExist()
        compose.onNodeWithText("Marker Selection").assertDoesNotExist()
        compose.onNodeWithText("Add Marker").assertDoesNotExist()
        compose.onNodeWithText("Select Marker").assertDoesNotExist()
        compose.onNodeWithText("Clear All Markers").assertDoesNotExist()
    }

    @Test
    fun chipOpensToolsAndCloseAllowsReopening() {
        showButton()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        assertSheetClosed()

        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Add Marker").assertIsDisplayed()
        compose.onNodeWithText("Select Marker").assertIsNotEnabled()
        compose.onNodeWithText("Clear All Markers").assertIsDisplayed()

        compose.onNodeWithContentDescription("Close").performClick()
        assertSheetClosed()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun actionsStayOpenAndSelectionIsEnabledOnlyWhileMarking() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Select Marker").assertIsNotEnabled()
        compose.onNodeWithText("Clear All Markers").assertIsEnabled().performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Add Marker").performClick()
        compose.onNode(isToggleable()).assertIsOn()
        compose.onNodeWithText("Select Marker").assertIsEnabled().performClick()
        compose.onNodeWithText("Marker Selection").assertIsDisplayed()
        compose.onNodeWithContentDescription("Yellow triangle").performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Clear All Markers").performClick()
        compose.onNodeWithText("Add Marker").performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.onNodeWithText("Select Marker").assertIsNotEnabled()
        compose.onNodeWithText("Clear All Markers").assertIsEnabled()
        compose.runOnIdle {
            assertEquals(MarkerOptionId.YellowTriangle, selectedOption.value)
            assertEquals(listOf("clear", "add", "select", "clear", "add"), actions)
        }
    }

    @Test
    fun outsideTapClosesSheetAndNextTapReopensIt() {
        showButton()
        compose.onNodeWithText("Markers").tapOnScreen()
        compose.onNodeWithTag("outside").tapOnScreen()
        assertSheetClosed()
        compose.onNodeWithText("Markers").assertIsNotSelected()

        compose.onNodeWithText("Markers").tapOnScreen()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        compose.onNodeWithText("Add Marker").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun backAndSwipeDoNotDismissTools() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Add Marker").assertIsDisplayed()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        compose.onNodeWithText("Marker Tools").assertIsDisplayed().performTouchInput { swipeDown() }
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithContentDescription("Drag handle").performTouchInput { click() }
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun switchTogglesMarkingMode() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Add Marker").assertIsOff().performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Select Marker").assertIsEnabled()
        compose.onNodeWithText("Add Marker").assertIsOn().performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Select Marker").assertIsNotEnabled()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle { assertEquals(listOf("add", "add"), actions) }
    }

    @Test
    fun switchReflectsExternalMarkingStateChanges() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle { isMarkingLocation.value = true }
        compose.onNode(isToggleable()).assertIsOn()
        compose.runOnIdle { isMarkingLocation.value = false }
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun selectionBackReturnsToToolsAndCloseDismissesBothPages() {
        isMarkingLocation.value = true
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Select Marker").performClick()
        compose.onNodeWithContentDescription("Back to marker tools").performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Select Marker").performClick()
        compose.onNodeWithText("Marker Selection").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(timeoutMillis = 5_000) { !isSelecting.value }
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
        compose.onNodeWithText("Select Marker").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        assertSheetClosed()
    }

    @Test
    fun outsideTapDismissesSelectionAndReopensAtTools() {
        isMarkingLocation.value = true
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Select Marker").performClick()
        compose.onNodeWithTag("outside").tapOnScreen()
        assertSheetClosed()
        compose.runOnIdle { assertEquals(false, isSelecting.value) }
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Marker Tools").assertIsDisplayed()
    }

    @Test
    fun toolsRemainScrollableWithLargeText() {
        showButton(fontScale = 5f)
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Clear All Markers").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("clear"), actions) }
        compose.onNodeWithText("Add Marker").performScrollTo().assertIsDisplayed()
    }
}

