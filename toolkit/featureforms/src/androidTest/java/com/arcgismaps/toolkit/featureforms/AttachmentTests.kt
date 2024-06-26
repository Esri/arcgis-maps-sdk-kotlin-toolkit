/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureforms

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AttachmentTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=3e551c383fc949c7982ec73ba67d409b",
    objectId = 1
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testImageAttachments() = runTest {
        composeTestRule.setContent {
            FeatureForm(featureForm = featureForm)
        }
        val attachmentsFormElement = featureForm.defaultAttachmentsElement
        assertThat(attachmentsFormElement).isNotNull()
        val formAttachment = attachmentsFormElement!!.attachments.first()
        // get the attachments form element node
        val attachmentsField = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsField.assertIsDisplayed()
        // get the form attachment node
        val attachmentNode = attachmentsField.onChildWithText(formAttachment.name)
        attachmentNode.assertIsDisplayed()
        // check the attachment size is visible
        attachmentNode.assertTextContains("154 kB")
        // check the download icon is visible
        attachmentNode.assertContentDescriptionContains("Download")
        // download the attachment
        attachmentNode.performClick()
        // wait for the attachment to download
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val contentDescription =
                attachmentNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
            // check the download icon is no longer visible which indicates the attachment
            // has been downloaded
            contentDescription?.none { item ->
                item.equals("Download", true)
            } ?: false
        }
        // check the thumbnail icon is visible
        attachmentNode.assertContentDescriptionContains("Thumbnail")
    }
}
