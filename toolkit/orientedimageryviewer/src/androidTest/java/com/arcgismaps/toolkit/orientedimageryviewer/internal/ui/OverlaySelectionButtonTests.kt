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
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.GeoViewInteractionStatus
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewer
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OverlaySelectionButtonTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: OrientedImageryViewerState
    private lateinit var layer: OrientedImageryLayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun tearDown() {
        compose.runOnIdle { scope.cancel() }
    }

    private fun showViewer(imageCount: Int = 2) {
        layer = mockk()
        every { layer.name } returns "Overlay selection"
        every { layer.supportsSequentialNavigation } returns false
        every { layer.visibleFootprints } returns mutableListOf()
        compose.setContent {
            state = remember {
                OrientedImageryViewerState(
                    layer,
                    GraphicsOverlay(),
                    scope
                ) { mockk(relaxed = true) }
            }
            MaterialTheme {
                OrientedImageryViewer(state, showCloseIcon = false)
            }
        }
        compose.waitForIdle()
        if (imageCount > 0) loadImages(imageCount)
    }

    private fun loadImages(count: Int) {
        compose.runOnIdle {
            val images = List(count) {
                mockk<OrientedImage>().also { image ->
                    every { image.dataUri } returns null
                    every { image.type } returns OrientedImageType.Horizontal
                    every { image.geometry } returns Point(1.0, 2.0)
                    coEvery { image.load() } returns Result.success(Unit)
                }
            }
            coEvery { layer.searchImages(any(), any()) } returns Result.success(images)
            scope.launch { state.searchImages(Point(1.0, 2.0)).getOrThrow() }
        }
        compose.waitForIdle()
    }

    @Test
    fun opensWithThreeIndependentSwitchesAndKeepsSheetOpen() {
        showViewer()
        compose.onNodeWithText("Overlays").assertIsSelected().performClick()
        compose.onNodeWithText("Overlays Selection").assertIsDisplayed()
        compose.onAllNodes(isToggleable()).assertCountEquals(3)
        compose.onNodeWithText("Show Current Footprint").assertIsEnabled().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("Show Additional Footprints").assertIsEnabled().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Show Camera Locations").assertIsEnabled().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("Show Camera Locations").performClick().assertIsOn()
        compose.onNodeWithText("Show Additional Footprints").assertIsOn()
        compose.onNodeWithText("Show Camera Locations").assertIsOn()
        compose.onNodeWithText("Overlays Selection").assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(state.uiState.isActiveFootprintVisible)
            assertTrue(state.uiState.isAdditionalFootprintsVisible)
            assertTrue(state.uiState.isAdditionalCameraLocationsVisible)
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
        }
    }

    @Test
    fun closingAndReopeningPreservesVisibilityAndChipSelection() {
        showViewer()
        compose.onNodeWithText("Overlays").performClick()
        compose.onNodeWithText("Show Current Footprint").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Overlays Selection").assertDoesNotExist()
        compose.onNodeWithText("Overlays").assertIsSelected().performClick()
        compose.onNodeWithText("Show Current Footprint").assertIsOff()
        compose.onNodeWithText("Show Additional Footprints").assertIsOff()
        compose.onNodeWithText("Show Camera Locations").assertIsOn().performClick().assertIsOff()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Overlays").assertIsNotSelected()
    }

    @Test
    fun externalChangesAndResetUpdateOpenSheet() {
        showViewer()
        compose.onNodeWithText("Overlays").performClick()
        compose.runOnIdle {
            state.toggleActiveFootprint()
            state.toggleAdditionalFootprints()
            state.toggleAdditionalCameraLocations()
        }
        compose.onNodeWithText("Show Current Footprint").assertIsOff()
        compose.onNodeWithText("Show Additional Footprints").assertIsOn()
        compose.onNodeWithText("Show Camera Locations").assertIsOff()
        compose.runOnIdle { state.resetAll() }
        compose.onNodeWithText("Show Current Footprint").assertIsNotEnabled().assertIsOn()
        compose.onNodeWithText("Show Additional Footprints").assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText("Show Camera Locations").assertIsNotEnabled().assertIsOn()
        compose.onNodeWithText("Overlays Selection").assertIsDisplayed()
    }

    @Test
    fun availabilityChangesWhileSheetIsOpen() {
        showViewer(imageCount = 0)
        compose.onNodeWithText("Overlays").performClick()
        compose.onNodeWithText("Show Current Footprint").assertIsNotEnabled().assertIsOn()
        compose.onNodeWithText("Show Additional Footprints").assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText("Show Camera Locations").assertIsNotEnabled().assertIsOn()
        loadImages(1)
        compose.onNodeWithText("Show Current Footprint").assertIsEnabled()
        compose.onNodeWithText("Show Additional Footprints").assertIsNotEnabled()
        compose.onNodeWithText("Show Camera Locations").assertIsNotEnabled()
        loadImages(2)
        compose.onNodeWithText("Show Current Footprint").assertIsEnabled()
        // Available even though additional footprints have not yet been selected or created.
        compose.onNodeWithText("Show Additional Footprints").assertIsEnabled().assertIsOff()
        compose.onNodeWithText("Show Camera Locations").assertIsEnabled()
        loadImages(1)
        compose.onNodeWithText("Show Additional Footprints").assertIsNotEnabled()
        compose.onNodeWithText("Show Camera Locations").assertIsNotEnabled()
        loadImages(0)
        compose.onNodeWithText("Show Current Footprint").assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText("Show Additional Footprints").assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText("Show Camera Locations").assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText("Overlays Selection").assertIsDisplayed()
    }

    @Test
    fun disabledOptionsDoNotChangeSelections() {
        showViewer(imageCount = 0)
        compose.onNodeWithText("Overlays").performClick()
        compose.onNodeWithText("Show Current Footprint").assertIsNotEnabled().performClick().assertIsOn()
        compose.onNodeWithText("Show Additional Footprints").assertIsNotEnabled().performClick().assertIsOff()
        compose.onNodeWithText("Show Camera Locations").assertIsNotEnabled().performClick().assertIsOn()
    }

    @Test
    fun backDismissesSheetAndAllowsReopening() {
        showViewer()
        compose.onNodeWithText("Overlays").performClick()
        compose.onNodeWithText("Overlays Selection").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Overlays Selection").assertDoesNotExist()
        compose.onNodeWithText("Overlays").performClick()
        compose.onNodeWithText("Overlays Selection").assertIsDisplayed()
    }
}
