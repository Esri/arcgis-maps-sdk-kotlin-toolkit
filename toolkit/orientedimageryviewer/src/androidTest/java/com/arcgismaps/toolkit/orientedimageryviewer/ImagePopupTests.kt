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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImagePopupTests {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: OrientedImageryViewerState
    private var viewerDismissed = false

    private fun showViewer(
        useTestPopup: Boolean = true,
        createPopup: (suspend () -> Result<Popup>)? = null
    ) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            state = androidx.compose.runtime.remember {
                OrientedImageryViewerState(
                    OrientedImageryLayer("https://example.com/FeatureServer/0"),
                    GraphicsOverlay(),
                    scope
                )
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
    }

    private fun openPopup() {
        compose.onNodeWithContentDescription("Image popup").performScrollTo().performClick()
        compose.onNodeWithContentDescription("close popup").assertIsDisplayed()
    }

    @Test
    fun noActiveImageShowsSnackbarAndKeepsViewerVisible() {
        showViewer(useTestPopup = false)
        compose.runOnIdle {
            runBlocking {
                assertTrue(state.createImagePopup().exceptionOrNull() is IllegalStateException)
            }
        }
        compose.onNodeWithContentDescription("Image popup").performScrollTo().performClick()
        compose.onNodeWithText("Select an image before opening its popup.").assertIsDisplayed()
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
        compose.onNodeWithContentDescription("close popup").assertDoesNotExist()
    }

    @Test
    fun popupUsesOwnTopBarAndDismissalRestoresViewer() {
        showViewer()
        openPopup()
        compose.onNodeWithText("Viewer title").assertDoesNotExist()
        compose.onNodeWithContentDescription("Image popup").assertDoesNotExist()

        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
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
            compose.onNodeWithContentDescription("close popup").assertDoesNotExist()
            compose.onNodeWithText("Viewer title").assertIsDisplayed()
        }
    }

    @Test
    fun repeatedTapsDoNotStartDuplicatePopupQueries() {
        val pending = CompletableDeferred<Result<Popup>>()
        var requests = 0
        showViewer(createPopup = {
            requests++
            pending.await()
        })
        repeat(2) {
            compose.onNodeWithContentDescription("Image popup").performScrollTo().performClick()
        }
        compose.runOnIdle {
            assertEquals(1, requests)
            pending.complete(Result.success(Popup(Graphic(Point(0.0, 0.0)))))
        }
        compose.onNodeWithContentDescription("close popup").assertIsDisplayed()
    }

    @Test
    fun imageChangeDiscardsPendingPopup() {
        val pending = CompletableDeferred<Result<Popup>>()
        showViewer(createPopup = { pending.await() })
        compose.onNodeWithContentDescription("Image popup").performScrollTo().performClick()
        compose.runOnIdle {
            state.resetAll()
            pending.complete(Result.success(Popup(Graphic(Point(0.0, 0.0)))))
        }
        compose.onNodeWithContentDescription("close popup").assertDoesNotExist()
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
    }

    @Test
    fun unrelatedToolbarStateDoesNotClosePopupAndSurvivesDismissal() {
        showViewer()
        openPopup()
        compose.runOnIdle { state.setSequentialNavigationEnabled(true) }
        compose.onNodeWithContentDescription("close popup").assertIsDisplayed()
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.runOnIdle { assertTrue(state.uiState.isSequentialNavigationEnabled) }
        compose.onNodeWithText("Viewer title").assertIsDisplayed()
    }
}
