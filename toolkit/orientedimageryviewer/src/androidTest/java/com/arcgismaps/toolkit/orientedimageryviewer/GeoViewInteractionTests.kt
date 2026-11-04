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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GeoViewInteractionTests {
    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val location = Point(1.0, 2.0)
    private lateinit var state: OrientedImageryViewerState
    private lateinit var layer: OrientedImageryLayer
    private lateinit var overlay: GraphicsOverlay
    private lateinit var proxy: MapViewProxy
    private lateinit var event: SingleTapConfirmedEvent
    private lateinit var identified: IdentifyLayerResult
    private lateinit var feature: Feature
    private var showViewer by mutableStateOf(false)

    @Before
    fun setUp() {
        compose.setContent {
            if (showViewer) ViewerLayout(viewerState = state)
        }
        compose.runOnIdle {
            layer = mockk()
            every { layer.name } returns "Interaction"
            every { layer.supportsSequentialNavigation } returns false
            every { layer.visibleFootprints } returns mutableListOf()
            overlay = GraphicsOverlay()
            state = OrientedImageryViewerState(layer, overlay, scope)
            state.toggleActiveFootprint()
            state.toggleAdditionalCameraLocations()
            proxy = mockk()
            event = mockk()
            every { event.mapPoint } returns location
            every { event.screenCoordinate } returns mockk()
            feature = mockk()
            every { feature.geometry } returns location
            identified = mockk()
            every { identified.error } returns null
            every { identified.geoElements } returns listOf(feature)
            coEvery { proxy.identify(layer, event.screenCoordinate, 20.dp, false, 1) } returns
                Result.success(identified)
            coEvery { layer.searchImages(any(), any()) } returns Result.success(emptyList())
            val image = mockk<OrientedImage>()
            every { image.dataUri } returns "/local/image.png"
            every { image.type } returns OrientedImageType.Horizontal
            every { image.geometry } returns location
            coEvery { image.load() } returns Result.success(Unit)
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image)
        }
    }

    @After
    fun tearDown() {
        compose.runOnIdle { scope.cancel() }
    }

    @Test
    fun defaultsToFindImagesAndSearchesAtTapLocation() {
        compose.runOnIdle {
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            coVerify(exactly = 1) { layer.searchImages(location, any()) }
            coVerify(exactly = 0) { proxy.identify(layer, any(), any(), any(), any()) }
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
        }
    }

    @Test
    fun noneDoesNotReadCoordinatesOrPerformOperations() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.None
            val ignoredEvent = mockk<SingleTapConfirmedEvent>()
            scope.launch { state.onSingleTapOnGeoView(ignoredEvent, proxy).getOrThrow() }
            verify(exactly = 0) { ignoredEvent.mapPoint; ignoredEvent.screenCoordinate }
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
            coVerify(exactly = 0) { proxy.identify(layer, any(), any(), any(), any()) }
            assertTrue(overlay.graphics.isEmpty())
        }
    }

    @Test
    fun displayIdentifiesFirstFeatureWithoutRequiringMapPointAndPreservesMode() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            every { event.mapPoint } returns null
            every { identified.geoElements } returns listOf(feature, mockk<Feature>())
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            coVerify(exactly = 1) { proxy.identify(layer, event.screenCoordinate, 20.dp, false, 1) }
            coVerify(exactly = 1) { layer.fetchImageForFeature(feature) }
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
            assertTrue(state.hasActiveImage)
            assertNull(state.uiState.messageResourceId)
            assertEquals(GeoViewInteractionStatus.DisplayImage, state.geoViewInteractionStatus)
        }
    }

    @Test
    fun identifyMissClearsActiveImage() {
        compose.runOnIdle {
            scope.launch { state.fetchImage(feature).getOrThrow() }
            val revision = state.uiState.activeImageRevision
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            every { identified.geoElements } returns emptyList()
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            assertTrue(state.uiState.activeImageRevision > revision)
            assertFalse(state.hasActiveImage)
            assertTrue(state.searchResultImages.isEmpty())
            assertEquals(R.string.no_images_found_for_tapped_location, state.uiState.messageResourceId)
            coVerify(exactly = 1) { layer.fetchImageForFeature(feature) }
        }
    }

    @Test
    fun identifyMissShowsPersistentMessageScreen() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            every { identified.geoElements } returns emptyList()
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            showViewer = true
        }
        compose.onNodeWithTag("MessageScreen").assertIsDisplayed()
        compose.onNodeWithText("No images found for the tapped location").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(R.string.no_images_found_for_tapped_location, state.uiState.messageResourceId)
        }
    }

    @Test
    fun identifyFailureIsReturned() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            val error = IllegalStateException("Identify failed")
            coEvery { proxy.identify(layer, any(), any(), any(), any()) } returns Result.failure(error)
            scope.launch { assertSame(error, state.onSingleTapOnGeoView(event, proxy).exceptionOrNull()) }
            assertEquals(R.string.image_fetch_failed, state.uiState.messageResourceId)
            assertEquals("Identify failed", state.uiState.message)
            coVerify(exactly = 0) { layer.fetchImageForFeature(any()) }
        }
    }

    @Test
    fun embeddedIdentifyErrorNotifiesWithoutFetching() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            val error = IllegalStateException("Layer identify failed")
            every { identified.error } returns error
            scope.launch { assertSame(error, state.onSingleTapOnGeoView(event, proxy).exceptionOrNull()) }
            assertEquals(R.string.image_fetch_failed, state.uiState.messageResourceId)
            assertEquals("Layer identify failed", state.uiState.message)
            coVerify(exactly = 0) { layer.fetchImageForFeature(any()) }
        }
    }

    @Test
    fun nonFeatureFirstResultNotifiesWithoutFetchingLaterFeature() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            every { identified.geoElements } returns listOf(Graphic(), feature)
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            assertEquals(R.string.no_images_found_for_tapped_location, state.uiState.messageResourceId)
            coVerify(exactly = 0) { layer.fetchImageForFeature(any()) }
        }
    }

    @Test
    fun repeatedMissesRemainVisibleUntilReset() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            every { identified.geoElements } returns emptyList()
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            assertEquals(R.string.no_images_found_for_tapped_location, state.uiState.messageResourceId)
            state.resetAll()
            assertNull(state.uiState.messageResourceId)
            assertNull(state.uiState.message)
        }
    }

    @Test
    fun cancelledIdentifyDoesNotNotify() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            coEvery { proxy.identify(layer, any(), any(), any(), any()) } returns
                Result.failure(CancellationException("Cancelled"))
            val job = scope.launch { state.onSingleTapOnGeoView(event, proxy) }
            assertTrue(job.isCancelled)
            assertNull(state.uiState.messageResourceId)
        }
    }

    @Test
    fun addMarkersAddsMapMarkerWithoutIdentifyingOrSearching() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.AddMarkers
            scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            assertTrue(state.isMarkingLocation)
            assertFalse(state.uiState.isFindImagesSelected)
            assertFalse(state.uiState.isDisplayImageSelected)
            assertEquals(location, overlay.graphics.single().geometry)
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
            coVerify(exactly = 0) { proxy.identify(layer, any(), any(), any(), any()) }
        }
    }

    @Test
    fun markerToggleClearsImageModesAndReturnsToNone() {
        compose.runOnIdle {
            for (mode in listOf(GeoViewInteractionStatus.FindImages, GeoViewInteractionStatus.DisplayImage)) {
                state.geoViewInteractionStatus = mode
                state.addMarker()
                assertTrue(state.isMarkingLocation)
                assertEquals(GeoViewInteractionStatus.AddMarkers, state.geoViewInteractionStatus)
                assertFalse(state.uiState.isFindImagesSelected)
                assertFalse(state.uiState.isDisplayImageSelected)
                state.addMarker()
                assertFalse(state.isMarkingLocation)
                assertEquals(GeoViewInteractionStatus.None, state.geoViewInteractionStatus)
                assertFalse(state.uiState.isFindImagesSelected)
                assertFalse(state.uiState.isDisplayImageSelected)
                scope.launch { state.onSingleTapOnGeoView(event, proxy).getOrThrow() }
            }
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
            coVerify(exactly = 0) { proxy.identify(layer, any(), any(), any(), any()) }
        }
    }

    @Test
    fun nullMapLocationReturnsFailureForFindImages() {
        compose.runOnIdle {
            every { event.mapPoint } returns null
            scope.launch { assertTrue(state.onSingleTapOnGeoView(event, proxy).isFailure) }
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
        }
    }

    @Test
    fun sceneTapResolvesLocationThroughProxy() {
        compose.runOnIdle {
            val sceneProxy = mockk<SceneViewProxy>()
            val sceneLocation = Point(3.0, 4.0, 5.0)
            coEvery { sceneProxy.screenToLocation(event.screenCoordinate) } returns Result.success(sceneLocation)
            scope.launch { state.onSingleTapOnGeoView(event, sceneProxy).getOrThrow() }
            coVerify(exactly = 1) { layer.searchImages(sceneLocation, any()) }
        }
    }

    @Test
    fun resetInvalidatesPendingIdentify() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            val pending = CompletableDeferred<Result<IdentifyLayerResult>>()
            coEvery { proxy.identify(layer, any(), any(), any(), any()) } coAnswers { pending.await() }
            val job = scope.launch { state.onSingleTapOnGeoView(event, proxy) }
            state.resetAll()
            pending.complete(Result.success(identified))
            assertTrue(job.isCancelled)
            assertFalse(state.hasActiveImage)
            assertNull(state.uiState.messageResourceId)
            assertEquals(GeoViewInteractionStatus.FindImages, state.geoViewInteractionStatus)
            coVerify(exactly = 0) { layer.fetchImageForFeature(any()) }
        }
    }

    @Test
    fun changingModeInvalidatesPendingIdentify() {
        compose.runOnIdle {
            state.geoViewInteractionStatus = GeoViewInteractionStatus.DisplayImage
            val pending = CompletableDeferred<Result<IdentifyLayerResult>>()
            coEvery { proxy.identify(layer, any(), any(), any(), any()) } coAnswers { pending.await() }
            val job = scope.launch { state.onSingleTapOnGeoView(event, proxy) }
            state.geoViewInteractionStatus = GeoViewInteractionStatus.None
            pending.complete(Result.success(identified))
            assertTrue(job.isCancelled)
            assertNull(state.uiState.messageResourceId)
            coVerify(exactly = 0) { layer.fetchImageForFeature(any()) }
        }
    }

    @Test
    fun constructedStateDisplaysInitialGuidanceAndToolbar() {
        compose.runOnIdle { showViewer = true }
        compose.onNodeWithTag("MessageScreen").assertIsDisplayed()
        compose.onNodeWithText("No Image to display").assertIsDisplayed()
        compose.onNodeWithText("Tap on the map to search", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Mode").assertIsDisplayed()
        compose.onNodeWithTag("OrientedImagePager").assertDoesNotExist()
    }

    @Test
    fun customMessageClearsImageAndOwnedGraphicsAndResetRestoresGuidance() {
        compose.runOnIdle {
            scope.launch { state.fetchImage(feature).getOrThrow() }
            val foreignGraphic = Graphic(location)
            overlay.graphics.add(foreignGraphic)
            state.addImageLocationMarker(location)
            state.showMessage("Unable to display this image.")
            assertFalse(state.hasActiveImage)
            assertTrue(state.searchResultImages.isEmpty())
            assertEquals(listOf(foreignGraphic), overlay.graphics.toList())
            assertEquals("Unable to display this image.", state.uiState.message)
            showViewer = true
        }
        compose.onNodeWithText("Unable to display this image.").assertIsDisplayed()
        compose.runOnIdle { state.resetAll() }
        compose.onNodeWithText("No Image to display").assertIsDisplayed()
    }

    @Test
    fun searchAndFetchFailuresDisplayMessagesAndSuccessClearsThem() {
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } returns Result.failure(IllegalStateException("Search failed"))
            scope.launch { assertTrue(state.searchImages(location).isFailure) }
            assertEquals(R.string.image_search_failed, state.uiState.messageResourceId)
            assertEquals("Search failed", state.uiState.message)
            coEvery { layer.fetchImageForFeature(feature) } returns Result.failure(IllegalStateException("Fetch failed"))
            scope.launch { assertTrue(state.fetchImage(feature).isFailure) }
            assertEquals(R.string.image_fetch_failed, state.uiState.messageResourceId)
            assertEquals("Fetch failed", state.uiState.message)
            val image = mockk<OrientedImage>()
            every { image.dataUri } returns null
            every { image.type } returns OrientedImageType.Horizontal
            every { image.geometry } returns location
            coEvery { image.load() } returns Result.success(Unit)
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image)
            scope.launch { state.fetchImage(feature).getOrThrow() }
            assertTrue(state.hasActiveImage)
            assertNull(state.uiState.message)
            assertNull(state.uiState.messageResourceId)
        }
    }

    @Test
    fun showingMessageInvalidatesPendingImageRequest() {
        compose.runOnIdle {
            val pending = CompletableDeferred<Result<List<OrientedImage>>>()
            coEvery { layer.searchImages(any(), any()) } coAnswers { pending.await() }
            val job = scope.launch { state.searchImages(location) }
            state.showMessage("Request replaced")
            pending.complete(Result.success(emptyList()))
            assertTrue(job.isCancelled)
            assertEquals("Request replaced", state.uiState.message)
            assertNull(state.uiState.messageResourceId)
        }
    }

    @Test
    fun markerLocationFailureRetainsActiveImageAndDoesNotPublishMessage() {
        compose.runOnIdle {
            scope.launch { state.fetchImage(feature).getOrThrow() }
            val revision = state.uiState.activeImageRevision
            state.geoViewInteractionStatus = GeoViewInteractionStatus.AddMarkers
            every { event.mapPoint } returns null
            scope.launch { assertTrue(state.onSingleTapOnGeoView(event, proxy).isFailure) }
            assertTrue(state.hasActiveImage)
            assertEquals(revision, state.uiState.activeImageRevision)
            assertNull(state.uiState.message)
            assertNull(state.uiState.messageResourceId)
        }
    }
}
