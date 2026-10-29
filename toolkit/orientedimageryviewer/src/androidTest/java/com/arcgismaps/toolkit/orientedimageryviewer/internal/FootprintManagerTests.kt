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

package com.arcgismaps.toolkit.orientedimageryviewer.internal

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageFootprint
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FootprintManagerTests {
    @get:Rule
    val compose = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val visibleFootprints = mutableListOf<OrientedImageFootprint>()
    private lateinit var controller: FootprintManager
    private lateinit var overlay: GraphicsOverlay

    @Before
    fun setUp() {
        compose.setContent { }
        compose.runOnIdle {
            val layer = mockk<OrientedImageryLayer>()
            every { layer.visibleFootprints } returns visibleFootprints
            val image = mockk<OrientedImage>()
            every { image.geometry } returns Point(1.0, 2.0)
            overlay = GraphicsOverlay()
            controller = FootprintManager(
                layer, overlay, scope,
                activeImage = { image },
                additionalImages = { emptyList() },
                inputLocation = { Point(3.0, 4.0) },
                uiState = { OrientedImageryViewerUiState(isActiveFootprintVisible = true) },
                createFootprint = { mockk(relaxed = true) }
            )
            controller.setActiveVisible(true)
        }
    }

    @After
    fun tearDown() {
        compose.runOnIdle {
            controller.reset()
            scope.cancel()
        }
    }

    @Test
    fun updatesConflateToTheLatestPendingViewport() {
        compose.runOnIdle {
            val pending = CompletableDeferred<Result<Unit>>()
            val updates = mutableListOf<Int>()
            controller.update { updates += 1; pending.await() }
            controller.update { updates += 2; Result.success(Unit) }
            controller.update { updates += 3; Result.success(Unit) }
            assertEquals(listOf(1), updates)
            pending.complete(Result.success(Unit))
            assertEquals(listOf(1, 3), updates)
        }
    }

    @Test
    fun resetCancelsPendingUpdatesAndRetainsForeignObjects() {
        compose.runOnIdle {
            val foreignGraphic = Graphic(Point(5.0, 6.0))
            val foreignFootprint = mockk<OrientedImageFootprint>()
            overlay.graphics.add(foreignGraphic)
            visibleFootprints.add(foreignFootprint)
            val pending = CompletableDeferred<Result<Unit>>()
            var staleUpdateRan = false
            controller.update { pending.await() }
            controller.update { staleUpdateRan = true; Result.success(Unit) }
            controller.reset()
            pending.complete(Result.success(Unit))
            assertTrue(!staleUpdateRan)
            assertEquals(listOf(foreignGraphic), overlay.graphics.toList())
            assertEquals(listOf(foreignFootprint), visibleFootprints)
        }
    }

    @Test
    fun failedUpdateDoesNotDiscardTheLatestPendingViewport() {
        compose.runOnIdle {
            val pending = CompletableDeferred<Unit>()
            var latestRan = false
            controller.update { pending.await(); error("SDK failure") }
            controller.update { latestRan = true; Result.success(Unit) }
            pending.complete(Unit)
            assertTrue(latestRan)
        }
    }
}
