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
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.exceptions.ArcGISException
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ViewerAppearance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
            coEvery { layer.fetchAdjacentImage(any(), any()) } returns Result.failure(noAdjacentImage())
            feature = mockk()
            every { feature.geometry } returns otherLocation
            overlay = GraphicsOverlay()
            state = OrientedImageryViewerState(MapViewProxy(), layer, overlay, scope) { mockk(relaxed = true) }
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

    private fun noAdjacentImage(): ArcGISException = mockk<ArcGISException>().also {
        every { it.errorCode } returns 20
        every { it.message } returns "No adjacent image query result is available"
    }

    private fun search(vararg images: OrientedImage) {
        coEvery { layer.searchImages(any(), any()) } returns Result.success(images.toList())
        scope.launch { state.searchImages(location).getOrThrow() }
    }

    private fun assertOverlays(active: Boolean, additional: Boolean, cameras: Boolean) {
        assertEquals(active, state.uiState.isActiveFootprintVisible)
        assertEquals(additional, state.uiState.isAdditionalFootprintsVisible)
        assertEquals(cameras, state.uiState.isAdditionalCameraLocationsVisible)
    }

    @Test
    fun activePanoramaIsVisibleOnlyAfterFirstSuccessfulViewportUpdate() {
        val completion = CompletableDeferred<Unit>()
        val footprint = mockk<OrientedImageFootprint>(relaxed = true)
        compose.runOnIdle {
            val panorama = image("panorama")
            every { panorama.type } returns OrientedImageType.Image360
            coEvery { footprint.updateFootprint(any(), any(), any(), any()) } coAnswers {
                completion.await()
                Result.success(Unit)
            }
            state = OrientedImageryViewerState(MapViewProxy(), layer, overlay, scope) { footprint }
            search(panorama)
            assertTrue(state.uiState.isActiveFootprintVisible)
            assertTrue(footprints.isEmpty())

            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
            assertTrue(footprints.isEmpty())
            completion.complete(Unit)
        }
        compose.runOnIdle {
            assertEquals(listOf(footprint), footprints)
            state.toggleActiveFootprint()
            assertTrue(footprints.isEmpty())
            state.toggleActiveFootprint()
            assertEquals(listOf(footprint), footprints)
        }
    }

    @Test
    fun failedPanoramaUpdateDoesNotExposeDefaultFootprint() {
        compose.runOnIdle {
            val panorama = image("panorama")
            every { panorama.type } returns OrientedImageType.Image360
            val footprint = mockk<OrientedImageFootprint>(relaxed = true)
            coEvery { footprint.updateFootprint(any(), any(), any(), any()) } returns
                Result.failure(IllegalStateException("Update failed"))
            state = OrientedImageryViewerState(MapViewProxy(), layer, overlay, scope) { footprint }
            search(panorama)
            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
            assertTrue(footprints.isEmpty())
            state.toggleActiveFootprint()
            state.toggleActiveFootprint()
            assertTrue(footprints.isEmpty())

            coEvery { footprint.updateFootprint(any(), any(), any(), any()) } returns Result.success(Unit)
            state.updateFootprints(46.0, 90.0, 60.0, 45.0)
            assertEquals(listOf(footprint), footprints)
        }
    }

    @Test
    fun panoramaUpdateCompletingAfterHideDoesNotPublishFootprint() {
        val completion = CompletableDeferred<Unit>()
        val footprint = mockk<OrientedImageFootprint>(relaxed = true)
        compose.runOnIdle {
            val panorama = image("panorama")
            every { panorama.type } returns OrientedImageType.Image360
            coEvery { footprint.updateFootprint(any(), any(), any(), any()) } coAnswers {
                withContext(NonCancellable) { completion.await() }
                Result.success(Unit)
            }
            state = OrientedImageryViewerState(MapViewProxy(), layer, overlay, scope) { footprint }
            search(panorama)
            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
            state.toggleActiveFootprint()
            completion.complete(Unit)
        }
        compose.runOnIdle {
            assertTrue(footprints.isEmpty())
            state.toggleActiveFootprint()
            assertTrue(footprints.isEmpty())
            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
        }
        compose.runOnIdle { assertEquals(listOf(footprint), footprints) }
    }

    @Test
    fun stalePanoramaUpdateDoesNotInitializeReplacementFootprint() {
        val completion = CompletableDeferred<Unit>()
        val oldFootprint = mockk<OrientedImageFootprint>(relaxed = true)
        val newFootprint = mockk<OrientedImageFootprint>(relaxed = true)
        compose.runOnIdle {
            val panorama = image("panorama")
            val replacement = image("replacement panorama")
            every { panorama.type } returns OrientedImageType.Image360
            every { replacement.type } returns OrientedImageType.Image360
            coEvery { oldFootprint.updateFootprint(any(), any(), any(), any()) } coAnswers {
                withContext(NonCancellable) { completion.await() }
                Result.success(Unit)
            }
            coEvery { newFootprint.updateFootprint(any(), any(), any(), any()) } returns Result.success(Unit)
            state = OrientedImageryViewerState(MapViewProxy(), layer, overlay, scope) {
                if (it === panorama) oldFootprint else newFootprint
            }
            search(panorama)
            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
            search(replacement)
            assertTrue(footprints.isEmpty())
            completion.complete(Unit)
        }
        compose.runOnIdle {
            assertTrue(footprints.isEmpty())
            state.toggleActiveFootprint()
            state.toggleActiveFootprint()
            assertTrue(footprints.isEmpty())
            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
            assertEquals(listOf(newFootprint), footprints)
        }
    }

    @Test
    fun additionalPanoramaVisibilityOnlyChangesCollectionAndUpdatesOnlyTargetActiveImage() {
        compose.runOnIdle {
            val active = image("active panorama")
            val panorama = image("additional panorama")
            every { active.type } returns OrientedImageType.Image360
            every { panorama.type } returns OrientedImageType.Image360
            val raster = image("additional raster")
            val replacement = image("replacement")
            val created = listOf(active, panorama, raster, replacement).associateWith { mockk<OrientedImageFootprint>(relaxed = true) }
            val panoramaFootprint = created.getValue(panorama)
            val activeFootprint = created.getValue(active)
            coEvery { activeFootprint.updateFootprint(45.0, 90.0, 60.0, 45.0) } returns Result.success(Unit)
            state = OrientedImageryViewerState(MapViewProxy(), layer, overlay, scope) { created.getValue(it) }

            search(active, panorama, raster)
            state.toggleActiveFootprint()
            state.toggleAdditionalFootprints()
            assertEquals(setOf(panoramaFootprint, created.getValue(raster)), footprints.toSet())

            state.toggleAdditionalFootprints()
            assertTrue(footprints.isEmpty())
            state.toggleAdditionalFootprints()
            assertEquals(setOf(panoramaFootprint, created.getValue(raster)), footprints.toSet())
            coVerify(exactly = 0) { activeFootprint.updateFootprint(any(), any(), any(), any()) }

            state.toggleActiveFootprint()
            state.updateFootprints(45.0, 90.0, 60.0, 45.0)
            coVerify(exactly = 1) { activeFootprint.updateFootprint(45.0, 90.0, 60.0, 45.0) }
            listOf(panoramaFootprint, created.getValue(raster)).forEach { footprint ->
                coVerify(exactly = 0) { footprint.updateFootprint(any(), any(), any(), any()) }
                coVerify(exactly = 0) { footprint.updateFootprint(any()) }
            }
            coVerify(exactly = 0) { panorama.load() }
            search(replacement)
            assertFalse(panoramaFootprint in footprints)
            assertImage("replacement")
        }
    }

    @Test
    fun appearanceDefaultsMatchSpecifiedColorsAndSymbols() {
        compose.runOnIdle {
            val defaults = ViewerAppearance()
            assertEquals(Color.fromRgba(255, 0, 0, 128), defaults.selectedFootprintFillColor)
            assertEquals(Color.red, defaults.selectedFootprintOutlineColor)
            assertEquals(Color.fromRgba(0, 0, 255, 64), defaults.unselectedFootprintFillColor)
            assertEquals(Color.blue, defaults.unselectedFootprintOutlineColor)
            listOf(
                Triple(defaults.searchPointMarkerSymbol, SimpleMarkerSymbolStyle.X, Color.red),
                Triple(defaults.selectedCameraMarkerSymbol, SimpleMarkerSymbolStyle.Circle, Color.red),
                Triple(defaults.allCamerasMarkerSymbol, SimpleMarkerSymbolStyle.Circle, Color.fromRgba(0, 0, 255, 200))
            ).forEach { (symbol, style, color) ->
                val marker = symbol as SimpleMarkerSymbol
                assertEquals(style, marker.style)
                assertEquals(color, marker.color)
                assertEquals(15f, marker.size)
                assertEquals(Color.white, marker.outline?.color)
                assertEquals(1f, marker.outline?.width)
                assertEquals(SimpleLineSymbolStyle.Solid, marker.outline?.style)
            }
            val noImage = defaults.noImageMarkerSymbol as SimpleMarkerSymbol
            assertEquals(SimpleMarkerSymbolStyle.Circle, noImage.style)
            assertEquals(Color.fromRgba(211, 211, 211, 255), noImage.color)
            assertEquals(12f, noImage.size)
            assertEquals(Color.black, noImage.outline?.color)
            assertEquals(1f, noImage.outline?.width)
            assertEquals(SimpleLineSymbolStyle.Solid, noImage.outline?.style)
        }
    }

    @Test
    fun appearanceChangesUpdateExistingOverlaysWithoutResettingSessionOrMarkers() {
        compose.runOnIdle {
            search(image("active"), image("additional"))
            state.toggleAdditionalFootprints()
            state.addImageLocationMarker(otherLocation)
            val graphics = overlay.graphics.toList()
            val userMarker = graphics.last()
            val userSymbol = userMarker.symbol
            val currentMarker = state.currentMarkerSymbol
            val revision = state.uiState.activeImageRevision
            val newAppearance = ViewerAppearance(
                selectedFootprintFillColor = Color.black,
                selectedFootprintOutlineColor = Color.white,
                unselectedFootprintFillColor = Color.white,
                unselectedFootprintOutlineColor = Color.black
            )
            state.updateAppearance(newAppearance)
            assertImage("active")
            assertEquals(revision, state.uiState.activeImageRevision)
            assertEquals(graphics, overlay.graphics.toList())
            assertEquals(userSymbol, userMarker.symbol)
            assertSame(currentMarker, state.currentMarkerSymbol)
            assertTrue(overlay.graphics.any { it.symbol == newAppearance.selectedCameraMarkerSymbol })
            assertTrue(overlay.graphics.any { it.symbol == newAppearance.allCamerasMarkerSymbol })
            assertTrue(overlay.graphics.any { it.symbol == newAppearance.searchPointMarkerSymbol })
            verify { footprints.first().fillColor = Color.black }
            verify { footprints.first().outlineColor = Color.white }
            verify { footprints.last().fillColor = Color.white }
            verify { footprints.last().outlineColor = Color.black }
            state.resetAll()
            assertEquals(newAppearance, state.appearance)
            search(image("replacement"))
            verify { footprints.single().fillColor = Color.black }
            verify { footprints.single().outlineColor = Color.white }
        }
    }

    @Test
    fun directRequestsRemainLoadingThroughRetrievalAndImageLoad() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.None
            for (fetch in listOf(false, true)) {
                for (failLoad in listOf(false, true)) {
                    val retrieving = CompletableDeferred<Unit>()
                    val loading = CompletableDeferred<Result<Unit>>()
                    val candidate = image("candidate")
                    coEvery { candidate.load() } coAnswers { loading.await() }
                    coEvery { layer.searchImages(any(), any()) } coAnswers {
                        retrieving.await()
                        Result.success(listOf(candidate))
                    }
                    coEvery { layer.fetchImageForFeature(feature) } coAnswers {
                        retrieving.await()
                        Result.success(candidate)
                    }
                    state.showMessage("Previous error")
                    val job = scope.launch {
                        val result = if (fetch) state.fetchImage(feature) else state.searchImages(location)
                        assertEquals(failLoad, result.isFailure)
                    }
                    assertTrue(state.uiState.isLoading)
                    assertNull(state.uiState.message)
                    assertImage(null)
                    retrieving.complete(Unit)
                    assertTrue(state.uiState.isLoading)
                    assertFalse(job.isCompleted)
                    assertImage(null)
                    loading.complete(if (failLoad) Result.failure(IllegalStateException("Load failed"))
                        else Result.success(Unit))
                    assertTrue(job.isCompleted)
                    assertFalse(state.uiState.isLoading)
                    if (failLoad) {
                        assertImage(null)
                        assertEquals(if (fetch) R.string.image_fetch_failed else R.string.image_search_failed,
                            state.uiState.messageResourceId)
                    } else {
                        assertImage("candidate")
                        assertNull(state.uiState.messageResourceId)
                    }
                }
            }
        }
    }

    @Test
    fun cancellingDirectRequestsEndsLoadingAtEitherStage() {
        compose.runOnIdle {
            for (fetch in listOf(false, true)) {
                for (duringLoad in listOf(false, true)) {
                    val pending = CompletableDeferred<Unit>()
                    val candidate = image("cancelled")
                    coEvery { candidate.load() } coAnswers {
                        if (duringLoad) pending.await()
                        Result.success(Unit)
                    }
                    coEvery { layer.searchImages(any(), any()) } coAnswers {
                        if (!duringLoad) pending.await()
                        Result.success(listOf(candidate))
                    }
                    coEvery { layer.fetchImageForFeature(feature) } coAnswers {
                        if (!duringLoad) pending.await()
                        Result.success(candidate)
                    }
                    val job = scope.launch {
                        if (fetch) state.fetchImage(feature) else state.searchImages(location)
                    }
                    assertTrue(state.uiState.isLoading)
                    job.cancel()
                    assertFalse(state.uiState.isLoading)
                    assertImage(null)
                    assertNull(state.uiState.messageResourceId)
                }
            }
        }
    }

    @Test
    fun obsoleteRequestCleanupCannotHidePendingReplacementProgress() {
        compose.runOnIdle {
            for (cancelOld in listOf(false, true)) {
                val searching = CompletableDeferred<Result<List<OrientedImage>>>()
                val fetching = CompletableDeferred<Result<OrientedImage>>()
                coEvery { layer.searchImages(any(), any()) } coAnswers { searching.await() }
                coEvery { layer.fetchImageForFeature(feature) } coAnswers { fetching.await() }
                val oldJob = scope.launch { state.searchImages(location) }
                scope.launch { state.fetchImage(feature).getOrThrow() }
                if (cancelOld) oldJob.cancel() else {
                    searching.complete(Result.failure(IllegalStateException("Obsolete search")))
                }
                assertTrue(oldJob.isCancelled)
                assertTrue(state.uiState.isLoading)
                assertNull(state.uiState.messageResourceId)
                fetching.complete(Result.success(image("replacement")))
                assertFalse(state.uiState.isLoading)
                assertImage("replacement")
            }
        }
    }

    @Test
    fun overlayAvailabilityFollowsImageSessionRatherThanCreatedFootprints() {
        compose.runOnIdle {
            assertFalse(state.hasActiveImage)
            assertFalse(state.uiState.hasAdditionalImages)
            search(image("first"), image("second"))
            assertTrue(state.hasActiveImage)
            assertTrue(state.uiState.hasAdditionalImages)
            assertFalse(state.uiState.isAdditionalFootprintsVisible)
            assertEquals(1, footprints.size)
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image("feature"))
            scope.launch { state.fetchImage(feature).getOrThrow() }
            assertTrue(state.hasActiveImage)
            assertFalse(state.uiState.hasAdditionalImages)
            search()
            assertFalse(state.hasActiveImage)
            assertFalse(state.uiState.hasAdditionalImages)
            search(image("replacement"), image("additional"))
            val pending = CompletableDeferred<Result<List<OrientedImage>>>()
            coEvery { layer.searchImages(any(), any()) } coAnswers { pending.await() }
            scope.launch { assertTrue(state.searchImages(location).isFailure) }
            assertFalse(state.hasActiveImage)
            assertFalse(state.uiState.hasAdditionalImages)
            pending.complete(Result.failure(IllegalStateException("Search failed")))
            assertFalse(state.hasActiveImage)
            assertFalse(state.uiState.hasAdditionalImages)
            search(image("recovered"), image("additional"))
            state.resetAll()
            assertFalse(state.hasActiveImage)
            assertFalse(state.uiState.hasAdditionalImages)
        }
    }

    @Test
    fun overlaysDefaultToCurrentFootprintAndCameraLocations() {
        compose.runOnIdle {
            assertOverlays(true, false, true)
            state.toggleActiveFootprint()
            state.toggleAdditionalFootprints()
            state.toggleAdditionalCameraLocations()
            assertOverlays(false, true, false)
            state.resetAll()
            assertOverlays(true, false, true)
        }
    }

    @Test
    fun allOverlayCombinationsSurviveEmptySearchAndFeatureFetch() {
        compose.runOnIdle {
            for (selection in 0..7) {
                state.resetAll()
                val active = selection and 1 != 0
                val additional = selection and 2 != 0
                val cameras = selection and 4 != 0
                if (!active) state.toggleActiveFootprint()
                if (additional) state.toggleAdditionalFootprints()
                if (!cameras) state.toggleAdditionalCameraLocations()
                search(image("first"))
                assertOverlays(active, additional, cameras)
                search()
                assertImage(null)
                assertOverlays(false, false, false)
                // Repeated empty results must not overwrite remembered choices.
                search()
                coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image("feature"))
                scope.launch { state.fetchImage(feature).getOrThrow() }
                assertImage("feature")
                assertOverlays(active, additional, cameras)
            }
        }
    }

    @Test
    fun searchAndFetchErrorsClearChecksWithoutLosingPreferences() {
        compose.runOnIdle {
            state.toggleAdditionalCameraLocations()
            coEvery { layer.searchImages(any(), any()) } returns Result.failure(IllegalStateException("Search failed"))
            scope.launch { assertTrue(state.searchImages(location).isFailure) }
            assertOverlays(false, false, false)
            coEvery { layer.fetchImageForFeature(feature) } returns Result.failure(IllegalStateException("Fetch failed"))
            scope.launch { assertTrue(state.fetchImage(feature).isFailure) }
            assertOverlays(false, false, false)
            search(image("recovered"))
            assertOverlays(true, false, false)
        }
    }

    @Test
    fun imageLoadFailureClearsChecksAndSuccessfulSearchRestoresThem() {
        compose.runOnIdle {
            state.toggleAdditionalFootprints()
            val broken = image("broken")
            coEvery { broken.load() } returns Result.failure(IllegalStateException("Load failed"))
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(broken)
            scope.launch { assertTrue(state.fetchImage(feature).isFailure) }
            assertOverlays(false, false, false)
            search(image("recovered"))
            assertOverlays(true, true, true)
        }
    }

    @Test
    fun pendingSearchUsesLatestOverlayChoices() {
        compose.runOnIdle {
            val pending = CompletableDeferred<Result<List<OrientedImage>>>()
            coEvery { layer.searchImages(any(), any()) } coAnswers { pending.await() }
            scope.launch { state.searchImages(location).getOrThrow() }
            assertOverlays(true, false, true)
            state.toggleActiveFootprint()
            state.toggleAdditionalFootprints()
            pending.complete(Result.success(listOf(image("loaded"))))
            assertImage("loaded")
            assertOverlays(false, true, true)
        }
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
            pending.complete(Result.success(listOf(old, image("obsolete additional"))))
            assertTrue(oldJob.isCancelled)
            assertImage("latest")
            assertFalse(state.uiState.hasAdditionalImages)
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
            val pending = CompletableDeferred<Result<OrientedImage>>()
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } coAnswers { pending.await() }
            coEvery { layer.fetchAdjacentImage(second, SequenceStep.Next) } returns Result.success(third)
            state.setSequentialNavigationEnabled(true)
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
    fun boundaryNavigationPublishesSearchResultIndicesAndPreservesFailedSelection() {
        compose.runOnIdle {
            val first = image("first")
            val last = image("last")
            search(first, image("middle"), last)
            assertEquals(3, state.uiState.searchResultCount)
            assertEquals(0, state.uiState.selectedResultIndex)
            scope.launch {
                assertEquals(ImageNavigationStatus.BoundaryReached, state.navigateFirstImage().getOrThrow())
                assertEquals(ImageNavigationStatus.Moved, state.navigateLastImage().getOrThrow())
                assertImage("last")
                assertEquals(2, state.uiState.selectedResultIndex)
                coEvery { first.load() } returns Result.failure(IllegalStateException("Load failed"))
                assertTrue(state.navigateFirstImage().isFailure)
                assertImage("last")
                assertEquals(2, state.uiState.selectedResultIndex)
                coEvery { first.load() } returns Result.success(Unit)
                assertEquals(ImageNavigationStatus.Moved, state.navigateFirstImage().getOrThrow())
                assertImage("first")
                assertEquals(0, state.uiState.selectedResultIndex)
            }
        }
    }

    @Test
    fun sequentialModeClearsSearchContextAndPreservesDisplayedImageOnExit() {
        compose.runOnIdle {
            val selected = image("selected")
            val adjacent = image("adjacent")
            search(image("first"), selected, image("last"))
            scope.launch { state.navigateNextImage().getOrThrow() }
            coEvery { layer.fetchAdjacentImage(selected, SequenceStep.Next) } returns Result.success(adjacent)
            state.setSequentialNavigationEnabled(true)
            assertNull(state.inputLocationData)
            assertFalse(state.uiState.hasAdditionalImages)
            assertEquals(listOf(selected), state.searchResultImages)
            assertEquals(1, overlay.graphics.size)
            assertEquals(1, footprints.size)
            scope.launch { state.navigateNextImage().getOrThrow() }
            state.setInputLocation(otherLocation)
            assertImage("adjacent")
            assertEquals(0, state.uiState.selectedResultIndex)
            scope.launch {
                assertEquals(ImageNavigationStatus.BoundaryReached, state.navigateFirstImage().getOrThrow())
                assertEquals(ImageNavigationStatus.BoundaryReached, state.navigateLastImage().getOrThrow())
            }
            state.setSequentialNavigationEnabled(false)
            assertImage("adjacent")
            assertEquals(otherLocation.x, state.inputLocationData!!.x, 0.0)
            assertEquals(listOf(adjacent), state.searchResultImages)
            assertEquals(0, state.uiState.selectedResultIndex)
            coVerify(exactly = 1) { selected.load() }
            coVerify(exactly = 1) { adjacent.load() }
            val revision = state.uiState.activeImageRevision
            state.setSequentialNavigationEnabled(false)
            assertEquals(revision, state.uiState.activeImageRevision)
            scope.launch {
                assertEquals(ImageNavigationStatus.BoundaryReached, state.navigateNextImage().getOrThrow())
            }
            assertImage("adjacent")
        }
    }

    @Test
    fun leavingSequentialModeInvalidatesRunningAndQueuedAdjacentRequests() {
        compose.runOnIdle {
            val first = image("first")
            search(first, image("second"))
            val pending = CompletableDeferred<Result<OrientedImage>>()
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } coAnswers { pending.await() }
            state.setSequentialNavigationEnabled(true)
            val running = scope.launch { state.navigateNextImage() }
            val queued = scope.launch { state.navigatePreviousImage() }
            state.setSequentialNavigationEnabled(false)
            assertImage("first")
            pending.complete(Result.success(image("obsolete adjacent")))
            assertTrue(running.isCancelled)
            assertTrue(queued.isCancelled)
            assertImage("first")
            assertEquals(0, state.uiState.selectedResultIndex)
        }
    }

    @Test
    fun enteringSequentialModeInvalidatesPendingResultLoad() {
        compose.runOnIdle {
            val second = image("second")
            search(image("first"), second)
            val pending = CompletableDeferred<Result<Unit>>()
            coEvery { second.load() } coAnswers { pending.await() }
            val running = scope.launch { state.navigateLastImage() }
            state.setSequentialNavigationEnabled(true)
            pending.complete(Result.success(Unit))
            assertTrue(running.isCancelled)
            assertImage("first")
            state.setSequentialNavigationEnabled(false)
            assertImage("first")
            assertEquals(0, state.uiState.selectedResultIndex)
        }
    }

    @Test
    fun resetClearsSequentialModeAndResultSelection() {
        compose.runOnIdle {
            search(image("first"), image("second"))
            state.setSequentialNavigationEnabled(true)
            state.resetAll()
            assertFalse(state.uiState.isSequentialNavigationEnabled)
            assertEquals(0, state.uiState.searchResultCount)
            assertEquals(-1, state.uiState.selectedResultIndex)
            assertTrue(state.searchResultImages.isEmpty())
            assertFalse(state.uiState.canNavigatePrevious)
            assertFalse(state.uiState.canNavigateNext)
        }
    }

    @Test
    fun adjacentPrefetchesResolveIndependentlyWithoutLoadingImages() {
        compose.runOnIdle {
            val first = image("first")
            val previous = image("previous")
            val next = image("next")
            val previousFetch = CompletableDeferred<Result<OrientedImage>>()
            val nextFetch = CompletableDeferred<Result<OrientedImage>>()
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Previous) } coAnswers { previousFetch.await() }
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } coAnswers { nextFetch.await() }
            search(first)
            state.setSequentialNavigationEnabled(true)
            assertFalse(state.uiState.canNavigatePrevious)
            assertFalse(state.uiState.canNavigateNext)
            coVerify(exactly = 1) { layer.fetchAdjacentImage(first, SequenceStep.Previous) }
            coVerify(exactly = 1) { layer.fetchAdjacentImage(first, SequenceStep.Next) }
            previousFetch.complete(Result.success(previous))
            assertTrue(state.uiState.canNavigatePrevious)
            assertFalse(state.uiState.canNavigateNext)
            nextFetch.complete(Result.success(next))
            assertTrue(state.uiState.canNavigateNext)
            coVerify(exactly = 0) { previous.load() }
            coVerify(exactly = 0) { next.load() }
            scope.launch { state.navigateNextImage().getOrThrow() }
            assertImage("next")
            coVerify(exactly = 1) { next.load() }
            coVerify(exactly = 1) { layer.fetchAdjacentImage(first, SequenceStep.Next) }
            coVerify(exactly = 1) { layer.fetchAdjacentImage(next, SequenceStep.Previous) }
            coVerify(exactly = 1) { layer.fetchAdjacentImage(next, SequenceStep.Next) }
            assertFalse(state.uiState.canNavigatePrevious)
            assertFalse(state.uiState.canNavigateNext)
        }
    }

    @Test
    fun expectedBoundaryIsSilentButUnexpectedFetchErrorIsPublished() {
        compose.runOnIdle {
            val first = image("first")
            val failure = IllegalStateException("Network unavailable")
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } returns Result.failure(failure)
            search(first)
            state.setSequentialNavigationEnabled(true)
            assertFalse(state.uiState.canNavigatePrevious)
            assertFalse(state.uiState.canNavigateNext)
            assertNull(state.uiState.previousImageError)
            assertEquals("Network unavailable", state.uiState.nextImageError)
            scope.launch {
                assertEquals(ImageNavigationStatus.BoundaryReached, state.navigatePreviousImage().getOrThrow())
                assertEquals(failure, state.navigateNextImage().exceptionOrNull())
            }
            state.setSequentialNavigationEnabled(false)
            assertNull(state.uiState.nextImageError)
        }
    }

    @Test
    fun boundaryMessageWithoutSdkNoDataCodeIsAnUnexpectedError() {
        compose.runOnIdle {
            val first = image("first")
            val failure = mockk<ArcGISException>()
            every { failure.errorCode } returns 19
            every { failure.message } returns "No adjacent image query result is available"
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } returns Result.failure(failure)
            search(first)
            state.setSequentialNavigationEnabled(true)
            assertEquals(failure.message, state.uiState.nextImageError)
            assertFalse(state.uiState.canNavigateNext)
        }
    }

    @Test
    fun adjacentLoadFailurePreservesImageAndAllowsRetryWithoutRefetching() {
        compose.runOnIdle {
            val first = image("first")
            val adjacent = image("adjacent")
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } returns Result.success(adjacent)
            coEvery { adjacent.load() } returns Result.failure(IllegalStateException("Load failed"))
            search(first)
            state.setSequentialNavigationEnabled(true)
            assertTrue(state.uiState.canNavigateNext)
            scope.launch {
                assertTrue(state.navigateNextImage().isFailure)
                assertImage("first")
                coEvery { adjacent.load() } returns Result.success(Unit)
                state.navigateNextImage().getOrThrow()
                assertImage("adjacent")
                coVerify(exactly = 1) { layer.fetchAdjacentImage(first, SequenceStep.Next) }
            }
        }
    }

    @Test
    fun exitingDuringAdjacentLoadKeepsCommittedImageAndCancelsNavigation() {
        compose.runOnIdle {
            val first = image("first")
            val adjacent = image("adjacent")
            val pending = CompletableDeferred<Result<Unit>>()
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } returns Result.success(adjacent)
            coEvery { adjacent.load() } coAnswers { pending.await() }
            search(first)
            state.setSequentialNavigationEnabled(true)
            val running = scope.launch { state.navigateNextImage() }
            state.setSequentialNavigationEnabled(false)
            pending.complete(Result.success(Unit))
            assertTrue(running.isCancelled)
            assertImage("first")
            assertEquals(listOf(first), state.searchResultImages)
        }
    }

    @Test
    fun obsoletePrefetchCannotPublishAvailabilityOrErrorsAfterReplacement() {
        compose.runOnIdle {
            val first = image("first")
            val pending = CompletableDeferred<Result<OrientedImage>>()
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } coAnswers {
                withContext(NonCancellable) { pending.await() }
            }
            search(first)
            state.setSequentialNavigationEnabled(true)
            state.resetAll()
            search(image("replacement"))
            pending.complete(Result.failure(IllegalStateException("Obsolete error")))
            assertImage("replacement")
            assertFalse(state.uiState.canNavigateNext)
            assertNull(state.uiState.nextImageError)
        }
    }

    @Test
    fun enteringSequentialModeBeforeSearchingStillPreservesDisplayedImageOnExit() {
        compose.runOnIdle {
            val first = image("first")
            val adjacent = image("adjacent")
            coEvery { layer.fetchAdjacentImage(first, SequenceStep.Next) } returns Result.success(adjacent)
            state.setSequentialNavigationEnabled(true)
            search(first, image("additional"))
            assertFalse(state.uiState.hasAdditionalImages)
            scope.launch { state.navigateNextImage().getOrThrow() }
            state.setSequentialNavigationEnabled(false)
            assertImage("adjacent")
            assertEquals(listOf(adjacent), state.searchResultImages)
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
            assertTrue(foreignGraphic in overlay.graphics)
            assertTrue(foreignFootprint in footprints)
            state.resetAll()
            assertEquals(listOf(foreignGraphic), overlay.graphics.toList())
            assertEquals(listOf(foreignFootprint), footprints)
        }
    }
}
