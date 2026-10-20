/*
 * Copyright 2026 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.view.GraphicsOverlay
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PanoramaFootprintPublicationTests {
    @get:Rule
    val compose = createComposeRule()

    private val visible = mutableListOf<OrientedImageFootprint>()
    private val completions = mutableListOf<CompletableDeferred<Result<Unit>>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var state: OrientedImageryViewerState
    private lateinit var footprint: OrientedImageFootprint
    private lateinit var image: OrientedImage
    private var updatesReturned = 0

    @Before
    fun setUp() {
        image = mockk()
        every { image.dataUri } returns "/local/panorama.png"
        every { image.type } returns OrientedImageType.Image360
        every { image.geometry } returns Point(0.0, 0.0, 10.0)
        coEvery { image.load() } returns Result.success(Unit)
        val layer = mockk<OrientedImageryLayer>()
        every { layer.name } returns "Panorama"
        every { layer.supportsSequentialNavigation } returns false
        every { layer.visibleFootprints } returns visible
        footprint = mockFootprint(image)
        compose.setContent { }
        compose.runOnIdle {
            state = OrientedImageryViewerState(layer, GraphicsOverlay(), scope)
            // SDK images are service-created. Seed the existing-image path without network I/O.
            OrientedImageryViewerState::class.java.getDeclaredMethod(
                "setActiveOrientedImage", OrientedImage::class.java
            ).apply { isAccessible = true }.invoke(state, image)
            OrientedImageryViewerState::class.java.getDeclaredField("activeFootprint")
                .apply { isAccessible = true }.set(state, footprint)
        }
    }

    @After
    fun tearDown() {
        compose.runOnIdle {
            scope.cancel()
            completions.forEach { it.complete(Result.success(Unit)) }
        }
    }

    @Test
    fun firstShowAndReshowWaitForCurrentViewUpdate() {
        repeat(2) { index ->
            compose.runOnIdle {
                state.toggleActiveFootprint()
                assertTrue(visible.isEmpty())
                requestUpdate()
                assertTrue(visible.isEmpty())
                completions[index].complete(Result.success(Unit))
            }
            compose.waitUntil { visible.size == 1 }
            compose.runOnIdle {
                assertEquals(listOf(footprint), visible)
                state.toggleActiveFootprint()
            }
        }
    }

    @Test
    fun nextAndPreviousWaitForUpdateEvenWhenReusingVisibleFootprints() {
        val nextImage = mockk<OrientedImage>()
        every { nextImage.dataUri } returns "/local/next.png"
        every { nextImage.type } returns OrientedImageType.Image360
        every { nextImage.geometry } returns Point(1.0, 1.0, 10.0)
        coEvery { nextImage.load() } returns Result.success(Unit)
        val nextFootprint = mockFootprint(nextImage)
        compose.runOnIdle {
            OrientedImageryViewerState::class.java.getDeclaredField("orientedImages")
                .apply { isAccessible = true }.set(state, listOf(image, nextImage))
            OrientedImageryViewerState::class.java.getDeclaredField("activeIndex")
                .apply { isAccessible = true }.setInt(state, 0)
            state.toggleActiveFootprint()
            state.showConstructedPanoramaFootprint(state.uiState.activeImageRevision)
        }
        for ((index, target) in listOf(nextFootprint, footprint).withIndex()) {
            compose.runOnIdle {
                visible.add(target)
                scope.launch {
                    val result = if (index == 0) state.navigateNextImage() else state.navigatePreviousImage()
                    assertEquals(ImageNavigationStatus.Moved, result.getOrThrow())
                }
                assertTrue(state.isActiveFootprintVisible())
                assertTrue(visible.isEmpty())
                requestUpdate()
                assertTrue(visible.isEmpty())
                completions[index].complete(Result.success(Unit))
            }
            compose.waitUntil { visible.size == 1 }
            compose.runOnIdle { assertEquals(listOf(target), visible) }
        }
    }

    @Test
    fun rasterFootprintRemainsImmediatelyVisible() {
        compose.runOnIdle {
            every { image.type } returns OrientedImageType.Horizontal
            OrientedImageryViewerState::class.java.getDeclaredMethod(
                "setActiveOrientedImage", OrientedImage::class.java
            ).apply { isAccessible = true }.invoke(state, image)
            state.toggleActiveFootprint()
            assertEquals(listOf(footprint), visible)
            assertTrue(completions.isEmpty())
        }
    }

    @Test
    fun missingImageUriShowsConstructedFootprintImmediately() {
        compose.runOnIdle {
            every { image.dataUri } returns null
            OrientedImageryViewerState::class.java.getDeclaredMethod(
                "setActiveOrientedImage", OrientedImage::class.java
            ).apply { isAccessible = true }.invoke(state, image)
            state.toggleActiveFootprint()
            assertEquals(listOf(footprint), visible)
        }
    }

    @Test
    fun missingOrientationPublishesConstructedFootprintOnlyForCurrentRevision() {
        compose.runOnIdle {
            state.toggleActiveFootprint()
            val revision = state.uiState.activeImageRevision
            state.showConstructedPanoramaFootprint(revision - 1)
            assertTrue(visible.isEmpty())
            repeat(2) { state.showConstructedPanoramaFootprint(revision) }
            assertEquals(listOf(footprint), visible)
            assertTrue(completions.isEmpty())
        }
    }

    @Test
    fun failedUpdatePublishesFallbackButCanceledUpdateDoesNot() {
        compose.runOnIdle {
            state.toggleActiveFootprint()
            requestUpdate()
            completions[0].complete(Result.failure(IllegalStateException("Unavailable")))
        }
        compose.waitUntil { visible.size == 1 }
        compose.runOnIdle {
            state.toggleActiveFootprint()
            state.toggleActiveFootprint()
            requestUpdate()
            completions[1].complete(Result.failure(CancellationException()))
        }
        compose.waitUntil { updatesReturned == 2 }
        compose.runOnIdle { assertTrue(visible.isEmpty()) }
    }

    @Test
    fun hideAndReshowIgnoreOldCompletion() {
        compose.runOnIdle {
            state.toggleActiveFootprint()
            requestUpdate()
            state.toggleActiveFootprint()
            state.toggleActiveFootprint()
            requestUpdate()
            completions[0].complete(Result.success(Unit))
        }
        compose.waitUntil { updatesReturned == 1 }
        compose.runOnIdle {
            assertTrue(visible.isEmpty())
            completions[1].complete(Result.success(Unit))
        }
        compose.waitUntil { visible.size == 1 }
    }

    @Test
    fun imageResetDiscardsPendingPublication() {
        compose.runOnIdle {
            state.toggleActiveFootprint()
            requestUpdate()
            val revision = state.uiState.activeImageRevision
            state.resetAll()
            state.showConstructedPanoramaFootprint(revision)
            completions[0].complete(Result.success(Unit))
        }
        compose.waitUntil { updatesReturned == 1 }
        compose.runOnIdle { assertTrue(visible.isEmpty()) }
    }

    @Test
    fun sequentialModeShowsFallbackAndLeavingItWaitsForResync() {
        compose.runOnIdle {
            state.toggleActiveFootprint()
            requestUpdate()
            state.setSequentialNavigationEnabled(true)
            assertEquals(listOf(footprint), visible)
            state.setSequentialNavigationEnabled(false)
            assertTrue(visible.isEmpty())
            requestUpdate()
            completions[0].complete(Result.success(Unit))
        }
        compose.waitUntil { updatesReturned == 1 }
        compose.runOnIdle {
            assertTrue(visible.isEmpty())
            completions[1].complete(Result.success(Unit))
        }
        compose.waitUntil { visible.size == 1 }
    }

    @Test
    fun coalescedInitialUpdatesPublishOnlyAfterLatestRequest() {
        compose.runOnIdle {
            state.toggleActiveFootprint()
            requestUpdate()
            requestUpdate()
            completions[0].complete(Result.success(Unit))
        }
        compose.waitUntil { completions.size == 2 }
        compose.runOnIdle {
            assertTrue(visible.isEmpty())
            completions[1].complete(Result.success(Unit))
        }
        compose.waitUntil { visible.size == 1 }
    }

    private fun requestUpdate() = state.updateFootprints(10.0, 70.0, 60.0, 40.0)

    private fun mockFootprint(image: OrientedImage): OrientedImageFootprint {
        val footprint = mockk<OrientedImageFootprint>()
        every { footprint.orientedImage } returns image
        coEvery { footprint.updateFootprint(any<Double>(), any<Double>(), any<Double>(), any<Double>()) } coAnswers {
            val completion = CompletableDeferred<Result<Unit>>().also { completions.add(it) }
            // Model a native operation that can finish after its caller was canceled.
            withContext(NonCancellable) { completion.await().also { updatesReturned++ } }
        }
        return footprint
    }
}
