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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class RasterImageTapTests {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unreferencedRasterTapAddsImageAndGeoViewMarkers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("raster-tap", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val initialized = AtomicBoolean(false)
        val location = Point(8.0, 8.0, SpatialReference.wgs84())
        val image = mockk<OrientedImage>()
        every { image.dataUri } returns file.absolutePath
        every { image.type } returns OrientedImageType.Horizontal
        every { image.geometry } returns location
        every { image.attributes } returns mutableMapOf()
        coEvery { image.load() } returns Result.success(Unit)
        coEvery { image.locationToImage(any()) } coAnswers {
            initialized.set(true)
            Result.success(DoubleXY(50.0, 40.0))
        }
        var tappedPixel: DoubleXY? = null
        coEvery { image.imageToLocation(any()) } coAnswers {
            tappedPixel = firstArg()
            Result.success(location)
        }
        val layer = mockk<OrientedImageryLayer>()
        every { layer.name } returns "Raster tap"
        every { layer.supportsSequentialNavigation } returns false
        every { layer.visibleFootprints } returns mutableListOf()
        coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(image))
        val mapOverlay = GraphicsOverlay()
        val imageOverlay = GraphicsOverlay()
        val state = OrientedImageryViewerState(layer, mapOverlay, scope)
        try {
            compose.runOnIdle {
                scope.launch {
                    state.searchImages(location).getOrThrow()
                    state.addMarker()
                }
            }
            compose.setContent {
                RasterImage(state, Modifier.size(300.dp).testTag("raster"), imageOverlay)
            }
            compose.waitUntil(20_000) { initialized.get() }
            compose.waitForIdle()
            compose.onNodeWithTag("raster").performTouchInput { click(center) }
            compose.waitUntil(5_000) { tappedPixel != null }
            compose.runOnIdle {
                val pixel = requireNotNull(tappedPixel)
                assertEquals(50.0, pixel.x, 2.0)
                assertEquals(40.0, pixel.y, 2.0)
                assertEquals(1, mapOverlay.graphics.size)
                assertEquals(location, mapOverlay.graphics.single().geometry)
                assertEquals(1, imageOverlay.graphics.size)
                val rasterPoint = imageOverlay.graphics.single().geometry as Point
                assertTrue(rasterPoint.x.isFinite() && rasterPoint.y.isFinite())
            }
        } finally {
            compose.runOnIdle { scope.cancel() }
            file.delete()
        }
    }
}
