/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureforms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormTextFieldTests {
    lateinit var featureFormDefinition: FeatureFormDefinition
    val labelSemanticLabel = "label"
    val helperSemanticLabel = "helper"
    val outlinedTextFieldSemanticLabel = "outlined text field"
    val charCountSemanticLabel = "char count"
    val clearTextSemanticLabel = "Clear text button"
    
    @Before
    fun setUp() = runTest {
        featureFormDefinition = FeatureFormDefinition.fromJsonOrNull(TestData.inputValidationFeatureFormJson)!!
    }
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Given a FormTextField with no value, placeholder, or description
     * When it is unfocused
     * Then the label is displayed and the helper text composable does not exist.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-unfocused-and-focused-state-no-value
     */
    @Test
    fun testNoValueUnfocusedState() = runTest {
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement, featureFormDefinition, LocalContext.current))
        }
        
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        helper.assertDoesNotExist()
    }
    
    /**
     * Given a FormTextField with no value, placeholder, description, but with a max length.
     * When it is focused
     * Then the label is displayed and the helper text is displayed, indicating the max length of the form field text.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-unfocused-and-focused-state-no-value
     */
    @Test
    fun testNoValueFocusedState() = runTest {
        
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement, featureFormDefinition, LocalContext.current))
        }
        
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        outlinedTextField.performClick()
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
        val maxLength = (fieldFeatureFormElement.inputType as TextBoxFeatureFormInput).maxLength.toInt()
        assertEquals("Maximum $maxLength characters", helperText)
    }
    
    /**
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered
     * Then the label, the helper text, and the char count are displayed.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-12-focused-and-unfocused-state-with-value-populated
     */
    @Test
    fun testEnteredValueFocusedState() = runTest {
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement, featureFormDefinition, LocalContext.current))
        }
        
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = "lorem ipsum"
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
    
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
        val maxLength = (fieldFeatureFormElement.inputType as TextBoxFeatureFormInput).maxLength.toInt()
        assertEquals("Maximum $maxLength characters", helperText)
        
        val charCountNode = composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        val charCountText = charCountNode.getTextString()
        charCountNode.assertIsDisplayed()
        assertEquals(text.length.toString(), charCountText)
    
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    /**
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered, but the focus is elsewhere
     * Then the label is displayed. bue the helper text, and the char count are not displayed.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-12-focused-and-unfocused-state-with-value-populated
     */
    @Test
    fun testEnteredValueUnfocusedState() = runTest {
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement, featureFormDefinition, LocalContext.current))
        }
        
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = "lorem ipsum"
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
        val maxLength = (fieldFeatureFormElement.inputType as TextBoxFeatureFormInput).maxLength.toInt()
        assertEquals("Maximum $maxLength characters", helperText)
        
        outlinedTextField.performImeAction()
        outlinedTextField.assertIsNotFocused()
        helper.assertDoesNotExist()
    
        val charCountNode = composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        charCountNode.assertDoesNotExist()
    
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    /**
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered that exceeds the max length of the form input
     * Then the label is displayed, and the helper text and the char count are displayed in red.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-13-unfocused-and-focused-state-with-error-value--254-chars
     */
    @Test
    fun testErrorValueFocusedState() = runTest {
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement, featureFormDefinition, LocalContext.current))
        }
    
        val maxLength = (fieldFeatureFormElement.inputType as TextBoxFeatureFormInput).maxLength.toInt()
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = buildString {
            repeat(maxLength + 1) {
                append("x")
            }
        }
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
        
        assertEquals("Maximum $maxLength characters", helperText)
        helper.assertTextColor(Color.Red)
        
        val charCountNode = composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        val charCountText = charCountNode.getTextString()
        charCountNode.assertIsDisplayed()
        assertEquals(text.length.toString(), charCountText)
        charCountNode.assertTextColor(Color.Red)
        
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    /**
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered, but the focus is elsewhere
     * Then the label is displayed, the helper text is displayed in red, and the char count is not displayed.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-13-unfocused-and-focused-state-with-error-value--254-char
     */
    @Test
    fun testErrorValueUnfocusedState() = runTest {
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement, featureFormDefinition, LocalContext.current))
        }
        
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val maxLength = (fieldFeatureFormElement.inputType as TextBoxFeatureFormInput).maxLength.toInt()
        val text = buildString {
            repeat(maxLength + 1) {
                append("x")
            }
        }
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
        assertEquals("Maximum $maxLength characters", helperText)
        
        outlinedTextField.performImeAction()
        outlinedTextField.assertIsNotFocused()
        helper.assertTextColor(Color.Red)
        
        val charCountNode = composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        charCountNode.assertDoesNotExist()
        
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
}

fun SemanticsNodeInteraction.getAnnotatedTextString(): AnnotatedString {
    val textList = fetchSemanticsNode().config.first {
        it.key.name == "Text"
    }.value as List<*>
    return textList.first() as AnnotatedString
}
fun SemanticsNodeInteraction.getTextString(): String {
    return getAnnotatedTextString().text
}

fun SemanticsNodeInteraction.assertTextColor(
    color: Color
): SemanticsNodeInteraction = assert(isOfColor(color))

private fun isOfColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} is of color '$color'"
) {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    it.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action
        ?.invoke(textLayoutResults)
    return@SemanticsMatcher if (textLayoutResults.isEmpty()) {
        false
    } else {
        textLayoutResults.first().layoutInput.style.color == color
    }
}
