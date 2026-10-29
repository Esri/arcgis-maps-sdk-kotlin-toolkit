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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OrientedImageryConcurrencyTests {
    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val location = Point(1.0, 2.0)
    private val otherLocation = Point(3.0, 4.0)
    private val footprints = mutableListOf<OrientedImageFootprint>()
    private lateinit var layer: OrientedImageryLayer
    private lateinit var state: OrientedImageryViewerState
    private lateinit var overlay: GraphicsOverlay
    private lateinit var feature: Feature

    @Before
    fun setUp() {
        compose.setContent { }
        compose.runOnIdle {
            layer = mockk()
            every { layer.name } returns "Concurrency"
            every { layer.supportsSequentialNavigation } returns true
            every { layer.visibleFootprints } returns footprints
            feature = mockk()
            every { feature.geometry } returns otherLocation
            overlay = GraphicsOverlay()
            state = OrientedImageryViewerState(layer, overlay, scope)
        }
    }

    @After
    fun tearDown() {
        compose.runOnIdle { scope.cancel() }
    }

    private fun image(path: String): OrientedImage = mockk<OrientedImage>().also { image ->
        every { image.dataUri } returns path
        every { image.type } returns OrientedImageType.Horizontal
        every { image.geometry } returns location
        every { image.attributes } returns mutableMapOf()
        coEvery { image.load() } returns Result.success(Unit)
    }

    private fun assertImage(path: String?) {
        assertEquals(path, state.uiState.activeImageInfo?.dataUri)
    }

    private fun search(vararg images: OrientedImage) {
        coEvery { layer.searchImages(any(), any()) } returns Result.success(images.toList())
        scope.launch { state.searchImages(location).getOrThrow() }
    }

    @Test
    fun newerSearchWinsEvenWhenOlderSearchCompletesLast() {
        compose.runOnIdle {
            val old = image("old")
            val latest = image("latest")
            val pending = CompletableDeferred<Result<List<OrientedImage>>>()
            coEvery { layer.searchImages(location, any()) } coAnswers { pending.await() }
            coEvery { layer.searchImages(otherLocation, any()) } returns Result.success(listOf(latest))
            val oldJob = scope.launch { state.searchImages(location) }
            scope.launch { state.searchImages(otherLocation).getOrThrow() }
            assertImage("latest")
            pending.complete(Result.success(listOf(old)))
            assertTrue(oldJob.isCancelled)
            assertImage("latest")
            coVerify(exactly = 0) { old.load() }
        }
    }

    @Test
    fun staleSearchFailureCannotClearNewerFeatureFetch() {
        compose.runOnIdle {
            val pending = CompletableDeferred<Result<List<OrientedImage>>>()
            coEvery { layer.searchImages(any(), any()) } coAnswers { pending.await() }
            val oldJob = scope.launch { state.searchImages(location) }
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image("feature"))
            scope.launch { state.fetchImage(feature).getOrThrow() }
            pending.complete(Result.failure(IllegalStateException("Old search failed")))
            assertTrue(oldJob.isCancelled)
            assertImage("feature")
            assertEquals(otherLocation.x, state.inputLocationData!!.x, 0.0)
        }
    }

    @Test
    fun featureFetchSupersedesSearchDuringImageLoad() {
        compose.runOnIdle {
            val old = image("old")
            val loading = CompletableDeferred<Result<Unit>>()
            coEvery { old.load() } coAnswers { loading.await() }
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(old))
            val oldJob = scope.launch { state.searchImages(location) }
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image("feature"))
            scope.launch { state.fetchImage(feature).getOrThrow() }
            loading.complete(Result.success(Unit))
            assertTrue(oldJob.isCancelled)
            assertImage("feature")
        }
    }

    @Test
    fun searchSupersedesPendingFeatureFetch() {
        compose.runOnIdle {
            val pending = CompletableDeferred<Result<OrientedImage>>()
            coEvery { layer.fetchImageForFeature(feature) } coAnswers { pending.await() }
            val oldJob = scope.launch { state.fetchImage(feature) }
            search(image("search"))
            pending.complete(Result.success(image("feature")))
            assertTrue(oldJob.isCancelled)
            assertImage("search")
        }
    }

    @Test
    fun resetInvalidatesFeatureFetchDuringLoad() {
        compose.runOnIdle {
            val candidate = image("feature")
            val loading = CompletableDeferred<Result<Unit>>()
            coEvery { candidate.load() } coAnswers { loading.await() }
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(candidate)
            val job = scope.launch { state.fetchImage(feature) }
            state.resetAll()
            loading.complete(Result.success(Unit))
            assertTrue(job.isCancelled)
            assertImage(null)
            assertNull(state.inputLocationData)
        }
    }

    @Test
    fun cancellationReturnedBySdkIsRethrown() {
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } returns Result.failure(CancellationException("Cancelled"))
            var returned = false
            val job = scope.launch {
                state.searchImages(location)
                returned = true
            }
            assertTrue(job.isCancelled)
            assertFalse(returned)
        }
    }

    @Test
    fun nextRequestsQueueAndResolveTheIndexAfterAcquiringTheLock() {
        compose.runOnIdle {
            val first = image("first")
            val second = image("second")
            val third = image("third")
            search(first, second, third)
            val loading = CompletableDeferred<Result<Unit>>()
            coEvery { second.load() } coAnswers { loading.await() }
            val next = scope.launch { state.navigateNextImage().getOrThrow() }
            val queued = scope.launch { state.navigateNextImage().getOrThrow() }
            assertFalse(next.isCompleted)
            assertFalse(queued.isCompleted)
            coVerify(exactly = 0) { third.load() }
            loading.complete(Result.success(Unit))
            assertTrue(next.isCompleted)
            assertTrue(queued.isCompleted)
            assertImage("third")
            scope.launch {
                assertEquals(ImageNavigationStatus.BoundaryReached, state.navigateNextImage().getOrThrow())
            }
        }
    }

    @Test
    fun previousQueuesBehindNextAndUsesItsCommittedIndex() {
        compose.runOnIdle {
            val first = image("first")
            val second = image("second")
            search(first, second)
            val loading = CompletableDeferred<Result<Unit>>()
            coEvery { second.load() } coAnswers { loading.await() }
            scope.launch { state.navigateNextImage().getOrThrow() }
            val previous = scope.launch { state.navigatePreviousImage().getOrThrow() }
            assertFalse(previous.isCompleted)
            loading.complete(Result.success(Unit))
            assertTrue(previous.isCompleted)
            assertImage("first")
        }
    }

    @Test
    fun newSearchInvalidatesRunningAndQueuedNavigation() {
        compose.runOnIdle {
            val second = image("second")
            search(image("first"), second)
            val loading = CompletableDeferred<Result<Unit>>()
            coEvery { second.load() } coAnswers { loading.await() }
            val running = scope.launch { state.navigateNextImage() }
            val queued = scope.launch { state.navigatePreviousImage() }
            search(image("replacement"))
            loading.complete(Result.success(Unit))
            assertTrue(running.isCancelled)
            assertTrue(queued.isCancelled)
            assertImage("replacement")
        }
    }

    @Test
    fun resetInvalidatesRunningAndQueuedNavigation() {
        compose.runOnIdle {
            val second = image("second")
            search(image("first"), second)
            val loading = CompletableDeferred<Result<Unit>>()
            coEvery { second.load() } coAnswers { loading.await() }
            val running = scope.launch { state.navigateNextImage() }
            val queued = scope.launch { state.navigateNextImage() }
            state.resetAll()
            loading.complete(Result.success(Unit))
            assertTrue(running.isCancelled)
            assertTrue(queued.isCancelled)
            assertImage(null)
        }
    }

    @Test
    fun sequentialNavigationUsesThePreviouslyCommittedImage() {
        compose.runOnIdle {
            val first = image("first")
            val second = image("second")
            val third = image("third")
            search(first)
            state.setSequentialNavigationEnabled(true)
            val pending = CompletableDeferred<Result<OrientedImage>>()
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } coAnswers { pending.await() }
            coEvery { layer.fetchAdjacentImage(second, SequenceStep.Next) } returns Result.success(third)
            scope.launch { state.navigateNextImage().getOrThrow() }
            val queued = scope.launch { state.navigateNextImage().getOrThrow() }
            coVerify(exactly = 0) { layer.fetchAdjacentImage(second, any()) }
            pending.complete(Result.success(second))
            assertTrue(queued.isCompleted)
            assertImage("third")
        }
    }

    @Test
    fun navigationFailurePreservesImageAndReleasesQueue() {
        compose.runOnIdle {
            val first = image("first")
            val second = image("second")
            search(first, second)
            val failure = IllegalStateException("Cannot load")
            coEvery { second.load() } returns Result.failure(failure)
            scope.launch {
                assertEquals(failure, state.navigateNextImage().exceptionOrNull())
                assertImage("first")
                coEvery { second.load() } returns Result.success(Unit)
                state.navigateNextImage().getOrThrow()
                assertImage("second")
            }
        }
    }

    @Test
    fun searchingPreservesInputAndCameraVerticalReferences() {
        compose.runOnIdle {
            val input = Point(1.0, 2.0, 3.0, SpatialReference(3857, 5773))
            val camera = Point(4.0, 5.0, 6.0, SpatialReference(3857, 115700))
            val image = image("vertical")
            every { image.geometry } returns camera
            coEvery { layer.searchImages(input, any()) } returns Result.success(listOf(image))
            scope.launch { state.searchImages(input).getOrThrow() }
            assertEquals(input.spatialReference, state.inputLocationData?.toPoint()?.spatialReference)
            assertEquals(camera.spatialReference, state.uiState.activeImageInfo?.cameraLocationData?.toPoint()?.spatialReference)
        }
    }

    @Test
    fun resetAndSearchPreserveForeignGraphicsAndFootprints() {
        compose.runOnIdle {
            val foreignGraphic = Graphic(location)
            val foreignFootprint = mockk<OrientedImageFootprint>()
            overlay.graphics.add(foreignGraphic)
            footprints.add(foreignFootprint)
            state.addImageLocationMarker(otherLocation)
            state.resetAll()
            assertEquals(listOf(foreignGraphic), overlay.graphics.toList())
            assertEquals(listOf(foreignFootprint), footprints)
            search(image("replacement"))
            assertEquals(listOf(foreignGraphic), overlay.graphics.toList())
            assertEquals(listOf(foreignFootprint), footprints)
        }
    }
}
