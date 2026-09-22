/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.popup

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.toolkit.popup.internal.element.attachment.AttachmentsElementState
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsElementState
import com.arcgismaps.toolkit.popup.internal.element.media.MediaElementState
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for [Popup] toolkit component.
 *
 * @since 300.2.0
 */
class PopupTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given a popup created from a web map feature,
     * When its cards and media are opened, expanded, collapsed, and closed,
     * Then each rendered state passes accessibility checks.
     *
     * @since 300.2.0
     */
    @Test
    fun testPopupAlly() = runTest(timeout = 2.minutes) {
        // Load a popup using the web map.
        val mapViewModel = MapViewModel(itemId = "9f3a674e998f461580006e626611f9ad")
        val popupState = mapViewModel.load().first()
        composeTestRule.setContent { PopupScenario(mapViewModel.map, popupState) }
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            popupState.getActivePopupStateData().initialEvaluation
        }

        fun SemanticsNodeInteraction.clickAndCheck() {
            assertIsDisplayed().performClick()
            composeTestRule.checkAccessibility(false)
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Check the initial popup.
        composeTestRule.waitForIdle()
        composeTestRule.checkAccessibility(false)
        popupState.getActivePopupStateData().stateCollection.forEachIndexed { elementIndex, entry ->
            val state = entry.state
            // Move to next popup element to perform checks.
            composeTestRule.onNodeWithTag("PopupBody").performScrollToIndex(elementIndex)
            if (state is MediaElementState) {
                composeTestRule.waitUntil(timeoutMillis = 10000) { state.isPopupMediaCreated }
            }
            composeTestRule.checkAccessibility(false)
            val title = when (state) {
                is FieldsElementState -> state.title
                is MediaElementState -> state.title
                is AttachmentsElementState -> state.title
                else -> return@forEachIndexed
            }
            val card = hasTestTag("ExpandableCard") and hasAnyDescendant(hasText(title))
            fun cardNode(tag: String) = composeTestRule.onNode(
                hasTestTag(tag) and hasAnyAncestor(card), useUnmergedTree = true
            )

            val header = cardNode("ExpandableHeader")
            val content = cardNode("AnimatedVisibility")
            // Verify each expandable card can be collapsed and restored.
            header.performScrollTo()
            composeTestRule.checkAccessibility(false)
            content.assertExists()
            header.clickAndCheck()
            content.assertDoesNotExist()
            header.clickAndCheck()
            content.assertExists()

            if (state is MediaElementState) {
                state.media.forEachIndexed { mediaIndex, medium ->
                    // Open each image or chart and verify the viewer and its action menu.
                    val gallery = cardNode("MediaGallery")
                    gallery.performScrollTo()
                    composeTestRule.checkAccessibility(false)
                    gallery.performScrollToIndex(mediaIndex)
                    composeTestRule.waitUntil(timeoutMillis = 10000) { medium.imageUri.value.isNotEmpty() }
                    composeTestRule.checkAccessibility(false)
                    composeTestRule.onNode(
                        hasClickAction() and hasText(medium.title) and
                                hasAnyAncestor(hasTestTag("MediaGallery")) and
                                hasAnyAncestor(card)
                    ).clickAndCheck()
                    composeTestRule.onNodeWithTag("FileViewer").assertIsDisplayed()
                    composeTestRule.onNodeWithContentDescription(context.getString(R.string.more))
                        .clickAndCheck()
                    listOf(R.string.share, R.string.save).forEach { label ->
                        composeTestRule.onNode(
                            hasText(context.getString(label)) and hasAnyAncestor(isPopup())
                        ).assertIsDisplayed()
                    }
                    composeTestRule.onNodeWithTag("FileViewer").performTouchInput { click(center) }
                    composeTestRule.checkAccessibility(false)
                    // Close the viewer before continuing to the next media item.
                    composeTestRule.onNodeWithContentDescription(context.getString(R.string.close))
                        .clickAndCheck()
                    composeTestRule.onNodeWithTag("FileViewer").assertDoesNotExist()
                }
            }
        }
    }
}
