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

package com.arcgismaps.toolkit.featureforms

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentSizeLimitExceededException
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentSource
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.EmptyAttachmentException
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.addAttachmentFromFile
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.addAttachmentFromUri
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.deleteIfExists
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.maxAttachmentUploadSize
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile

/**
 * Test class for the authored AttachmentsFormElement in the FeatureForm.
 *
 * @since 300.1.0
 */
class AttachmentsFormElementTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=7064081d2d1a4af6b871d35954417e5e",
    objectId = 1
) {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val intentsTestRule = IntentsRule()

    /**
     * The test [Context].
     */
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Test case 14.1:
     * Given a `FeatureForm` with authored `AttachmentsFormElement`s and constraints
     * When the `FeatureForm` is displayed and attachments are added
     * Then the appropriate attachment options and validation errors are displayed
     * And the minimum and maximum attachment constraints are honored
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-141-general-attachments-form-elements
     *
     * @since 300.1.0
     */
    @Test
    fun testAttachmentsFormElementWithGeneralInputs() = runTest {
        // Create a FeatureFormState and set the compose content
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }
        val attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "General - All Inputs"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()
        // Assert that the attachments element is displayed
        val generalAttachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement!!.label)
        generalAttachmentsNode.assertIsDisplayed()

        // Get the add attachment button and assert that it is displayed
        var addAttachmentButton =
            generalAttachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed()
        addAttachmentButton.performClick()

        // Assert all the options are displayed
        var menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.take_video),
                context.getString(R.string.choose_from_gallery)
            ),
        )
        val chooseFromFiles = menu.onChildWithText(
            value = context.getString(R.string.choose_from_files),
            recurse = true
        ).assertIsDisplayed()

        // Create a temporary file to simulate the selected document
        mockFilePickerIntentResult(context, "test_document.txt", "text/plain")
        // Perform the click on the "Choose From Files" option
        chooseFromFiles.performClick()
        composeTestRule.waitForIdle()
        // Assert that the intent was sent and received correctly by the component and the new
        // attachment is displayed in the attachments list
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            // Check for the content description since `displayFileName` is false
            generalAttachmentsNode.onChildWithContentDescription(
                "Attachment: test_document.txt"
            ).isDisplayed()
        }

        var attachmentNode = generalAttachmentsNode.onChildWithContentDescription(
            "Attachment: test_document.txt"
        )
        // Long click on the attachment to trigger the context menu
        attachmentNode.performTouchInput { longClick() }
        // Assert the Rename option is disable
        composeTestRule.onNodeWithText(context.getString(R.string.rename)).assert(isEnabled().not())

        // Create another temporary file to simulate the selected document
        mockFilePickerIntentResult(context, "test_document_2.txt", "text/plain")
        // Perform the click on the "Choose From Files" option
        addAttachmentButton.performClick()
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.choose_from_files),
        ).performClick()
        composeTestRule.waitForIdle()
        // Assert that the intent was sent and received correctly by the component and the new
        // attachment is displayed in the attachments list
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            // Check for the content description since `displayFileName` is false
            generalAttachmentsNode.onChildWithContentDescription(
                "Attachment: test_document_2.txt"
            ).isDisplayed()
        }

        attachmentNode = generalAttachmentsNode.onChildWithContentDescription(
            "Attachment: test_document_2.txt"
        )
        // Long click on the attachment to trigger the context menu
        attachmentNode.performTouchInput { longClick() }
        // Assert the Rename option is disabled
        composeTestRule.onNodeWithText(context.getString(R.string.rename)).assert(isEnabled().not())
        // Dismiss the menu
        Espresso.pressBack()

        // Assert that the add attachments button is now disabled since the max number of
        // attachments configured is 2
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            addAttachmentButton.isEnabled().not()
        }

        // Find the "General - Photo Only" attachments form element
        val photoAttachmentsElement = featureForm.elements.firstOrNull {
            it.label == "General - Photo Only"
        } as? AttachmentsFormElement
        assertThat(photoAttachmentsElement).isNotNull()
        val photoAttachmentsNode = composeTestRule.onNodeWithText(photoAttachmentsElement!!.label)
        photoAttachmentsNode.assertIsDisplayed()

        // Click the save button to trigger validation
        val saveButton = composeTestRule.onNodeWithText(context.getString(R.string.save))
        saveButton.assertIsDisplayed()
        saveButton.performClick()

        // Assert that the validation error dialog is displayed
        val validationErrorDialog = composeTestRule.onNode(isDialog())
        validationErrorDialog.assertIsDisplayed()
        val okButton = validationErrorDialog.onChildWithText(
            value = context.getString(R.string.ok),
            recurse = true
        )
        // dismiss the dialog
        okButton.assertIsDisplayed().performClick()

        // Assert that the validation error is now visible on the element
        photoAttachmentsNode.assertTextContains(
            context.resources.getQuantityString(
                R.plurals.min_attachments_required, 1, 1
            )
        )

        // Create a temporary file to simulate the selected image
        mockFilePickerIntentResult(context, "test_img.png", "image/png")

        // Click the add attachment button to open the menu
        addAttachmentButton = photoAttachmentsNode.onChildWithContentDescription(
            context.getString(R.string.add_attachment)
        ).assertIsDisplayed()
        addAttachmentButton.performClick()

        // Perform the click on the "Choose From Files" option
        menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        menu.onChildWithText(
            value = context.getString(R.string.choose_from_files),
            recurse = true
        ).performClick()
        composeTestRule.waitForIdle()

        // Wait until the progress dialog is dismissed
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onNode(isDialog()).isNotDisplayed()
        }

        // Assert that the new attachment is displayed in the attachments list
        composeTestRule.waitUntil {
            // Check for the thumbnail in content description since `displayFileName` is true
            photoAttachmentsNode.onChildWithContentDescription(
                "Thumbnail",
                recurse = true
            ).isDisplayed()
        }

        // Assert that the attachment has a name that matches the pattern "attachment_#.png"
        attachmentNode = photoAttachmentsNode.onChildWithContentDescription(
            "Thumbnail",
            recurse = true
        )
        val pattern = Regex("attachment_\\d+\\.png", RegexOption.IGNORE_CASE)
        attachmentNode.assert(hasTextMatching(pattern))

        // Assert the validation error is no longer visible on the element
        photoAttachmentsNode.assert(
            hasText(
                context.resources.getQuantityString(
                    R.plurals.min_attachments_required, 1, 1
                )
            ).not()
        )

        // Assert the attachment can be renamed
        attachmentNode.performTouchInput { longClick() }
        composeTestRule.onNodeWithText(context.getString(R.string.rename)).assert(isEnabled())
    }

    /**
     * Test case 14.2:
     * Given a `FeatureForm` with an authored `AttachmentsFormElement` of type `DocumentFormInput`
     * When the `FeatureForm` is displayed and the add attachment button is clicked
     * Then only the "Choose From Files" option is displayed in the menu
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-142-document-attachments-form-elements
     *
     * @since 300.1.0
     */
    @Test
    fun testAttachmentsFormElementWithDocumentInput() = runTest {
        // Create a FeatureFormState and set the compose content
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }
        val attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Document"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        // Assert that the attachments element is displayed
        val attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement!!.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        val addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert only the "Choose From Files" option is displayed
        val menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(context.getString(R.string.choose_from_files)),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.take_video),
                context.getString(R.string.choose_from_gallery)
            )
        )
    }

    /**
     * Test case 14.3:
     * Given a `FeatureForm` with `AttachmentsFormElement`s using `ImageFormInput`s with different `AttachmentInputMethod`s
     * When the `FeatureForm` is displayed and their add attachment menu is opened
     * Then each menu displays the appropriate options
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-143-photo-media-attachments-form-elements
     *
     * @since 300.1.0
     */
    @Test
    fun testAttachmentsFormElementWithImageInput() = runTest {
        // Create a FeatureFormState and set the compose content
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        // Find the lazy column node the FeatureForm is displayed in
        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")

        // Find the "Media - Photo - Any" attachments form element
        var attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Photo - Any"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        // Assert that the attachments element is displayed
        var attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement!!.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        var addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        var menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.choose_from_files)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_video)
            )
        )
        // Dismiss the menu
        Espresso.pressBack()

        // Find the "Media - Photo - Capture" attachments form element
        attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Photo - Capture"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        // Assert that the attachments element is displayed
        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.take_photo)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_video),
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.choose_from_files)
            )
        )
        // Dismiss the menu
        Espresso.pressBack()

        // Find the "Media - Photo - Upload" attachments form element
        attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Photo - Upload"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        // Assert that the attachments element is displayed
        attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.choose_from_files)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.take_video)
            )
        )
    }

    /**
     * Test case 14.4:
     * Given a `FeatureForm` with an authored `AttachmentsFormElement` of type `VideoFormInput`s with different `AttachmentInputMethod`s
     * When the `FeatureForm` is displayed and the add attachment menu is opened
     * Then the menu displays the appropriate options
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-144-video-media-attachments-form-elements
     *
     * @since 300.1.0
     */
    @Test
    fun testAttachmentsFormElementWithVideoInput() = runTest {
        // Create a FeatureFormState and set the compose content
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        // Find the lazy column node the FeatureForm is displayed in
        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")

        // Find the "Media - Video - Any" attachments form element
        var attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Video - Any"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        // Assert that the attachments element is displayed
        var attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        var addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        var menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.take_video),
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.choose_from_files)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_photo)
            )
        )
        // Dismiss the menu
        Espresso.pressBack()

        // Find the "Media - Video - Capture" attachments form element
        attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Video - Capture"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        // Assert that the attachments element is displayed
        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.take_video)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.choose_from_files)
            )
        )
        // Dismiss the menu
        Espresso.pressBack()

        // Find the "Media - Video - Upload" attachments form element
        attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Video - Upload"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        // Assert that the attachments element is displayed
        attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.choose_from_files)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.take_video)
            )
        )
    }

    /**
     * Test case 14.5:
     * Given a `FeatureForm` with an authored `AttachmentsFormElement` of type `AudioFormInput`s with different `AttachmentInputMethod`s
     * When the `FeatureForm` is displayed and the add attachment menu is opened
     * Then the menu displays the appropriate options
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-145-audio-media-attachments-form-elements
     *
     * @since 300.1.0
     */
    @Test
    fun testAttachmentsFormElementWithAudioInput() = runTest {
        // Create a FeatureFormState and set the compose content
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        // Find the lazy column node the FeatureForm is displayed in
        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")

        // Find the "Media - Audio - Any" attachments form element
        var attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Audio - Any"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        // Assert that the attachments element is displayed
        var attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        var addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        var menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        // Recording audio is not currently supported in the toolkit
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.choose_from_files)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_video),
                context.getString(R.string.choose_from_gallery),
                context.getString(R.string.take_photo)
            )
        )
        // Dismiss the menu
        Espresso.pressBack()

        // Find the "Media - Audio - Capture" attachments form element
        attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Audio - Capture"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        // Assert that the attachments element is displayed
        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed()
        // Recording audio is not currently supported in the toolkit and hence the add option
        // is disabled
        addAttachmentButton.assertIsNotEnabled()

        // Find the "Media - Audio - Upload" attachments form element
        attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Audio - Upload"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        lazyColumnNode.performScrollToNode(hasText(attachmentsFormElement!!.label))
        // Assert that the attachments element is displayed
        attachmentsNode = composeTestRule.onNodeWithText(attachmentsFormElement.label)
        attachmentsNode.assertIsDisplayed()

        // Assert that the add attachment button is displayed and enabled
        addAttachmentButton =
            attachmentsNode.onChildWithContentDescription(context.getString(R.string.add_attachment))
        addAttachmentButton.assertIsDisplayed().performClick()

        // Assert all the appropriate options are displayed in the menu
        menu = composeTestRule.onNode(isPopup())
        menu.assertIsDisplayed()
        assertValidCaptureOptions(
            node = menu,
            visibleOptions = listOf(
                context.getString(R.string.choose_from_files)
            ),
            nonVisibleOptions = listOf(
                context.getString(R.string.take_photo),
                context.getString(R.string.take_video),
                context.getString(R.string.choose_from_gallery)
            )
        )
    }

    /**
     * Given valid and invalid captured files
     * When they are added as attachments
     * Then each temporary file is deleted after the attempt
     *
     * @since 300.1.0
     */
    @Test
    fun testCapturedFileIsDeleted() = runTest {
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        val attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "General - All Inputs"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        val elementState =
            featureFormState.getActiveFormStateData().stateCollection[attachmentsFormElement!!] as? AttachmentElementState
        assertThat(elementState).isNotNull()

        // Create a temporary file to simulate a captured image
        val validFile = File.createTempFile(
            "source",
            ".txt",
            context.cacheDir
        )
        validFile.writeText("Attachment content")

        try {
            // Add the valid file and assert it was a success
            val result = elementState!!.addAttachmentFromFile(
                file = validFile,
                source = AttachmentSource.Capture
            )
            assertThat(result.isSuccess).isTrue()
            // Assert that the temporary file was deleted after the attempt
            assertThat(validFile.exists()).isFalse()
        } finally {
            // Test fallback cleanup
            validFile.deleteIfExists()
        }

        // Create empty file to simulate a captured image that will fail validation
        val invalidFile = File.createTempFile(
            "invalid_source",
            ".txt",
            context.cacheDir
        )
        try {
            // Add the invalid file and assert it was a failure
            val result = elementState.addAttachmentFromFile(
                file = invalidFile,
                source = AttachmentSource.Capture
            )
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(
                EmptyAttachmentException::class.java
            )
            // Assert that the temporary file was deleted after the attempt
            assertThat(invalidFile.exists()).isFalse()
        } finally {
            // Test fallback cleanup
            invalidFile.deleteIfExists()
        }

        // Create a temporary file to simulate a captured image that exceeds the maximum attachment
        // upload size
        val overSizedFile = File.createTempFile(
            "oversized_source",
            ".txt",
            context.cacheDir
        )
        try {
            // Set the file length to exceed the maximum attachment upload size
            RandomAccessFile(overSizedFile, "rw").use {
                // Set the file length to exceed the maximum attachment upload size
                it.setLength(maxAttachmentUploadSize + 1L)
            }
            // Add the oversized file and assert it was a failure due to exceeding the size limit
            val result = elementState.addAttachmentFromFile(
                file = overSizedFile,
                source = AttachmentSource.Capture
            )
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(
                AttachmentSizeLimitExceededException::class.java
            )
            // Assert that the temporary file was deleted after the attempt
            assertThat(overSizedFile.exists()).isFalse()
        } finally {
            // Test fallback cleanup
            overSizedFile.deleteIfExists()
        }
    }

    /**
     * Given a URI for an unsupported attachment type
     * When it is added to an attachments element
     * Then the validation failure is propagated
     *
     * @since 300.1.0
     */
    @Test
    fun testAddAttachmentFromUriPropagatesFailures() = runTest {
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        val attachmentsFormElement = featureForm.elements.firstOrNull {
            it.label == "Media - Audio - Any"
        } as? AttachmentsFormElement
        assertThat(attachmentsFormElement).isNotNull()

        val elementState =
            featureFormState.getActiveFormStateData().stateCollection[attachmentsFormElement!!] as? AttachmentElementState
        assertThat(elementState).isNotNull()

        // Create a temporary file to simulate a selected document
        val source = AttachmentsFileProvider.createTempFileWithUri(
            prefix = "source",
            suffix = ".txt",
            context = context
        )
        source.file.writeText("Attachment content")

        try {
            val result = elementState!!.addAttachmentFromUri(
                uri = source.uri,
                context = context
            )
            // Since the file is not a valid attachment type for the "Media - Audio - Any" input,
            // it should fail
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(
                FeatureFormValidationException.IncorrectAttachmentTypeException::class.java
            )
        } finally {
            // Clean up the temporary file
            source.file.deleteIfExists()
        }
        assertThat(source.file.exists()).isFalse()
    }

    /**
     * Mocks the result of a file picker intent to simulate selecting a file from the device's
     * storage. This method supports mocking files with the following content types: "image/png" and
     * "text/plain". If an unsupported content type is provided, an [UnsupportedOperationException]
     * will be thrown.
     *
     * @param context The context used to create the mock file and intent.
     * @param fileName The name of the file to be mocked as selected.
     * @param contentType The MIME type of the file to be mocked as selected.
     *
     * @throws UnsupportedOperationException if the provided content type is not supported.
     */
    private fun mockFilePickerIntentResult(
        context: Context,
        fileName: String,
        contentType: String
    ) {
        val file = File(
            // parent nested dir must match the path in the file provider XML
            File(context.cacheDir, "feature_forms_attachments"),
            // the test file
            fileName //"test_document.txt"
        )
        // Create the mock document file with some content
        file.parentFile?.mkdirs()
        // If the file already exists, we don't need to create it again
        if (file.exists()) return
        when (contentType) {
            "image/png" -> {
                val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
                file.outputStream().use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream)
                }
                bitmap.recycle()
            }

            "text/plain" -> {
                FileWriter(file).use { writer ->
                    writer.write("Mock file content for testing.")
                    writer.flush()
                }
            }

            else -> {
                // If the content type is not supported, throw an exception
                throw UnsupportedOperationException("Unsupported content type: $contentType")
            }
        }
        // Set up the intent response with the file URI
        val resultData = Intent().apply {
            data = AttachmentsFileProvider.getUriForFile(file, context)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        // Set up the intent matcher to respond with the result when the "Choose From Files" action
        // is triggered
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(result)
    }

    /**
     * Asserts that the specified options are displayed or not displayed in the given node.
     *
     * @param node The [SemanticsNodeInteraction] to check for the options.
     * @param visibleOptions A list of options that should be displayed.
     * @param nonVisibleOptions A list of options that should not be displayed.
     */
    private fun assertValidCaptureOptions(
        node: SemanticsNodeInteraction,
        visibleOptions: List<String> = emptyList(),
        nonVisibleOptions: List<String> = emptyList(),
    ) {
        visibleOptions.forEach { option ->
            node.onChildWithText(
                value = option,
                recurse = true
            ).assertIsDisplayed()
        }
        nonVisibleOptions.forEach { option ->
            assertThrows(AssertionError::class.java) {
                node.onChildWithText(
                    value = option,
                    recurse = true
                ).assertIsDisplayed()
            }
        }
    }
}
