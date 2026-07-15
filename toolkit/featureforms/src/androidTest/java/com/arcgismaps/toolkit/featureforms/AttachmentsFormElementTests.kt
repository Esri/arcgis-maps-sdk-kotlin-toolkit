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
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
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
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileWriter

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
        val addAttachmentButton =
            generalAttachmentsNode.onChildWithContentDescription("Add Attachment")
        addAttachmentButton.assertIsDisplayed()
        addAttachmentButton.performClick()

        // Assert all the options are displayed
        val menu = composeTestRule.onNode(isPopup())
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
        mockFilePickerIntentResult(context, "test_document.txt")
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
        mockFilePickerIntentResult(context, "test_document_2.txt")
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
        // Assert the Rename option is disable
        composeTestRule.onNodeWithText(context.getString(R.string.rename)).assert(isEnabled().not())

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
        val addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        var addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        var addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        var addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
        addAttachmentButton = attachmentsNode.onChildWithContentDescription("Add Attachment")
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
     * Mocks the result of a file picker intent to simulate selecting a file from the device's
     * storage.
     *
     * @param context The context used to create the mock file and intent.
     * @param fileName The name of the file to be mocked as selected.
     */
    private fun mockFilePickerIntentResult(
        context: Context,
        fileName: String
    ) {
        val file = File(
            // parent nested dir must match the path in the file provider xml
            File(context.cacheDir, "feature_forms_attachments"),
            // the test file
            fileName //"test_document.txt"
        )
        // Create the mock document file with some content
        file.parentFile?.mkdirs()
        // Create the mock document file with some content if it doesn't exist
        if (file.exists().not()) {
            FileWriter(file).use { writer ->
                writer.write("Mock file content for testing.")
                writer.flush()
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
