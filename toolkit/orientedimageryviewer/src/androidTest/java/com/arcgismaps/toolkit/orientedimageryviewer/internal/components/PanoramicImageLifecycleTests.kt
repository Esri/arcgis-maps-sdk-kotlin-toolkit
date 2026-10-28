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

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class PanoramicImageLifecycleTests {
    @get:Rule
    val compose = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val files = mutableListOf<File>()
    private val projections = AtomicInteger()
    private val centers = AtomicInteger()
    private val visible = mutableStateOf(true)
    private lateinit var state: OrientedImageryViewerState
    private lateinit var layer: OrientedImageryLayer

    @Before
    fun setUp() {
        // SceneView continuously awaits frames. Automatic clock advancement can never reach idle.
        compose.mainClock.autoAdvance = false
        layer = mockk()
        every { layer.name } returns "Panorama lifecycle"
        every { layer.supportsSequentialNavigation } returns true
        every { layer.visibleFootprints } returns mutableListOf()
        compose.runOnIdle { state = OrientedImageryViewerState(layer, GraphicsOverlay(), scope) }
    }

    @After
    fun tearDown() {
        compose.runOnIdle {
            visible.value = false
            scope.cancel()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        files.forEach { it.delete() }
    }

    private fun image(path: String): OrientedImage = mockk<OrientedImage>().also { image ->
        every { image.dataUri } returns path
        every { image.type } returns OrientedImageType.Image360
        every { image.geometry } returns Point(0.0, 0.0)
        every { image.attributes } returns mutableMapOf()
        coEvery { image.load() } returns Result.success(Unit)
        coEvery { image.locationToImage(any()) } coAnswers {
            projections.incrementAndGet()
            Result.success(DoubleXY(32.0, 16.0))
        }
        coEvery { image.imageToLocation(any()) } coAnswers {
            centers.incrementAndGet()
            Result.success(Point(0.0, 0.0))
        }
    }

    private fun activate(image: OrientedImage) {
        compose.runOnIdle {
            val feature = mockk<Feature>()
            every { feature.geometry } returns Point(0.0, 0.0)
            coEvery { layer.fetchImageForFeature(feature) } returns Result.success(image)
            scope.launch { state.fetchImage(feature).getOrThrow() }
        }
    }

    private fun showPanorama() {
        compose.setContent {
            if (visible.value) PanoramicImage(state, Modifier.size(240.dp).testTag("panorama"))
        }
    }

    private fun awaitFrameCondition(timeoutMillis: Long = 20_000, condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis) {
            compose.mainClock.advanceTimeByFrame()
            condition()
        }
    }

    private fun localImage(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("panorama-lifecycle", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        files += file
        return file
    }

    private fun tapPixel(xFraction: Float = 0.5f, yFraction: Float = 0.5f): DoubleXY {
        val count = compose.runOnIdle {
            if (!state.isMarkingLocation) state.addMarker()
            state.markerPointsInPanoramicImage.size
        }
        compose.onNodeWithTag("panorama").performTouchInput {
            click(Offset(width * xFraction, height * yFraction))
        }
        awaitFrameCondition { state.markerPointsInPanoramicImage.size > count }
        return compose.runOnIdle { state.markerPointsInPanoramicImage.last().imagePoint }
    }

    @Test
    fun tapPanAndZoomWorkAfterNextAndPreviousNavigation() {
        val first = image(localImage().absolutePath)
        val second = image(localImage().absolutePath)
        compose.runOnIdle {
            coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(first, second))
            scope.launch { state.searchImages(Point(0.0, 0.0)).getOrThrow() }
        }
        showPanorama()

        repeat(3) { index ->
            if (index > 0) {
                val count = projections.get()
                compose.runOnIdle {
                    scope.launch {
                        if (index == 1) state.navigateNextImage().getOrThrow()
                        else state.navigatePreviousImage().getOrThrow()
                    }
                }
                awaitFrameCondition { projections.get() > count }
            } else {
                awaitFrameCondition { projections.get() > 0 }
            }
            compose.mainClock.advanceTimeBy(1_000)
            compose.waitForIdle()

            val beforePan = tapPixel()
            compose.onNodeWithTag("panorama").performTouchInput {
                swipe(Offset(width * 0.4f, height * 0.5f), Offset(width * 0.6f, height * 0.5f), 400)
            }
            compose.mainClock.advanceTimeBy(1_000)
            val afterPan = tapPixel()
            assertTrue("Pan must rotate image $index", abs(afterPan.x - beforePan.x) > 0.1)

            val beforeZoom = tapPixel(yFraction = 0.3f)
            compose.onNodeWithTag("panorama").performTouchInput {
                pinch(
                    start0 = Offset(width * 0.4f, height * 0.5f),
                    start1 = Offset(width * 0.6f, height * 0.5f),
                    end0 = Offset(width * 0.2f, height * 0.5f),
                    end1 = Offset(width * 0.8f, height * 0.5f),
                    durationMillis = 400
                )
            }
            compose.mainClock.advanceTimeBy(1_000)
            val afterZoom = tapPixel(yFraction = 0.3f)
            assertTrue(
                "Pinch must narrow the view of image $index",
                abs(afterZoom.y - afterPan.y) < abs(beforeZoom.y - afterPan.y) - 0.1
            )
        }
    }

    @Test
    fun changingAimAndNavigationModeDoesNotReloadTheImageFile() {
        val file = localImage()
        activate(image(file.absolutePath))
        showPanorama()
        awaitFrameCondition { projections.get() > 0 }
        compose.waitForIdle()
        assertTrue(file.delete())
        val count = projections.get()
        compose.runOnIdle { state.setInputLocation(Point(1.0, 2.0)) }
        awaitFrameCondition { projections.get() > count }
        compose.runOnIdle { state.setSequentialNavigationEnabled(true) }
        awaitFrameCondition { centers.get() > 0 }
        compose.onNodeWithText("Failed to load panoramic image", substring = true).assertDoesNotExist()
    }

    @Test
    fun failedReplacementCanBeFollowedByRepeatedSuccessfulReplacements() {
        activate(image(localImage().absolutePath))
        showPanorama()
        awaitFrameCondition { projections.get() > 0 }
        compose.waitForIdle()
        val missing = localImage().apply { delete() }
        activate(image(missing.absolutePath))
        awaitFrameCondition {
            compose.onAllNodes(androidx.compose.ui.test.hasText("Failed to load panoramic image", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Failed to load panoramic image", substring = true).assertIsDisplayed()
        repeat(3) {
            val count = projections.get()
            activate(image(localImage().absolutePath))
            awaitFrameCondition { projections.get() > count }
            compose.waitForIdle()
            compose.onNodeWithText("Failed to load panoramic image", substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun disposalDuringInitialProjectionCancelsThePendingEffect() {
        val pending = CompletableDeferred<Result<DoubleXY>>()
        val started = CompletableDeferred<Unit>()
        val projectionExited = AtomicBoolean(false)
        val image = image(localImage().absolutePath)
        coEvery { image.locationToImage(any()) } coAnswers {
            started.complete(Unit)
            try {
                pending.await()
            } finally {
                projectionExited.set(true)
            }
        }
        activate(image)
        showPanorama()
        awaitFrameCondition { started.isCompleted }
        compose.runOnIdle { visible.value = false }
        awaitFrameCondition { projectionExited.get() }
        compose.runOnIdle { pending.complete(Result.success(DoubleXY(32.0, 16.0))) }
        compose.waitForIdle()
    }
}
