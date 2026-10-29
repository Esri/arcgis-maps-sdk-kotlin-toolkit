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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.raster.Raster
import com.arcgismaps.raster.RasterInfo
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerSymbols
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
        coEvery { image.load() } returns Result.success(Unit)
        // Even an uncovered location can project to valid, in-bounds pixels.
        coEvery { image.locationToImage(any()) } returns Result.success(DoubleXY(5.0, 5.0))
        layer = mockk()
        every { layer.name } returns "Markers"
        every { layer.supportsSequentialNavigation } returns false
        every { layer.visibleFootprints } returns mutableListOf()
        val table = mockk<ArcGISFeatureTable>()
        every { table.objectIdField } returns "OBJECTID"
        every { layer.featureTable } returns table
        coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(image))
        compose.setContent { }
        compose.runOnIdle {
            mapOverlay = GraphicsOverlay()
            imageOverlay = GraphicsOverlay()
            state = OrientedImageryViewerState(layer, mapOverlay, scope)
            activateImage()
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
    fun selectedSymbolIsUsedForRasterMarkersFromBothOrigins() {
        compose.runOnIdle {
            val selected = MarkerSymbols.symbols[1]
            state.selectMarkerSymbol(selected)
            tap()
            coEvery { image.imageToLocation(any()) } returns Result.success(location)
            scope.launch {
                state.addMarkerToRasterImage(DoubleXY(3.0, 4.0), Point(3.0, 4.0), imageOverlay)
            }
            assertEquals(2, imageOverlay.graphics.size)
            imageOverlay.graphics.forEach { graphic ->
                val symbol = graphic.symbol as SimpleMarkerSymbol
                assertEquals(selected.style, symbol.style)
                assertEquals(selected.color, symbol.color)
                assertEquals(selected.size, symbol.size)
                assertEquals(selected.outline, symbol.outline)
            }
            state.selectMarkerSymbol(MarkerSymbols.symbols.last())
            assertEquals(selected, imageOverlay.graphics.first().symbol)
        }
    }

    @Test
    fun panoramicMarkersSnapshotSelectedSymbolFromBothOrigins() {
        compose.runOnIdle {
            val revision = state.uiState.activeImageRevision
            state.registerMarkerInPanoramicImage(revision, 20, 10)
            val triangle = MarkerSymbols.symbols[1]
            val diamond = MarkerSymbols.symbols[2]
            state.selectMarkerSymbol(triangle)
            coEvery { image.imageToLocation(any()) } returns Result.success(location)
            scope.launch { state.addMarkerToPanoramicImage(DoubleXY(3.0, 4.0), revision) }
            state.selectMarkerSymbol(diamond)
            tap()
            assertEquals(listOf(triangle, diamond), state.markerPointsInPanoramicImage.map { it.symbol })
            assertEquals(listOf(false, true), state.markerPointsInPanoramicImage.map { it.isFromGeoView })
            state.selectMarkerSymbol(MarkerSymbols.symbols.first())
            assertEquals(triangle, state.markerPointsInPanoramicImage.first().symbol)
        }
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
            activateImage()
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
            activateImage()
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
    fun coveredLocationPlacesPanoramicMarkerInsteadOfRasterMarker() {
        compose.runOnIdle {
            state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            tap()
            assertEquals(1, mapOverlay.graphics.size)
            assertEquals(0, imageOverlay.graphics.size)
            val marker = state.markerPointsInPanoramicImage.single()
            assertEquals(5.0, marker.imagePoint.x, 0.0)
            assertEquals(5.0, marker.imagePoint.y, 0.0)
            assertTrue(marker.isFromGeoView)
        }
    }

    @Test
    fun uncoveredLocationDoesNotPlacePanoramicMarker() {
        compose.runOnIdle {
            state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            coEvery { layer.searchImages(any(), any()) } returns Result.success(emptyList())
            tap()
            assertEquals(1, mapOverlay.graphics.size)
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
        }
    }

    @Test
    fun panoramaRejectsInvalidPixelsButAcceptsWrappedLongitude() {
        compose.runOnIdle {
            state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            for (pixel in listOf(
                DoubleXY(Double.NaN, 5.0), DoubleXY(5.0, Double.POSITIVE_INFINITY),
                DoubleXY(5.0, -1.0), DoubleXY(5.0, 11.0)
            )) {
                coEvery { image.locationToImage(any()) } returns Result.success(pixel)
                tap()
            }
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
            coVerify(exactly = 0) { layer.searchImages(any(), any()) }
            coEvery { image.locationToImage(any()) } returns Result.success(DoubleXY(25.0, 5.0))
            tap()
            assertEquals(1, state.markerPointsInPanoramicImage.size)
        }
    }

    @Test
    fun retainedPanoramaCannotReceiveMarkersForNewImage() {
        compose.runOnIdle {
            state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision - 1, 20, 10)
            tap()
            assertEquals(1, mapOverlay.graphics.size)
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
            coVerify(exactly = 0) { image.locationToImage(any()) }
        }
    }

    @Test
    fun disposedPanoramaDoesNotReceiveMarkersOrUnregisterReplacement() {
        compose.runOnIdle {
            val unregisterOld = state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            val unregisterCurrent = state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            unregisterOld()
            tap()
            assertEquals(1, state.markerPointsInPanoramicImage.size)
            state.clearAllMarkers()
            unregisterCurrent()
            tap()
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
            assertEquals(0, imageOverlay.graphics.size)
        }
    }

    @Test
    fun clearingMarkersDuringProjectionDiscardsPendingPanoramicMarker() {
        val projection = CompletableDeferred<Result<DoubleXY>>()
        compose.runOnIdle {
            state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            coEvery { image.locationToImage(any()) } coAnswers { projection.await() }
            tap()
            state.clearAllMarkers()
            projection.complete(Result.success(DoubleXY(5.0, 5.0)))
        }
        compose.runOnIdle {
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
            assertEquals(0, mapOverlay.graphics.size)
        }
    }

    @Test
    fun panoramaDisposalDuringCoverageSearchDiscardsPendingMarker() {
        val coverage = CompletableDeferred<Result<List<OrientedImage>>>()
        compose.runOnIdle {
            val unregister = state.registerMarkerInPanoramicImage(state.uiState.activeImageRevision, 20, 10)
            coEvery { layer.searchImages(any(), any()) } coAnswers { coverage.await() }
            tap()
            unregister()
            coverage.complete(Result.success(listOf(image)))
        }
        compose.runOnIdle { assertTrue(state.markerPointsInPanoramicImage.isEmpty()) }
    }

    @Test
    fun panoramicImageOriginMarkersRemainDistinctAndClearTogether() {
        compose.runOnIdle {
            val revision = state.uiState.activeImageRevision
            state.registerMarkerInPanoramicImage(revision, 20, 10)
            coEvery { image.imageToLocation(any()) } returns Result.success(location)
            scope.launch { state.addMarkerToPanoramicImage(DoubleXY(3.0, 4.0), revision) }
            tap()
            assertEquals(listOf(false, true), state.markerPointsInPanoramicImage.map { it.isFromGeoView })
            state.clearAllMarkers()
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
            assertEquals(0, mapOverlay.graphics.size)
        }
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
    fun rasterConversionRoundTripsEdgesAndCenterWithNonzeroOriginAndUnequalCells() {
        val layer = rasterLayer()
        for (pixel in listOf(DoubleXY(0.0, 0.0), DoubleXY(3.25, 6.5), DoubleXY(10.0, 10.0))) {
            val point = requireNotNull(computeRasterPoint(pixel, layer))
            val roundTrip = requireNotNull(computeImagePoint(point, layer))
            assertEquals(pixel.x, roundTrip.x, 1e-8)
            assertEquals(pixel.y, roundTrip.y, 1e-8)
        }
        val center = requireNotNull(computeImageCenter(layer))
        val centerPoint = requireNotNull(computeRasterPoint(center, layer))
        assertEquals(110.0, centerPoint.x, 0.0)
        assertEquals(220.0, centerPoint.y, 0.0)
    }

    @Test
    fun inverseRasterConversionRejectsOutOfBoundsPoints() {
        val layer = rasterLayer()
        for (point in listOf(Point(99.0, 220.0), Point(121.0, 220.0), Point(110.0, 199.0), Point(110.0, 241.0))) {
            assertNull(computeImagePoint(point, layer))
        }
    }

    @Test
    fun unreferencedRasterAcceptsMapViewLocalTapReference() {
        // The unit-only reference produced by MapView is not constructible from SR JSON.
        // Exercise its coordinate-system flags here; RasterImageTapTests uses the real view.
        val localReference = mockk<SpatialReference>()
        every { localReference.isGeographic } returns false
        every { localReference.isProjected } returns false
        val point = mockk<Point>()
        every { point.spatialReference } returns localReference
        every { point.x } returns 106.5
        every { point.y } returns 214.0
        val layer = rasterLayer()
        val pixel = requireNotNull(computeImagePoint(point, layer))
        assertEquals(3.25, pixel.x, 1e-8)
        assertEquals(6.5, pixel.y, 1e-8)
        every { point.x } returns 99.0
        assertNull(computeImagePoint(point, layer))
        every { point.x } returns Double.NaN
        assertNull(computeImagePoint(point, layer))
    }

    @Test
    fun unreferencedRasterDoesNotRelabelGeographicOrProjectedTapCoordinates() {
        val layer = rasterLayer()
        for (reference in listOf(SpatialReference.wgs84(), SpatialReference.webMercator())) {
            assertNull(computeImagePoint(Point(110.0, 220.0, reference), layer))
        }
    }

    @Test
    fun georeferencedRasterProjectsTapIntoRasterReference() {
        val reference = SpatialReference.webMercator()
        val layer = rasterLayer(reference)
        val geographicPoint = requireNotNull(GeometryEngine.projectOrNull(
            Point(106.5, 214.0, reference), SpatialReference.wgs84()
        ))
        val pixel = requireNotNull(computeImagePoint(geographicPoint, layer))
        assertEquals(3.25, pixel.x, 1e-6)
        assertEquals(6.5, pixel.y, 1e-6)
        assertNull(computeImagePoint(Point(106.5, 214.0), layer))
    }

    @Test
    fun allRasterConversionsRejectInvalidCellSizes() {
        val layer = rasterLayer()
        val info = requireNotNull(layer.raster?.rasterInfo)
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            for ((x, y) in listOf(invalid to 4.0, 2.0 to invalid)) {
                every { info.cellSizeX } returns x
                every { info.cellSizeY } returns y
                assertNull(computeRasterPoint(DoubleXY(5.0, 5.0), layer))
                assertNull(computeImagePoint(Point(110.0, 220.0), layer))
                assertNull(computeImageCenter(layer))
                assertNull(computePixelCorners(Viewpoint(Envelope(100.0, 200.0, 120.0, 240.0)), layer))
            }
        }
    }

    @Test
    fun replacingRasterOverlayRemovesOnlyOwnedMarkersAndOldDisposalCannotRemoveReplacement() {
        compose.runOnIdle {
            val oldTransform: (DoubleXY) -> Point? = { Point(it.x, -it.y) }
            state.registerMarkerOverlayInImage(imageOverlay, oldTransform)
            val foreign = com.arcgismaps.mapping.view.Graphic(Point(0.0, 0.0))
            imageOverlay.graphics.add(foreign)
            tap()
            assertEquals(2, imageOverlay.graphics.size)
            val replacement = GraphicsOverlay()
            state.registerMarkerOverlayInImage(replacement) { Point(it.x, -it.y) }
            assertEquals(listOf(foreign), imageOverlay.graphics.toList())
            state.unregisterMarkerOverlayInImage(oldTransform)
            tap()
            assertEquals(1, replacement.graphics.size)
        }
    }

    @Test
    fun rasterOriginProjectionCancellationDoesNotPlaceMarkers() {
        compose.runOnIdle {
            coEvery { image.imageToLocation(any()) } returns Result.failure(CancellationException("Cancelled"))
            val job = scope.launch {
                state.addMarkerToRasterImage(DoubleXY(5.0, 5.0), Point(110.0, 220.0), imageOverlay)
            }
            assertTrue(job.isCancelled)
            assertTrue(imageOverlay.graphics.isEmpty())
            assertTrue(mapOverlay.graphics.isEmpty())
        }
    }

    @Test
    fun rasterOriginProjectionCannotCommitToAReplacedOverlay() {
        val projection = CompletableDeferred<Result<Point>>()
        compose.runOnIdle {
            coEvery { image.imageToLocation(any()) } coAnswers { projection.await() }
            scope.launch {
                state.addMarkerToRasterImage(DoubleXY(5.0, 5.0), Point(110.0, 220.0), imageOverlay)
            }
            val replacement = GraphicsOverlay()
            state.registerMarkerOverlayInImage(replacement) { Point(it.x, -it.y) }
            projection.complete(Result.success(location))
            assertTrue(imageOverlay.graphics.isEmpty())
            assertTrue(replacement.graphics.isEmpty())
            assertTrue(mapOverlay.graphics.isEmpty())
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

    @Test
    fun mapOriginMarkersKeepTheTapSymbolWhenSelectionChangesDuringProjection() {
        val projection = CompletableDeferred<Result<DoubleXY>>()
        compose.runOnIdle {
            val selected = MarkerSymbols.symbols[1]
            state.selectMarkerSymbol(selected)
            coEvery { image.locationToImage(any()) } coAnswers { projection.await() }
            tap()
            state.selectMarkerSymbol(MarkerSymbols.symbols[2])
            projection.complete(Result.success(DoubleXY(5.0, 5.0)))
            assertEquals(selected, mapOverlay.graphics.single().symbol)
            assertEquals(selected, imageOverlay.graphics.single().symbol)
        }
    }

    @Test
    fun imageOriginMarkersKeepTheTapSymbolInBothViewsDuringProjection() {
        val projection = CompletableDeferred<Result<Point>>()
        compose.runOnIdle {
            val selected = MarkerSymbols.symbols[1]
            state.selectMarkerSymbol(selected)
            coEvery { image.imageToLocation(any()) } coAnswers { projection.await() }
            scope.launch { state.addMarkerToRasterImage(DoubleXY(5.0, 5.0), Point(110.0, 220.0), imageOverlay) }
            state.selectMarkerSymbol(MarkerSymbols.symbols[2])
            projection.complete(Result.success(location))
            assertEquals(selected, mapOverlay.graphics.single().symbol)
            assertEquals(selected, imageOverlay.graphics.single().symbol)
        }
    }

    @Test
    fun panoramaDisposalDuringImageOriginProjectionDiscardsBothMarkers() {
        val projection = CompletableDeferred<Result<Point>>()
        compose.runOnIdle {
            val revision = state.uiState.activeImageRevision
            val unregister = state.registerMarkerInPanoramicImage(revision, 20, 10)
            coEvery { image.imageToLocation(any()) } coAnswers { projection.await() }
            scope.launch { state.addMarkerToPanoramicImage(DoubleXY(3.0, 4.0), revision) }
            unregister()
            projection.complete(Result.success(location))
            assertTrue(state.markerPointsInPanoramicImage.isEmpty())
            assertTrue(mapOverlay.graphics.isEmpty())
        }
    }

    private fun activateImage() {
        val feature = mockk<Feature>()
        every { feature.geometry } returns location
        coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image)
        scope.launch { state.fetchImage(feature).getOrThrow() }
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

