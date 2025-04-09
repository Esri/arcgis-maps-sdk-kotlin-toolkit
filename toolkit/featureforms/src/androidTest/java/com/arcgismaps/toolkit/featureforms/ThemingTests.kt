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

import android.content.Context
import android.text.format.Formatter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ThemingTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=9c7ee7cd979c434896684bf507cca75d",
    objectId = 1
) {
    private lateinit var context: Context

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    /**
     * Given a FeatureForm with a custom color scheme and typography for editable fields
     * When the FeatureForm is displayed
     * Then the custom color scheme and typography are applied to the editable form elements
     */
    @Test
    fun testEditableFieldTheming() {
        var colorScheme: FeatureFormColorScheme
        var typography: FeatureFormTypography
        // create custom color scheme and typography and apply to the FeatureForm
        composeTestRule.setContent {
            colorScheme = FeatureFormDefaults.colorScheme(
                editableTextFieldColors = FeatureFormDefaults.editableTextFieldColors(
                    focusedTextColor = Color.Red
                )
            )
            typography = FeatureFormDefaults.typography(
                editableTextFieldTypography = FeatureFormDefaults.editableTextFieldTypography(
                    labelStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Green
                    )
                )
            )
            FeatureForm(
                featureForm = featureForm,
                colorScheme = colorScheme,
                typography = typography
            )
        }
        val formElement = featureForm.getFieldFormElementWithLabel("Text Box")
        assertThat(formElement).isNotNull()
        val label = composeTestRule.onNodeWithText(formElement!!.label, useUnmergedTree = true)
        label.assertIsDisplayed()
        label.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Green
            )
        )
        val text =
            composeTestRule.onNodeWithText(formElement.formattedValue, useUnmergedTree = true)
        text.assertIsDisplayed()
        text.performClick()
        text.assertTextColor(Color.Red)
    }

    /**
     * Given a FeatureForm with a custom color scheme that includes placeholder colors
     * When the FeatureForm is displayed and a form element is focused
     * Then the custom placeholder colors are applied to the form elements based on focus state
     */
    @Test
    fun testPlaceHolderTransformation() {
        var colorScheme: FeatureFormColorScheme
        // create custom color scheme and typography and apply to the FeatureForm
        composeTestRule.setContent {
            colorScheme = FeatureFormDefaults.colorScheme(
                editableTextFieldColors = FeatureFormDefaults.editableTextFieldColors(
                    unfocusedPlaceholderColor = Color.White,
                    focusedPlaceholderColor = Color.Red,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Blue
                )
            )
            FeatureForm(
                featureForm = featureForm,
                colorScheme = colorScheme
            )
        }
        val formElement = featureForm.getFieldFormElementWithLabel("An empty field")
        assertThat(formElement).isNotNull()
        val field = composeTestRule.onNodeWithText(formElement!!.label)
        val placeholder = composeTestRule.onNodeWithText(formElement.hint, useUnmergedTree = true)
        placeholder.assertIsDisplayed()
        // test unfocused placeholder color
        placeholder.assertTextColor(Color.White)
        field.performClick()
        // test focused placeholder color
        placeholder.assertTextColor(Color.Red)
        field.performTextInput("test")
        val text = composeTestRule.onNodeWithText("test", useUnmergedTree = true)
        text.assertIsDisplayed()
        // bring focus to the field
        field.performClick()
        // test focused text color
        text.assertTextColor(Color.Blue)
        // clear focus from the field
        field.performImeAction()
        // test unfocused text color
        text.assertTextColor(Color.Black)
    }

    /**
     * Given a FeatureForm with a custom color scheme and typography for read only fields
     * When the FeatureForm is displayed
     * Then the custom color scheme and typography are applied to the read only form elements
     */
    @Test
    fun testReadOnlyFieldTheming() {
        var colorScheme: FeatureFormColorScheme
        var typography: FeatureFormTypography
        composeTestRule.setContent {
            colorScheme = FeatureFormDefaults.colorScheme(
                readOnlyFieldColors = FeatureFormDefaults.readOnlyFieldColors(
                    textColor = Color.Green
                )
            )
            typography = FeatureFormDefaults.typography(
                readOnlyFieldTypography = FeatureFormDefaults.readOnlyFieldTypography(
                    labelStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Red
                    )
                )
            )
            FeatureForm(
                featureForm = featureForm,
                colorScheme = colorScheme,
                typography = typography
            )
        }
        val formElement = featureForm.getFieldFormElementWithLabel("Name")
        assertThat(formElement).isNotNull()
        val label = composeTestRule.onNodeWithText(formElement!!.label, useUnmergedTree = true)
        label.assertIsDisplayed()
        label.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Red
            )
        )
        val text =
            composeTestRule.onNodeWithText(formElement.formattedValue, useUnmergedTree = true)
        text.assertIsDisplayed()
        text.assertTextColor(Color.Green)
    }

    /**
     * Given a FeatureForm with a custom color scheme and typography for radio button fields
     * When the FeatureForm is displayed
     * Then the custom color scheme and typography are applied to the radio button form elements
     */
    @Test
    fun testRadioButtonFieldTheming() {
        var colorScheme: FeatureFormColorScheme
        var typography: FeatureFormTypography
        composeTestRule.setContent {
            colorScheme = FeatureFormDefaults.colorScheme(
                radioButtonFieldColors = FeatureFormDefaults.radioButtonFieldColors(
                    textColor = Color.Green
                )
            )
            typography = FeatureFormDefaults.typography(
                radioButtonFieldTypography = FeatureFormDefaults.radioButtonFieldTypography(
                    labelStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Red
                    )
                )
            )
            FeatureForm(
                featureForm = featureForm,
                colorScheme = colorScheme,
                typography = typography
            )
        }
        val formElement = featureForm.getFieldFormElementWithLabel("Radio Button")
        assertThat(formElement).isNotNull()
        val label = composeTestRule.onNodeWithText(formElement!!.label, useUnmergedTree = true)
        label.assertIsDisplayed()
        label.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Red
            )
        )
        val text =
            composeTestRule.onNodeWithText(formElement.formattedValue, useUnmergedTree = true)
        text.assertIsDisplayed()
        text.assertTextColor(Color.Green)
    }

    /**
     * Given a FeatureForm with a custom color scheme and typography for group elements
     * When the FeatureForm is displayed
     * Then the custom color scheme and typography are applied to the group form elements
     */
    @Test
    fun testGroupElementTheming() {
        var colorScheme: FeatureFormColorScheme
        var typography: FeatureFormTypography
        composeTestRule.setContent {
            colorScheme = FeatureFormDefaults.colorScheme(
                groupElementColors = FeatureFormDefaults.groupElementColors(
                    supportingTextColor = Color.Green
                )
            )
            typography = FeatureFormDefaults.typography(
                groupElementTypography = FeatureFormDefaults.groupElementTypography(
                    labelStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Blue
                    )
                )
            )
            FeatureForm(
                featureForm = featureForm,
                colorScheme = colorScheme,
                typography = typography
            )
        }
        val groupElement = featureForm.getGroupFormElementWithLabel("Group One")
        assertThat(groupElement).isNotNull()
        val label = composeTestRule.onNodeWithText(groupElement!!.label, useUnmergedTree = true)
        label.assertIsDisplayed()
        label.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Blue
            )
        )
        val supportingText =
            composeTestRule.onNodeWithText(groupElement.description, useUnmergedTree = true)
        supportingText.assertIsDisplayed()
        supportingText.assertTextColor(Color.Green)
    }

    /**
     * Given a FeatureForm with a custom color scheme and typography for attachments elements
     * When the FeatureForm is displayed
     * Then the custom color scheme and typography are applied to the attachments form elements
     */
    @Test
    fun testAttachmentsElementTheming() {
        var colorScheme: FeatureFormColorScheme
        var typography: FeatureFormTypography
        composeTestRule.setContent {
            colorScheme = FeatureFormDefaults.colorScheme(
                attachmentsElementColors = FeatureFormDefaults.attachmentsElementColors(
                    labelColor = Color.Red,
                    tileTextColor = Color.Green
                )
            )
            typography = FeatureFormDefaults.typography(
                attachmentsElementTypography = FeatureFormDefaults.attachmentsElementTypography(
                    labelStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    tileTextStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    tileSupportingTextStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            )
            FeatureForm(
                featureForm = featureForm,
                colorScheme = colorScheme,
                typography = typography
            )
        }
        val attachmentsElement = featureForm.defaultAttachmentsElement
        assertThat(attachmentsElement).isNotNull()
        // find the scrollable container
        val lazyColumn = composeTestRule.onNodeWithContentDescription("lazy column")
        composeTestRule.waitUntil(
            timeoutMillis = 2_000
        ) {
            // wait until the attachments are loaded
            attachmentsElement!!.attachments.isNotEmpty()
        }
        val attachmentToTest = attachmentsElement!!.attachments.first()
        // scroll until the attachment element is visible
        lazyColumn.performScrollToNode(hasText(attachmentsElement.label))
        val attachmentsField = composeTestRule.onNodeWithText(attachmentsElement.label)
        // bring the entire attachment tile into view
        attachmentsField.performScrollTo()
        attachmentsField.assertIsDisplayed()
        attachmentsField.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Red
            )
        )
        // get the first attachment tile
        val tile = attachmentsField.onChildWithText(attachmentToTest.name)
        tile.assertIsDisplayed()
        tile.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Green
            )
        )
        val fileSize = Formatter.formatFileSize(context, attachmentToTest.size)
        val size = attachmentsField.onChildWithText(fileSize)
        size.assertIsDisplayed()
        size.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Green
            )
        )
    }
}
