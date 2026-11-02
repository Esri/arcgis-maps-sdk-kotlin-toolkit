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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.arcgismaps.exceptions.ArcGISException
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ImagePagerTests {
    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var state: OrientedImageryViewerState
    private lateinit var layer: OrientedImageryLayer
    private lateinit var images: List<OrientedImage>

    @After
    fun tearDown() {
        compose.runOnIdle { scope.cancel() }
    }

    private fun image(): OrientedImage = mockk<OrientedImage>().also {
        // Missing-URI placeholders keep these pager tests independent of native renderer frame loops.
        every { it.dataUri } returns null
        every { it.type } returns OrientedImageType.Horizontal
        every { it.geometry } returns Point(1.0, 2.0)
        every { it.attributes } returns mutableMapOf()
        coEvery { it.load() } returns Result.success(Unit)
    }

    private fun showViewer() {
        layer = mockk()
        every { layer.name } returns "Pager tests"
        every { layer.supportsSequentialNavigation } returns true
        every { layer.visibleFootprints } returns mutableListOf()
        val boundary = mockk<ArcGISException>()
        every { boundary.errorCode } returns 20
        coEvery { layer.fetchAdjacentImage(any(), any()) } returns Result.failure(boundary)
        images = List(3) { image() }
        coEvery { layer.searchImages(any(), any()) } returns Result.success(images)
        compose.setContent {
            state = remember {
                OrientedImageryViewerState(layer, GraphicsOverlay(), scope) { mockk(relaxed = true) }
            }
            MaterialTheme { OrientedImagery(state, Modifier.fillMaxSize()) }
        }
        compose.runOnIdle { scope.launch { state.searchImages(Point(1.0, 2.0)).getOrThrow() } }
    }

    @Test
    fun buttonsNavigateWithOneBasedBadgesAndDisabledBoundaries() {
        showViewer()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithContentDescription("First image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next image").performClick()
        compose.onNodeWithText("2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Last image").performClick()
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Last image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous image").performClick()
        compose.onNodeWithText("2").assertIsDisplayed()
        compose.onNodeWithContentDescription("First image").performClick()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, state.uiState.selectedResultIndex) }
    }

    @Test
    fun horizontalGesturesDoNotChangePage() {
        showViewer()
        compose.onNodeWithTag("OrientedImagePager").performTouchInput { swipeLeft() }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithTag("OrientedImagePager").performTouchInput { swipeRight() }
        compose.runOnIdle { assertEquals(0, state.uiState.selectedResultIndex) }
        compose.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun sequentialModeHidesBadgesAndBoundariesAndUsesAdjacentImages() {
        showViewer()
        compose.onNodeWithContentDescription("Next image").performClick()
        compose.onNodeWithText("2").assertIsDisplayed()
        val adjacent = image()
        coEvery { layer.fetchAdjacentImage(images[1], SequenceStep.Next) } returns Result.success(adjacent)
        coEvery { layer.fetchAdjacentImage(adjacent, SequenceStep.Previous) } returns Result.success(images[1])
        compose.runOnIdle { state.setSequentialNavigationEnabled(true) }
        compose.onNodeWithContentDescription("First image").assertDoesNotExist()
        compose.onNodeWithContentDescription("Last image").assertDoesNotExist()
        (1..3).forEach { compose.onNodeWithText(it.toString()).assertDoesNotExist() }
        compose.onNodeWithContentDescription("Next image").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Previous image").assertIsEnabled().performClick()
        compose.runOnIdle {
            coVerify(exactly = 2) { layer.fetchAdjacentImage(images[1], SequenceStep.Next) }
            coVerify(exactly = 1) { layer.fetchAdjacentImage(adjacent, SequenceStep.Previous) }
            state.setSequentialNavigationEnabled(false)
        }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithContentDescription("First image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Last image").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(listOf(images[1]), state.searchResultImages) }
    }

    @Test
    fun sequentialButtonsStayDisabledUntilTheirOwnPrefetchSucceeds() {
        showViewer()
        val previous = CompletableDeferred<Result<OrientedImage>>()
        val next = CompletableDeferred<Result<OrientedImage>>()
        val previousImage = image()
        val nextImage = image()
        coEvery { layer.fetchAdjacentImage(images[0], SequenceStep.Previous) } coAnswers { previous.await() }
        coEvery { layer.fetchAdjacentImage(images[0], SequenceStep.Next) } coAnswers { next.await() }
        compose.runOnIdle { state.setSequentialNavigationEnabled(true) }
        compose.onNodeWithContentDescription("Previous image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next image").assertIsNotEnabled()
        compose.runOnIdle { previous.complete(Result.success(previousImage)) }
        compose.onNodeWithContentDescription("Previous image").assertIsEnabled()
        compose.onNodeWithContentDescription("Next image").assertIsNotEnabled()
        compose.runOnIdle { next.complete(Result.success(nextImage)) }
        compose.onNodeWithContentDescription("Next image").assertIsEnabled()
        compose.runOnIdle {
            coVerify(exactly = 0) { previousImage.load() }
            coVerify(exactly = 0) { nextImage.load() }
        }
    }

    @Test
    fun unexpectedPrefetchErrorIsDisplayedWhileOtherDirectionRemainsUsable() {
        showViewer()
        val adjacent = image()
        coEvery { layer.fetchAdjacentImage(images[0], SequenceStep.Previous) } returns
            Result.failure(IllegalStateException("Network unavailable"))
        coEvery { layer.fetchAdjacentImage(images[0], SequenceStep.Next) } returns Result.success(adjacent)
        compose.runOnIdle { state.setSequentialNavigationEnabled(true) }
        compose.onNodeWithContentDescription("Previous image").assertIsNotEnabled()
        compose.onNodeWithText("Unable to navigate to the image.").assertIsDisplayed()
        compose.onNodeWithText("Network unavailable").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next image").assertIsEnabled().performClick()
        // Both directions from this image are normal boundaries, not errors.
        compose.onNodeWithContentDescription("Previous image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next image").assertIsNotEnabled()
        compose.onNodeWithText("Unable to navigate to the image.").assertDoesNotExist()
        compose.onNodeWithText("Network unavailable").assertDoesNotExist()
        compose.runOnIdle { state.setSequentialNavigationEnabled(false) }
        compose.runOnIdle { assertEquals(listOf(adjacent), state.searchResultImages) }
    }

    @Test
    fun failedNavigationRetainsCurrentBadgeAndReplacementSearchResetsPager() {
        showViewer()
        coEvery { images[1].load() } returns Result.failure(IllegalStateException("Load failed"))
        compose.onNodeWithContentDescription("Next image").performClick()
        compose.onNodeWithText("Unable to navigate to the image.").assertIsDisplayed()
        compose.onNodeWithText("1").assertIsDisplayed()
        coEvery { images[1].load() } returns Result.success(Unit)
        compose.onNodeWithContentDescription("Last image").performClick()
        compose.onNodeWithText("3").assertIsDisplayed()
        // The same SDK objects/URIs still constitute a new search session.
        compose.runOnIdle { scope.launch { state.searchImages(Point(3.0, 4.0)).getOrThrow() } }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithContentDescription("First image").assertIsNotEnabled()
    }
}

