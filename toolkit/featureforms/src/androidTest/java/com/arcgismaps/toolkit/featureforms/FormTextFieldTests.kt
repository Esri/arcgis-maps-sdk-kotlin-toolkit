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

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.rememberFormTextFieldState
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class FormTextFieldTests {
    private val labelSemanticLabel = "label"
    private val supportingTextSemanticLabel = "supporting text"
    private val outlinedTextFieldSemanticLabel = "outlined text field"
    private val charCountSemanticLabel = "char count"
    private val clearTextSemanticLabel = "Clear text button"
    
    private val featureForm by lazy {
        sharedFeatureForm!!
    }

    private val field by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
    }
    
    private val errorTextColor = Color(
        red = 0.7019608f,
        green = 0.14901961f,
        blue = 0.11764706f
    )
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setContent()  = runTest {
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val state = rememberFormTextFieldState(
                field = field,
                minLength = (field.input as TextBoxFormInput).minLength.toInt(),
                maxLength = (field.input as TextBoxFormInput).maxLength.toInt(),
                form = featureForm,
                scope = scope,
            )
            FormTextField(
                state = state
            )
        }
        featureForm.evaluateExpressions()
    }
    
    @After
    fun clearText() {
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        // clear out any text added to this empty field during tests
        outlinedTextField.performTextClearance()
    }
    
    /**
     * Test case 1.1:
     * Given a FormTextField with no value, placeholder, or description
     * When it is unfocused
     * Then the label is displayed and the helper text composable does not exist.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-unfocused-and-focused-state-no-value
     */
    @Test
    fun testNoValueUnfocusedState() = runTest {
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val supportingText = composeTestRule.onNode(hasContentDescription(supportingTextSemanticLabel), useUnmergedTree = true)
        supportingText.assertExists()
    }
    
    /**
     * Test case 1.1:
     * Given a FormTextField with no value, placeholder, description, but with a max length.
     * When it is focused
     * Then the label is displayed and the helper text is displayed, indicating the max length of the form field text.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-unfocused-and-focused-state-no-value
     */
    @Test
    fun testNoValueFocusedState() = runTest {
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        outlinedTextField.performClick()
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val supportingText = composeTestRule.onNode(hasContentDescription(supportingTextSemanticLabel), useUnmergedTree = true)
        supportingText.assertExists()
        assertEquals(field.description, supportingText.getTextString())
    }
    
    /**
     * Test case 1.2:
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered
     * Then the label, the helper text, and the char count are displayed.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-12-focused-and-unfocused-state-with-value-populated
     */
    @Test
    fun testEnteredValueFocusedState() = runTest {
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = "lorem ipsum"
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val supportingText = composeTestRule.onNode(hasContentDescription(supportingTextSemanticLabel), useUnmergedTree = true)
        supportingText.assertExists()
        assertEquals(field.description, supportingText.getTextString())
        
        val charCountNode =
            composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        val charCountText = charCountNode.getTextString()
        charCountNode.assertIsDisplayed()
        assertEquals(text.length.toString(), charCountText)
        
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    /**
     * Test case 1.2:
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered, but the focus is elsewhere
     * Then the label is displayed. but the helper text, and the char count are not displayed.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-12-focused-and-unfocused-state-with-value-populated
     */
    @Test
    fun testEnteredValueUnfocusedState() {
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel, useUnmergedTree = true)
        outlinedTextField.printToLog("TAG")
        val text = "lorem ipsum"
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val supportingText = composeTestRule.onNode(hasContentDescription(supportingTextSemanticLabel), useUnmergedTree = true)
        supportingText.assertExists()
        assertEquals(field.description, supportingText.getTextString())
        
        outlinedTextField.performImeAction()
        outlinedTextField.assertIsNotFocused()
        
        val charCountNode =
            composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        charCountNode.assertDoesNotExist()
        
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    /**
     * Test case 1.3:
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered that exceeds the max length of the form input
     * Then the label is displayed, and the helper text and the char count are displayed in red.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-13-unfocused-and-focused-state-with-error-value--254-chars
     */
    @Test
    fun testErrorValueFocusedState() = runTest {
        val maxLength = (field.input as TextBoxFormInput).maxLength.toInt()
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
        
        val supportingText = composeTestRule.onNode(hasContentDescription(supportingTextSemanticLabel), useUnmergedTree = true)
        supportingText.assertIsDisplayed()
        
        assertEquals("Maximum $maxLength characters", supportingText.getTextString())
        supportingText.assertTextColor(errorTextColor)
        
        val charCountNode =
            composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        val charCountText = charCountNode.getTextString()
        charCountNode.assertIsDisplayed()
        assertEquals(text.length.toString(), charCountText)
        charCountNode.assertTextColor(errorTextColor)
        
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    /**
     * Test case 1.3:
     * Given a FormTextField with no value, placeholder, or description
     * When a value is entered, but the focus is elsewhere
     * Then the label is displayed, the helper text is displayed in red, and the char count is not displayed.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-13-unfocused-and-focused-state-with-error-value--254-char
     */
    @Test
    fun testErrorValueUnfocusedState() = runTest {
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val maxLength = (field.input as TextBoxFormInput).maxLength.toInt()
        val text = buildString {
            repeat(maxLength + 1) {
                append("x")
            }
        }
        outlinedTextField.performTextInput(text)
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val supportingText = composeTestRule.onNode(hasContentDescription(supportingTextSemanticLabel), useUnmergedTree = true)
        supportingText.assertIsDisplayed()
        assertEquals("Maximum $maxLength characters", supportingText.getTextString())
        
        outlinedTextField.performImeAction()
        outlinedTextField.assertIsNotFocused()
        supportingText.assertTextColor(errorTextColor)
        
        val charCountNode =
            composeTestRule.onNode(hasContentDescription(charCountSemanticLabel), useUnmergedTree = true)
        charCountNode.assertDoesNotExist()
        
        val clearButton = composeTestRule.onNode(hasContentDescription(clearTextSemanticLabel), useUnmergedTree = true)
        clearButton.assertExists()
    }
    
    companion object {
        var sharedFeatureFormDefinition: FeatureFormDefinition? = null
        var sharedFeatureForm: FeatureForm? = null
        var sharedFeature: ArcGISFeature? = null
        var sharedMap: ArcGISMap? = null
        
        @BeforeClass
        @JvmStatic
        fun setupClass() = runTest {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
                FeatureFormsTestChallengeHandler(
                    BuildConfig.webMapUser,
                    BuildConfig.webMapPassword
                )
            
            sharedMap =
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=5d69e2301ad14ec8a73b568dfc29450a")
            sharedMap?.load()?.onFailure { fail("failed to load webmap with ${it.message}") }
            val featureLayer = sharedMap?.operationalLayers?.first() as? FeatureLayer
            featureLayer?.let { layer ->
                layer.load().onFailure { fail("failed to load layer with ${it.message}") }
                sharedFeatureFormDefinition = layer.featureFormDefinition!!
                val parameters = QueryParameters().also {
                    it.whereClause = "1=1"
                    it.maxFeatures = 1
                }
                layer.featureTable?.queryFeatures(parameters)?.onSuccess {
                    sharedFeature = it.filterIsInstance<ArcGISFeature>().first()
                    sharedFeature?.load()?.onFailure { fail("failed to load feature with ${it.message}") }
                    sharedFeatureForm = FeatureForm(sharedFeature!!, sharedFeatureFormDefinition!!)
                }?.onFailure {
                    fail("failed to query features on layer's featuretable with ${it.message}")
                }
            }
        }
    }
    
}
