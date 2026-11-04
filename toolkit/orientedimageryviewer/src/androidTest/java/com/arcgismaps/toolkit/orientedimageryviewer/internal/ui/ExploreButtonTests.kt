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
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
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

class ExploreButtonTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: OrientedImageryViewerState
    private lateinit var layer: OrientedImageryLayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun tearDown() {
        compose.runOnIdle { scope.cancel() }
    }

    private fun showViewer(supportsSequentialNavigation: Boolean = true, withImage: Boolean = true) {
        layer = mockk()
        every { layer.name } returns "Explore tests"
        every { layer.supportsSequentialNavigation } returns supportsSequentialNavigation
        every { layer.visibleFootprints } returns mutableListOf()
        compose.setContent {
            state = remember {
                OrientedImageryViewerState(MapViewProxy(), layer, GraphicsOverlay(), scope) { mockk(relaxed = true) }
            }
            MaterialTheme {
                OrientedImageryViewer(state, showCloseIcon = false)
            }
        }
        compose.waitForIdle()
        if (withImage) loadImage()
    }

    private fun loadImage() {
        compose.runOnIdle {
            val image = mockk<OrientedImage>()
            every { image.dataUri } returns null
            every { image.type } returns OrientedImageType.Horizontal
            every { image.geometry } returns Point(1.0, 2.0)
            coEvery { image.load() } returns Result.success(Unit)
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(image))
            scope.launch { state.searchImages(Point(1.0, 2.0)).getOrThrow() }
        }
        compose.waitForIdle()
    }

    @Test
    fun unsupportedLayersHideExplore() {
        showViewer(supportsSequentialNavigation = false)
        compose.onNodeWithText("Explore").assertDoesNotExist()
    }

    @Test
    fun exploreIsDisabledUntilAnImageIsActive() {
        showViewer(withImage = false)
        compose.onNodeWithText("Explore").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Sequential").assertDoesNotExist()
        compose.runOnIdle { assertFalse(state.uiState.isSequentialNavigationEnabled) }
        loadImage()
        compose.onNodeWithText("Explore").assertIsEnabled().performClick()
        compose.onNodeWithText("Sequential").assertIsEnabled().assertIsOff()
    }

    @Test
    fun sequentialDefaultsOffAndTogglesViewerStateWithoutClosingSheet() {
        showViewer()
        compose.onNodeWithText("Explore").assertIsNotSelected().performClick()
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.onNodeWithText("Sequential").assertIsOff().performClick().assertIsOn()
        compose.runOnIdle {
            assertTrue(state.uiState.isSequentialNavigationEnabled)
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
        }
        compose.onNodeWithText("Sequential").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Explore").assertIsSelected().performClick()
        compose.onNodeWithText("Sequential").assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertFalse(state.uiState.isSequentialNavigationEnabled) }
    }

    @Test
    fun externalChangesUpdateOpenSheetAndResetDisablesTheOption() {
        showViewer()
        compose.onNodeWithText("Explore").performClick()
        compose.runOnIdle { state.toggleSequentialNavigation() }
        compose.onNodeWithText("Sequential").assertIsOn()
        compose.runOnIdle { state.setSequentialNavigationEnabled(false) }
        compose.onNodeWithText("Sequential").assertIsOff()
        compose.runOnIdle { state.resetAll() }
        compose.onNodeWithText("Sequential").assertIsNotEnabled().performClick().assertIsOff()
        loadImage()
        compose.onNodeWithText("Sequential").assertIsEnabled().assertIsOff()
    }

    @Test
    fun backDismissesWithoutChangingSequentialNavigation() {
        showViewer()
        compose.onNodeWithText("Explore").performClick()
        compose.onNodeWithText("Sequential").assertIsOff()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Sequential").assertDoesNotExist()
        compose.onNodeWithText("Explore").assertIsNotSelected().performClick()
        compose.onNodeWithText("Sequential").assertIsOff()
    }
}
