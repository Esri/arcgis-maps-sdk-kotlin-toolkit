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

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TextFormElementTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=e10c0061182c4102a109dc6b030aa9ef",
    objectId = 1
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test case 10.1:
     * Given a `FeatureForm` with a `TextFormElement` that references a different field
     * When the `FeatureForm` is displayed
     * And the field is updated
     * Then the `TextFormElement` displays the correct updated text
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-101-test-substitution
     */
    @Test
    fun testTextFormElementSubstitution() = runTest {
        composeTestRule.setContent {
            MaterialTheme {
                FeatureForm(featureForm = featureForm)
            }
        }
        // verify initial state
        val fieldFormElement = featureForm.getFieldFormElementWithLabel("Title")
        assertThat(fieldFormElement).isNotNull()
        val titleNode = composeTestRule.onNodeWithText(fieldFormElement!!.label)
        titleNode.assertIsDisplayed()
        titleNode.assertEditableTextEquals(fieldFormElement.formattedValue)
        // verify the text form element displays the correct initial text
        val textFormElement = featureForm.elements[1] as TextFormElement
        val initialSubstitutedText = "Title of the map is ${fieldFormElement.formattedValue}."
        assertThat(textFormElement.text.value).isEqualTo(initialSubstitutedText)
        composeTestRule.onNodeWithText(initialSubstitutedText).assertIsDisplayed()
        // enter new text into the field form element which drives the substitution in the text form element
        titleNode.performTextClearance()
        titleNode.performTextInput("Los Angeles")
        titleNode.performImeAction()
        assertThat(fieldFormElement.formattedValue).isEqualTo("Los Angeles")
        // verify the text form element displays the correct updated text
        val updatedSubstitutedText = "Title of the map is Los Angeles."
        assertThat(textFormElement.text.value).isEqualTo(updatedSubstitutedText)
        composeTestRule.onNodeWithText(updatedSubstitutedText).assertIsDisplayed()
    }

    /**
     * Test case 10.2:
     *  Given a `FeatureForm` with a `TextFormElement` with a plain-text format
     *  When the `FeatureForm` is displayed
     *  Then the `TextFormElement` displays the text as plain text without any formatting
     *
     *  https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-102-test-plain-text
     */
    @Test
    fun testTextFormElementDisplaysPlainText() {
        composeTestRule.setContent {
            MaterialTheme {
                FeatureForm(featureForm = featureForm)
            }
        }
        val textFormElement = featureForm.elements[2] as TextFormElement
        composeTestRule.onNodeWithText(textFormElement.text.value).assertIsDisplayed()
    }
}
