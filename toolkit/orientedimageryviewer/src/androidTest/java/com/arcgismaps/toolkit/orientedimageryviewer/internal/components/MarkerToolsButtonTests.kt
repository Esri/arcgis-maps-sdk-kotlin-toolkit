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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun showButton() {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    MarkerToolsButton(
                        isMarkingLocation = isMarkingLocation.value,
                        onAddMarker = {
                            actions.add("add")
                            isMarkingLocation.value = !isMarkingLocation.value
                        },
                        onSelectMarker = { actions.add("select") },
                        onClearAllMarkers = { actions.add("clear") }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(64.dp)
                            .size(48.dp)
                            .testTag("outside")
                    )
                }
            }
        }
    }

    // Route through Android's window manager to exercise popup outside-touch handling.
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

    private fun assertMenuClosed() {
        compose.onNodeWithText("Mark Location").assertDoesNotExist()
        compose.onNodeWithText("Select Marker").assertDoesNotExist()
        compose.onNodeWithText("Clear All Markers").assertDoesNotExist()
    }

    @Test
    fun everyTapExpandsMenu() {
        showButton()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        assertMenuClosed()

        compose.onNodeWithText("Markers").tapOnScreen()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        compose.onNodeWithText("Mark Location").assertIsDisplayed()
        compose.onNodeWithText("Select Marker").assertIsDisplayed()
        compose.onNodeWithText("Clear All Markers").assertIsDisplayed()

        compose.onNodeWithText("Markers").tapOnScreen()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        compose.onNodeWithText("Mark Location").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun eachActionInvokesOnlyItsHandlerAndClosesMenu() {
        showButton()
        val expected = mutableListOf<String>()
        listOf("Mark Location" to "add", "Select Marker" to "select", "Clear All Markers" to "clear")
            .forEach { (label, action) ->
                compose.onNodeWithText("Markers").performClick()
                compose.onNodeWithText(label).performClick()
                expected.add(action)
                assertMenuClosed()
                compose.onNodeWithText("Markers").assertIsSelected()
                compose.runOnIdle { assertEquals(expected, actions) }
            }
    }

    @Test
    fun outsideTapClosesMenuAndNextTapReopensIt() {
        showButton()
        compose.onNodeWithText("Markers").tapOnScreen()
        compose.onNodeWithTag("outside").tapOnScreen()
        assertMenuClosed()
        compose.onNodeWithText("Markers").assertIsNotSelected()

        compose.onNodeWithText("Markers").tapOnScreen()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        compose.onNodeWithText("Mark Location").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun backDismissesMenuAndNextTapReopensIt() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Mark Location").assertIsDisplayed()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        assertMenuClosed()
        compose.onNodeWithText("Markers").assertIsNotSelected()
        compose.onNodeWithText("Markers").performClick()
        compose.onNodeWithText("Mark Location").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
    }

    @Test
    fun switchTogglesMarkingMode() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNode(isToggleable()).assertIsOff().performClick()
        assertMenuClosed()
        compose.onNodeWithText("Markers").assertIsSelected().performClick()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        assertMenuClosed()
        compose.onNodeWithText("Markers").assertIsNotSelected().performClick()
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
}

