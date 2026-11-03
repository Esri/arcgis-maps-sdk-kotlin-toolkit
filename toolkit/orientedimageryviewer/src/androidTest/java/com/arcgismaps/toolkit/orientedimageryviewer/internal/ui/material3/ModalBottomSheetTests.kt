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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.material3

import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ModalBottomSheetTests {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun swipeDismissesFullyExpandedSheetAndAllowsReopening() {
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                var open by rememberSaveable { mutableStateOf(true) }
                Button(onClick = { open = true }) { Text("Open sheet") }
                if (open) {
                    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        sheetState = state,
                        onDismissRequest = { dismissals++; open = false }
                    ) {
                        Text("Sheet content", Modifier.fillMaxWidth().height(240.dp))
                    }
                }
            }
        }
        compose.onNodeWithText("Sheet content").assertIsDisplayed()
            .performTouchInput { swipeDown() }
        compose.onNodeWithText("Sheet content").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissals) }
        compose.onNodeWithText("Open sheet").performClick()
        compose.onNodeWithText("Sheet content").assertIsDisplayed()
    }

    @Test
    fun backUsesLatestDismissCallbackAfterRecomposition() {
        val revision = mutableStateOf(0)
        val dismissals = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                val currentRevision = revision.value
                ModalBottomSheet(
                    onDismissRequest = { dismissals.add(currentRevision) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Text("Revision $currentRevision")
                }
            }
        }
        compose.onNodeWithText("Revision 0").assertIsDisplayed()
        compose.runOnIdle { revision.value = 1 }
        compose.onNodeWithText("Revision 1").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { dismissals.isNotEmpty() }
        compose.runOnIdle { assertEquals(listOf(1), dismissals) }
    }

    @Test
    fun scrimDismissesExactlyOnce() {
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                var open by rememberSaveable { mutableStateOf(true) }
                if (open) {
                    ModalBottomSheet(
                        onDismissRequest = { dismissals++; open = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) { Text("Sheet content") }
                }
            }
        }
        compose.onNodeWithText("Sheet content").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close sheet").performClick()
        compose.onNodeWithText("Sheet content").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun openSheetAndHoistedSelectionSurviveRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                var open by rememberSaveable { mutableStateOf(false) }
                // Toolbar choices are hoisted; StateRestorationTester restores this composition,
                // not a separate Android dialog window's SavedStateRegistry.
                var selected by rememberSaveable { mutableStateOf(false) }
                Button(onClick = { open = true }) { Text("Open sheet") }
                if (open) {
                    ModalBottomSheet(
                        onDismissRequest = { open = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        Button(onClick = { selected = !selected }) {
                            Text(if (selected) "Selected" else "Not selected")
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Open sheet").performClick()
        compose.onNodeWithText("Not selected").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Selected").assertIsDisplayed()
    }
}
