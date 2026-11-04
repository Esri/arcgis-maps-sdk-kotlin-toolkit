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

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.orientedimageryviewer.GeoViewInteractionStatus
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewer
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ModeSelectionButtonTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: OrientedImageryViewerState

    private fun showButton() {
        compose.setContent {
            state = androidx.compose.runtime.remember {
                OrientedImageryViewerState(
                    MapViewProxy(),
                    OrientedImageryLayer("https://example.com/FeatureServer/0"),
                    GraphicsOverlay(),
                    CoroutineScope(Dispatchers.Main.immediate)
                )
            }
            MaterialTheme {
                OrientedImageryViewer(state, showCloseIcon = false)
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    @Test
    fun opensSheetWithFindImagesSelected() {
        showButton()
        compose.onNodeWithText("Mode Selection").assertDoesNotExist()
        compose.onNodeWithText("Mode").assertIsEnabled().assertIsSelected().performClick()
        compose.onNodeWithText("Mode Selection").assertIsDisplayed()
        compose.onNodeWithText("Find Images").assertIsSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
    }

    @Test
    fun modeChoicesAreMutuallyExclusiveAndCanBeToggledOff() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Display Image").performClick().assertIsSelected()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.DisplayImage, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Display Image").performClick().assertIsNotSelected()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.None, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Display Image").performClick().assertIsSelected()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.DisplayImage, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Find Images").performClick().assertIsSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Find Images").performClick().assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.None, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Find Images").performClick().assertIsSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Find Images").performClick().assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.None, state.geoViewInteractionStatus)
        }
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Mode").assertIsEnabled().assertIsNotSelected()
    }

    @Test
    fun closingAndReopeningPreservesSelectedMode() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Display Image").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Mode Selection").assertDoesNotExist()
        compose.onNodeWithText("Mode").assertIsSelected()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Display Image").assertIsSelected()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
    }

    @Test
    fun selectingImageModeUnchecksMarkerPlacement() {
        showButton()
        compose.onNodeWithText("Markers").performClick()
        compose.onNode(isToggleable()).assertIsOff().performClick()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.AddMarkers, state.geoViewInteractionStatus)
        }
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Mode").assertIsEnabled().assertIsNotSelected().performClick()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.onNodeWithText("Display Image").performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.DisplayImage, state.geoViewInteractionStatus)
        }
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Mode").assertIsSelected()
        compose.onNodeWithText("Markers").assertIsNotSelected().performClick()
        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun uncheckingMarkerPlacementKeepsNoneWhenNoImageOptionIsSelected() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Find Images").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Markers").performClick()
        compose.onNode(isToggleable()).assertIsOff().performClick()
        compose.onNodeWithText("Markers").assertIsSelected().performClick()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.None, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Mode").assertIsNotSelected()
    }

    @Test
    fun markerPlacementClearsImageSelectionsAndUncheckingKeepsThemCleared() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Display Image").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        repeat(2) {
            compose.onNodeWithText("Markers").performClick()
            compose.onNode(isToggleable()).assertIsOff().performClick()
            compose.runOnIdle {
                assertEquals(GeoViewInteractionStatus.AddMarkers, state.geoViewInteractionStatus)
            }
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Mode").assertIsNotSelected().performClick()
            compose.onNodeWithText("Find Images").assertIsNotSelected()
            compose.onNodeWithText("Display Image").assertIsNotSelected()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Markers").performClick()
            compose.onNode(isToggleable()).assertIsOn().performClick()
            compose.runOnIdle {
                assertEquals(GeoViewInteractionStatus.None, state.geoViewInteractionStatus)
            }
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Mode").assertIsNotSelected()
        }
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
    }

    @Test
    fun assigningImageModeUnselectsTheOtherOption() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.runOnIdle { state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage }
        compose.onNodeWithText("Display Image").assertIsSelected()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.runOnIdle { state.geoViewInteractionStatus = GeoViewInteractionStatus.FindImages }
        compose.onNodeWithText("Find Images").assertIsSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.runOnIdle { state.geoViewInteractionStatus = GeoViewInteractionStatus.AddMarkers }
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
        compose.runOnIdle { state.geoViewInteractionStatus = GeoViewInteractionStatus.None }
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
    }

    @Test
    fun resetRestoresDefaultSelectionsFromMarkerPlacement() {
        showButton()
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            state.addMarker()
            state.resetAll()
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Mode").assertIsSelected().performClick()
        compose.onNodeWithText("Find Images").assertIsSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected()
    }

    @Test
    fun modeButtonCanReopenWithNeitherModeSelected() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Find Images").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Mode").assertIsEnabled().assertIsNotSelected().performClick()
        compose.onNodeWithText("Find Images").assertIsNotSelected()
        compose.onNodeWithText("Display Image").assertIsNotSelected().performClick()
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.DisplayImage, state.geoViewInteractionStatus)
        }
    }

    @Test
    fun backDismissesSheetAndAllowsReopening() {
        showButton()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Mode Selection").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Mode Selection").assertDoesNotExist()
        compose.onNodeWithText("Mode").performClick()
        compose.onNodeWithText("Mode Selection").assertIsDisplayed()
    }
}
