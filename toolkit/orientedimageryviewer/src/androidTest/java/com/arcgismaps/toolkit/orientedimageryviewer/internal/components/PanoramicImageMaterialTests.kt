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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.EngineDestroyQueue
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.mockk.mockk
import io.mockk.every
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PanoramicImageMaterialTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var resources: PanoramicImageMaterials
    private lateinit var engine: Engine

    private fun showOwner() {
        compose.setContent {
            engine = rememberEngine()
            val loader = rememberMaterialLoader(engine)
            resources = remember(loader) { PanoramicImageMaterials(loader) }
            DisposableEffect(resources) { onDispose { resources.close() } }
        }
    }

    @Test
    fun replacementAndDisposalReleaseEachTextureExactlyOnce() {
        showOwner()
        compose.runOnIdle {
            val queue = EngineDestroyQueue.of(engine)
            val baseline = queue.size
            val first = resources.create(Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888))
            val second = resources.create(Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888))
            resources.retainOnly(first, second)
            assertEquals(baseline, queue.size)
            resources.retainOnly(second)
            assertEquals(baseline + 1, queue.size)
            first.close()
            resources.retainOnly(second)
            assertEquals(baseline + 1, queue.size)
            resources.close()
            resources.close()
            assertEquals(baseline + 2, queue.size)
        }
    }

    @Test
    fun failedMaterialCreationReleasesItsTextureWithoutReleasingTheDisplayedImage() {
        showOwner()
        compose.runOnIdle {
            val queue = EngineDestroyQueue.of(engine)
            val displayed = resources.create(Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888))
            val failingLoader = mockk<MaterialLoader>()
            every { failingLoader.engine } returns engine
            every { failingLoader.createMaterial(any<String>()) } throws IllegalStateException("Material unavailable")
            val replacement = PanoramicImageMaterials(failingLoader)
            val baseline = queue.size
            assertTrue(runCatching {
                replacement.create(Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888))
            }.isFailure)
            assertEquals(baseline + 1, queue.size)
            resources.retainOnly(displayed)
            assertEquals(baseline + 1, queue.size)
            resources.close()
            assertEquals(baseline + 2, queue.size)
        }
    }

    @Test
    fun recycledBitmapsAreRejectedBeforeNativeUpload() {
        showOwner()
        compose.runOnIdle {
            val queue = EngineDestroyQueue.of(engine)
            val baseline = queue.size
            val bitmap = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888).apply { recycle() }
            assertTrue(runCatching { resources.create(bitmap) }.exceptionOrNull() is IllegalArgumentException)
            assertEquals(baseline, queue.size)
        }
    }

    @Test
    fun resourceDetachesItsRenderableBeforeReleasingItsMaterial() {
        val events = mutableListOf<String>()
        val image = PanoramicImageMaterial(mockk<MaterialInstance>(), 32, 16) { events += "release" }
        image.attach { events += "detach" }
        image.close()
        image.close()
        assertEquals(listOf("detach", "release"), events)
    }
}
