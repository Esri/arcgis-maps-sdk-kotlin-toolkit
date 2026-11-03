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

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ImagePopupTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: OrientedImageryViewerState
    private lateinit var scope: CoroutineScope
    private lateinit var layer: OrientedImageryLayer
    private lateinit var imageFile: File
    private val location = Point(0.0, 0.0)
    private var viewerDismissed = false

    @Before
    fun createLocalImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        imageFile = File.createTempFile("image-popup", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
        try {
            imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        val image = mockk<OrientedImage>()
        every { image.dataUri } returns imageFile.absolutePath
        every { image.type } returns OrientedImageType.Horizontal
        every { image.geometry } returns location
        every { image.attributes } returns mutableMapOf("Description" to "Test image")
        coEvery { image.load() } returns Result.success(Unit)
        coEvery { image.locationToImage(any()) } returns Result.success(DoubleXY(50.0, 40.0))
        layer = mockk()
        every { layer.name } returns "Popup test"
        every { layer.supportsSequentialNavigation } returns false
        every { layer.visibleFootprints } returns mutableListOf()
        coEvery { layer.searchImages(any(), any()) } returns Result.success(listOf(image))
    }

    @After
    fun deleteLocalImage() {
        imageFile.delete()
    }

    private fun showViewer(
        useTestPopup: Boolean = true,
        createPopup: (suspend () -> Result<Popup>)? = null,
        activateImage: Boolean = true
    ) {
        compose.setContent {
            scope = rememberCoroutineScope()
            state = androidx.compose.runtime.remember {
                OrientedImageryViewerState(
                    layer,
                    GraphicsOverlay(),
                    scope
                )
            }
            LaunchedEffect(state) {
                // These tests do not need SDK footprints or camera overlays.
                state.toggleActiveFootprint()
                state.toggleAdditionalCameraLocations()
                if (activateImage) state.searchImages(location).getOrThrow()
            }
            MaterialTheme {
                ViewerLayout(
                    viewerState = state,
                    modifier = Modifier.fillMaxSize(),
                    title = "Viewer title",
                    onDismiss = { viewerDismissed = true },
                    // A local graphic exercises real Popup UI without a network dependency.
                    onCreateImagePopup = {
                        if (createPopup != null) createPopup()
                        else if (useTestPopup) Result.success(Popup(Graphic(Point(0.0, 0.0))))
                        else state.createImagePopup()
                    }
                )
            }
        }
        if (activateImage) compose.waitUntil(10_000) { state.hasActiveImage }
    }

    private fun openPopup() {
        compose.onNodeWithContentDescription("Image popup").assertIsDisplayed().performClick()
        assertPopupVisible()
    }

    private fun assertPopupVisible() {
        compose.onNodeWithText("Viewer title").assertDoesNotExist()
        compose.onNodeWithContentDescription("Image popup").assertDoesNotExist()
        compose.onNodeWithText("Image details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close image details").assertIsDisplayed()
    }

    private fun assertViewerVisible() {
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
        compose.onNodeWithContentDescription("Image popup").assertExists()
        compose.onNodeWithText("Image details").assertDoesNotExist()
    }

    @Test
    fun noActiveImageHasNoFabAndCannotCreatePopup() {
        showViewer(useTestPopup = false, activateImage = false)
        compose.runOnIdle {
            runBlocking {
                assertTrue(state.createImagePopup().exceptionOrNull() is IllegalStateException)
            }
        }
        compose.onNodeWithContentDescription("Image popup").assertDoesNotExist()
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
    }

    @Test
    fun popupCreationFailureShowsSnackbarAndKeepsViewerVisible() {
        showViewer(createPopup = { Result.failure(IllegalStateException("Test failure")) })
        compose.onNodeWithContentDescription("Image popup").performClick()
        compose.onNodeWithText("Unable to create the image popup.").assertIsDisplayed()
        assertViewerVisible()
    }

    @Test
    fun fabCreatesActiveImagePopupAndHeaderCloseRestoresViewer() {
        showViewer(useTestPopup = false)
        openPopup()

        val titleBounds = compose.onNodeWithText("Image details").fetchSemanticsNode().boundsInRoot
        val closeBounds = compose.onNodeWithContentDescription("Close image details")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(titleBounds.right <= closeBounds.left)
        assertTrue(titleBounds.center.y in closeBounds.top..closeBounds.bottom)
        compose.onNodeWithContentDescription("Close image details").performClick()
        assertViewerVisible()
        compose.runOnIdle { assertFalse(viewerDismissed) }
    }

    @Test
    fun clearingActiveImageClosesPopupEvenWhenImageMetadataIsUnchanged() {
        showViewer()
        repeat(2) {
            openPopup()
            compose.runOnIdle {
                val revision = state.uiState.activeImageRevision
                state.resetAll()
                assertEquals(revision + 1, state.uiState.activeImageRevision)
            }
            compose.onNodeWithText("Viewer title").assertIsDisplayed()
            compose.onNodeWithContentDescription("Image popup").assertDoesNotExist()
            compose.runOnIdle {
                state.toggleActiveFootprint()
                state.toggleAdditionalCameraLocations()
                scope.launch { state.searchImages(location).getOrThrow() }
            }
            compose.waitUntil(10_000) { state.hasActiveImage }
            assertViewerVisible()
        }
    }

    @Test
    fun repeatedTapsDoNotStartDuplicatePopupCreationRequests() {
        val pending = CompletableDeferred<Result<Popup>>()
        var requests = 0
        showViewer(createPopup = {
            requests++
            pending.await()
        })
        repeat(2) {
            compose.onNodeWithContentDescription("Image popup").performClick()
        }
        compose.runOnIdle {
            assertEquals(1, requests)
            pending.complete(Result.success(Popup(Graphic(Point(0.0, 0.0)))))
        }
        assertPopupVisible()
    }

    @Test
    fun imageChangeDiscardsPendingPopup() {
        val pending = CompletableDeferred<Result<Popup>>()
        showViewer(createPopup = { pending.await() })
        compose.onNodeWithContentDescription("Image popup").performClick()
        compose.runOnIdle {
            state.resetAll()
            pending.complete(Result.success(Popup(Graphic(Point(0.0, 0.0)))))
        }
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
        compose.onNodeWithContentDescription("Image popup").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close image details").assertDoesNotExist()
    }

    @Test
    fun unrelatedToolbarStateDoesNotClosePopupAndSurvivesDismissal() {
        showViewer()
        openPopup()
        compose.runOnIdle { state.addMarker() }
        assertPopupVisible()
        compose.onNodeWithContentDescription("Close image details").performClick()
        compose.runOnIdle { assertTrue(state.isMarkingLocation) }
        assertViewerVisible()
    }
}
