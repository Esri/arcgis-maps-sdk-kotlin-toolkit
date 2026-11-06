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

package com.arcgismaps.toolkit.orientedimageryviewer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrientedImageryToolbarTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var scrollState: ScrollState

    private fun showToolbar(width: Dp = 411.dp, fontScale: Float = 1f) {
        compose.setContent {
            // Keep the simulated viewport independent of the test device's display density.
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme {
                    scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .width(width)
                            .padding(horizontal = 8.dp)
                            .testTag("toolbarViewport")
                            .horizontalScroll(scrollState)
                    ) {
                        OrientedImageryToolbar(
                            interactionStatus = GeoViewInteractionStatus.FindImages,
                            isFindImagesSelected = true,
                            isDisplayImageSelected = false,
                            onFindImagesSelectedChange = {},
                            onDisplayImageSelectedChange = {},
                            isMarkingLocation = false,
                            isMarkerSelectionVisible = false,
                            selectedMarkerOptionId = null,
                            onSelectMarkerOption = {},
                            onDismissMarkerSelection = {},
                            hasActiveImage = true,
                            hasAdditionalImages = true,
                            isActiveFootprintVisible = false,
                            isAdditionalFootprintsVisible = false,
                            isAdditionalCameraLocationsVisible = false,
                            onPreviousImage = null,
                            onNextImage = null,
                            onCurrentFootprint = {},
                            onAdditionalFootprints = {},
                            onAdditionalCameraLocations = {},
                            supportsSequentialNavigation = true,
                            isSequentialNavigationEnabled = false,
                            onSequentialNavigation = {},
                            onReset = {}
                        )
                    }
                }
            }
        }
    }

    private fun assertChipFullyVisible(label: String) {
        val viewport = compose.onNodeWithTag("toolbarViewport").getUnclippedBoundsInRoot()
        val chip = compose.onNodeWithText(label).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("$label starts inside the viewport", chip.left >= viewport.left)
        assertTrue("$label ends inside the viewport", chip.right <= viewport.right)
    }

    @Test
    fun allChipsFitPixel7WidthWithoutScrolling() {
        showToolbar()
        listOf("Mode", "Overlays", "Markers", "Explore").forEach(::assertChipFullyVisible)
        compose.runOnIdle { assertEquals(0, scrollState.maxValue) }
    }

    @Test
    fun enlargedTextCanScrollToExplore() {
        showToolbar(fontScale = 2f)
        compose.runOnIdle { assertTrue(scrollState.maxValue > 0) }
        compose.onNodeWithText("Explore").performScrollTo()
        assertChipFullyVisible("Explore")
    }

    @Test
    fun narrowViewportCanScrollToExplore() {
        showToolbar(width = 280.dp)
        compose.runOnIdle { assertTrue(scrollState.maxValue > 0) }
        compose.onNodeWithText("Explore").performScrollTo()
        assertChipFullyVisible("Explore")
    }
}
