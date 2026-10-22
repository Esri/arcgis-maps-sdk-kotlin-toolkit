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
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.raster.Raster
import com.arcgismaps.raster.RasterInfo
import com.arcgismaps.toolkit.orientedimageryviewer.internal.components.computeRasterPoint
import com.arcgismaps.toolkit.orientedimageryviewer.internal.components.computePixelCorners
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ImageLocationMarkerTests {
    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var state: OrientedImageryViewerState
    private lateinit var layer: OrientedImageryLayer
    private lateinit var image: OrientedImage
    private lateinit var mapOverlay: GraphicsOverlay
    private lateinit var imageOverlay: GraphicsOverlay
    private var tapResult: Result<Unit>? = null
    private val location = Point(8.0, 8.0, SpatialReference.wgs84())

    @Before
    fun setUp() {
        image = mockk()
        every { image.dataUri } returns "/local/image.png"
        every { image.type } returns OrientedImageType.Horizontal
        every { image.geometry } returns Point(0.0, 0.0, SpatialReference.wgs84())
        every { image.attributes } returns mutableMapOf("OBJECTID" to 42L)
        // Even an uncovered location can project to valid, in-bounds pixels.
        coEvery { image.locationToImage(any()) } returns Result.success(DoubleXY(5.0, 5.0))
        layer = mockk()
        every { layer.name } returns "Markers"
        every { layer.supportsSequentialNavigation } returns false
        val table = mockk<ArcGISFeatureTable>()
        every { table.objectIdField } returns "OBJECTID"
        every { layer.featureTable } returns table
        coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(image))
        compose.setContent { }
        compose.runOnIdle {
            mapOverlay = GraphicsOverlay()
            imageOverlay = GraphicsOverlay()
            state = OrientedImageryViewerState(layer, mapOverlay, scope)
            OrientedImageryViewerState::class.java.getDeclaredMethod(
                "setActiveOrientedImage", OrientedImage::class.java
            ).apply { isAccessible = true }.invoke(state, image)
            val rasterLayer = rasterLayer()
            state.registerMarkerOverlayInImage(imageOverlay) { computeRasterPoint(it, rasterLayer) }
            state.addMarker()
        }
    }

    @After
    fun tearDown() {
        compose.runOnIdle { scope.cancel() }
    }

    @Test
    fun uncoveredLocationDoesNotPlaceDiamondEvenWithInBoundsProjection() {
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } returns Result.success(emptyList())
            tap()
            assertEquals(1, mapOverlay.graphics.size)
            assertEquals(0, imageOverlay.graphics.size)
            coVerify(exactly = 1) { image.locationToImage(location) }
            coVerify(exactly = 1) { layer.searchImages(location, any()) }
        }
    }

    @Test
    fun coveredLocationPlacesDiamondEvenWhenFootprintIsHidden() {
        compose.runOnIdle {
            assertFalse(state.isActiveFootprintVisible())
            tap()
            assertEquals(1, mapOverlay.graphics.size)
            assertEquals(1, imageOverlay.graphics.size)
            assertTrue(tapResult?.isSuccess == true)
            coVerify(exactly = 1) {
                layer.searchImages(location, match { it.maxResults == -1 && !it.applyHeightFilter })
            }
        }
    }

    @Test
    fun coverageByAnotherImageDoesNotPlaceDiamond() {
        val other = mockk<OrientedImage>()
        every { other.attributes } returns mutableMapOf("OBJECTID" to 43L)
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(other))
            tap()
            assertEquals(1, mapOverlay.graphics.size)
            assertEquals(0, imageOverlay.graphics.size)
        }
    }

    @Test
    fun matchingObjectIdPlacesDiamondWithoutLoadingCandidate() {
        val candidate = mockk<OrientedImage>()
        every { candidate.attributes } returns mutableMapOf("OBJECTID" to 42L)
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(candidate))
            tap()
            assertEquals(1, imageOverlay.graphics.size)
            coVerify(exactly = 0) { candidate.load() }
        }
    }

    @Test
    fun matchingLoadedUriPlacesDiamondWhenObjectIdIsUnavailable() {
        val candidate = mockk<OrientedImage>()
        every { candidate.dataUri } returns "/local/image.png"
        coEvery { candidate.load() } returns Result.success(Unit)
        compose.runOnIdle {
            every { layer.featureTable } returns null
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(candidate))
            tap()
            assertEquals(1, imageOverlay.graphics.size)
            coVerify(exactly = 1) { candidate.load() }
        }
    }

    @Test
    fun failedCoverageSearchDoesNotPlaceDiamond() {
        compose.runOnIdle {
            val error = IllegalStateException("Unavailable")
            coEvery { layer.searchImages(any(), any()) } returns Result.failure(error)
            tap()
            assertEquals(error, tapResult?.exceptionOrNull())
            assertEquals(1, mapOverlay.graphics.size)
            assertEquals(0, imageOverlay.graphics.size)
        }
    }

    @Test
    fun outOfBoundsProjectionDoesNotPlaceDiamond() {
        compose.runOnIdle {
            coEvery { image.locationToImage(any()) } returns Result.success(DoubleXY(11.0, 5.0))
            tap()
            assertEquals(0, imageOverlay.graphics.size)
            assertEquals(1, mapOverlay.graphics.size)
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
        }
    }

    @Test
    fun coverageValidationDoesNotResetViewerOrChangeActiveImage() {
        compose.runOnIdle {
            val revision = state.uiState.activeImageRevision
            tap()
            assertEquals(1, imageOverlay.graphics.size)
            assertEquals(revision, state.uiState.activeImageRevision)
        }
    }

    @Test
    fun failedProjectionDoesNotPlaceDiamond() {
        compose.runOnIdle {
            val error = IllegalStateException("Unavailable")
            coEvery { image.locationToImage(any()) } returns Result.failure(error)
            tap()
            assertEquals(error, tapResult?.exceptionOrNull())
            assertEquals(0, imageOverlay.graphics.size)
            assertEquals(1, mapOverlay.graphics.size)
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
        }
    }

    @Test
    fun candidateLoadFailureIsReturned() {
        val candidate = mockk<OrientedImage>()
        val error = IllegalStateException("Candidate unavailable")
        coEvery { candidate.load() } returns Result.failure(error)
        compose.runOnIdle {
            every { layer.featureTable } returns null
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(candidate))
            tap()
            assertEquals(error, tapResult?.exceptionOrNull())
            assertEquals(0, imageOverlay.graphics.size)
        }
    }

    @Test
    fun projectionCancellationIsRethrown() {
        compose.runOnIdle {
            coEvery { image.locationToImage(any()) } returns Result.failure(CancellationException("Cancelled"))
            val job = tap()
            assertTrue(job.isCancelled)
            assertNull(tapResult)
        }
    }

    @Test
    fun tapAwaitsProjectionCompletion() {
        val projection = CompletableDeferred<Result<DoubleXY>>()
        compose.runOnIdle {
            coEvery { image.locationToImage(any()) } coAnswers { projection.await() }
            tap()
            assertNull(tapResult)
            projection.complete(Result.success(DoubleXY(5.0, 5.0)))
        }
        compose.runOnIdle {
            assertTrue(tapResult?.isSuccess == true)
            assertEquals(1, imageOverlay.graphics.size)
        }
    }

    @Test
    fun overlayReplacementDuringProjectionDiscardsDiamond() {
        val projection = CompletableDeferred<Result<DoubleXY>>()
        lateinit var replacement: GraphicsOverlay
        compose.runOnIdle {
            coEvery { image.locationToImage(any()) } coAnswers { projection.await() }
            tap()
            replacement = GraphicsOverlay()
            state.registerMarkerOverlayInImage(replacement) { Point(it.x, -it.y) }
            projection.complete(Result.success(DoubleXY(5.0, 5.0)))
        }
        compose.runOnIdle {
            assertEquals(0, imageOverlay.graphics.size)
            assertEquals(0, replacement.graphics.size)
        }
    }

    @Test
    fun imageChangeDuringProjectionDiscardsDiamond() {
        val projection = CompletableDeferred<Result<DoubleXY>>()
        compose.runOnIdle {
            coEvery { image.locationToImage(any()) } coAnswers { projection.await() }
            tap()
            OrientedImageryViewerState::class.java.getDeclaredMethod(
                "setActiveOrientedImage", OrientedImage::class.java
            ).apply { isAccessible = true }.invoke(state, image)
            projection.complete(Result.success(DoubleXY(5.0, 5.0)))
        }
        compose.runOnIdle { assertEquals(0, imageOverlay.graphics.size) }
    }

    @Test
    fun disablingMarkerModeDuringProjectionDiscardsDiamond() {
        val projection = CompletableDeferred<Result<DoubleXY>>()
        compose.runOnIdle {
            coEvery { image.locationToImage(any()) } coAnswers { projection.await() }
            tap()
            state.addMarker()
            projection.complete(Result.success(DoubleXY(5.0, 5.0)))
        }
        compose.runOnIdle { assertEquals(0, imageOverlay.graphics.size) }
    }

    @Test
    fun imageChangeDuringCoverageSearchDiscardsDiamond() {
        val coverage = CompletableDeferred<Result<List<OrientedImage>>>()
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } coAnswers { coverage.await() }
            tap()
            OrientedImageryViewerState::class.java.getDeclaredMethod(
                "setActiveOrientedImage", OrientedImage::class.java
            ).apply { isAccessible = true }.invoke(state, image)
            coverage.complete(Result.success(listOf(image)))
        }
        compose.runOnIdle { assertEquals(0, imageOverlay.graphics.size) }
    }

    @Test
    fun overlayReplacementDuringCoverageSearchDiscardsDiamond() {
        val coverage = CompletableDeferred<Result<List<OrientedImage>>>()
        lateinit var replacement: GraphicsOverlay
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } coAnswers { coverage.await() }
            tap()
            replacement = GraphicsOverlay()
            state.registerMarkerOverlayInImage(replacement) { Point(it.x, -it.y) }
            coverage.complete(Result.success(listOf(image)))
        }
        compose.runOnIdle {
            assertEquals(0, imageOverlay.graphics.size)
            assertEquals(0, replacement.graphics.size)
        }
    }

    @Test
    fun disablingMarkerModeDuringCoverageSearchDiscardsDiamond() {
        val coverage = CompletableDeferred<Result<List<OrientedImage>>>()
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } coAnswers { coverage.await() }
            tap()
            state.addMarker()
            coverage.complete(Result.success(listOf(image)))
        }
        compose.runOnIdle { assertEquals(0, imageOverlay.graphics.size) }
    }

    @Test
    fun rasterConversionUsesExtentAndCellSizesAndIncludesImageEdges() {
        val rasterLayer = rasterLayer()
        val topLeft = requireNotNull(computeRasterPoint(DoubleXY(0.0, 0.0), rasterLayer))
        val bottomRight = requireNotNull(computeRasterPoint(DoubleXY(10.0, 10.0), rasterLayer))
        assertEquals(100.0, topLeft.x, 0.0)
        assertEquals(240.0, topLeft.y, 0.0)
        assertEquals(120.0, bottomRight.x, 0.0)
        assertEquals(200.0, bottomRight.y, 0.0)
    }

    @Test
    fun rasterConversionRejectsOutOfBoundsAndNonfinitePixels() {
        val rasterLayer = rasterLayer()
        for (pixel in listOf(
            DoubleXY(-1.0, 5.0), DoubleXY(11.0, 5.0),
            DoubleXY(5.0, -1.0), DoubleXY(5.0, 11.0),
            DoubleXY(Double.NaN, 5.0), DoubleXY(5.0, Double.POSITIVE_INFINITY)
        )) {
            assertNull(computeRasterPoint(pixel, rasterLayer))
        }
    }

    @Test
    fun footprintClippingAcceptsDifferentVerticalReferences() {
        val vertical = SpatialReference(3857, 5773)
        val otherVertical = SpatialReference(3857, 115700)
        val horizontal = SpatialReference(3857)
        for ((rasterSr, viewSr) in listOf(
            vertical to otherVertical, otherVertical to vertical,
            vertical to horizontal, horizontal to vertical
        )) {
            val rasterLayer = rasterLayer(rasterSr)
            val viewpoint = Viewpoint(Envelope(106.0, 212.0, 114.0, 228.0, spatialReference = viewSr))
            val corners = requireNotNull(computePixelCorners(viewpoint, rasterLayer))
            assertEquals(3.0, corners.minOf { it.x }, 1e-8)
            assertEquals(7.0, corners.maxOf { it.x }, 1e-8)
            assertEquals(3.0, corners.minOf { it.y }, 1e-8)
            assertEquals(7.0, corners.maxOf { it.y }, 1e-8)
        }
    }

    @Test
    fun footprintClippingAcceptsDifferentVerticalReferencesAndRotation() {
        val rasterLayer = rasterLayer(SpatialReference(3857, 5773))
        for (rotation in listOf(0.0, 45.0, 90.0)) {
            val bounds = Envelope(106.0, 212.0, 114.0, 228.0, spatialReference = SpatialReference(3857, 115700))
            val viewpoint = Viewpoint(bounds, rotation)
            val corners = requireNotNull(computePixelCorners(viewpoint, rasterLayer))
            assertTrue(corners.all { it.x in 0.0..10.0 && it.y in 0.0..10.0 })
        }
        val halfImage = Viewpoint(Envelope(
            100.0, 200.0, 110.0, 240.0, spatialReference = SpatialReference(3857, 115700)
        ))
        val clipped = requireNotNull(computePixelCorners(halfImage, rasterLayer))
        assertEquals(0.0, clipped.minOf { it.x }, 1e-8)
        assertEquals(5.0, clipped.maxOf { it.x }, 1e-8)
    }

    @Test
    fun viewportWithDifferentHorizontalReferenceIsProjectedRatherThanRelabeled() {
        val rasterLayer = rasterLayer(SpatialReference(3857, 5773))
        val geographicBounds = requireNotNull(GeometryEngine.projectOrNull(
            Envelope(106.0, 212.0, 114.0, 228.0, spatialReference = SpatialReference(3857)),
            SpatialReference(4326, 115700)
        ))
        val viewpoint = Viewpoint(geographicBounds)
        assertNotNull(computePixelCorners(viewpoint, rasterLayer))
    }

    private fun rasterLayer(spatialReference: SpatialReference? = null): RasterLayer {
        val info = mockk<RasterInfo>()
        val extent = Envelope(100.0, 200.0, 120.0, 240.0, spatialReference = spatialReference)
        every { info.extent } returns extent
        every { info.cellSizeX } returns 2.0
        every { info.cellSizeY } returns 4.0
        val raster = mockk<Raster>()
        every { raster.rasterInfo } returns info
        val rasterLayer = mockk<RasterLayer>()
        every { rasterLayer.raster } returns raster
        every { rasterLayer.spatialReference } returns spatialReference
        every { rasterLayer.fullExtent } returns extent
        every { rasterLayer.isVisible } returns true
        return rasterLayer
    }

    private fun tap() = scope.launch {
        tapResult = state.onSingleTapOnGeoView(location)
    }
}
