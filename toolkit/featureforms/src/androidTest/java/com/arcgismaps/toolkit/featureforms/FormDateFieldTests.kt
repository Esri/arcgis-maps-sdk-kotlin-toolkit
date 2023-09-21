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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextClearance
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormDateFieldTests {
    private val labelSemanticLabel = "label"
    private val helperSemanticLabel = "helper"
    private val outlinedTextFieldSemanticLabel = "outlined text field"
    private val charCountSemanticLabel = "char count"
    private val clearTextSemanticLabel = "Clear text button"
    
    private val featureForm by lazy {
        sharedFeatureForm!!
    }
    
    private val fieldFeatureFormElement by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "Required Date"
            }
    }
    
    private val populatedFieldFeatureFormElement by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "Launch Date and Time for Apollo 11"
            }
    }
    
    private val errorTextColor = Color(
        red = 0.7019608f,
        green = 0.14901961f,
        blue = 0.11764706f
    )
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @After
    fun clearText() {
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        // clear out any text added to this empty field during tests
        outlinedTextField.performTextClearance()
    }
    
    /**
     * Given a required DateTimeField with no value
     * When it is unfocused
     * Then the label is displayed with an asterisk and the helper text says Required in red
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-21-unfocused-and-focused-state-no-value-date-required
     */
    @Test
    fun testNoValueUnfocusedStateRequiredField() = runTest {
        composeTestRule.setContent {
            DateTimeField(
                state = DateTimeFieldState(
                    fieldFeatureFormElement,
                    featureForm
                )
            )
        }
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        outlinedTextField.assertIsNotFocused()
        
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        val labelText = label.getTextString()
        assertEquals("Required Date *", labelText)
    
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
    
        assertEquals("Required", helperText)
        helper.assertTextColor(errorTextColor)
        
    }
    
    /**
     * Given a required DateTimeField with no value
     * When it is unfocused
     * Then the label is displayed with an asterisk and the helper text says Required in red
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-21-unfocused-and-focused-state-no-value-date-required
     */
    @Test
    fun testPopulatedValueUnfocusedStateRequiredField() = runTest {
        composeTestRule.setContent {
            DateTimeField(
                state = DateTimeFieldState(
                    populatedFieldFeatureFormElement,
                    featureForm
                )
            )
        }
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        outlinedTextField.assertIsNotFocused()

        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
    
        for ((key,value) in outlinedTextField.fetchSemanticsNode().config)
            if (key.name =="EditableText")
                assertEquals("Jul 07, 1969 8:17 PM", value.toString())


        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()

        assertEquals("Enter the launch date and time (July 7, 1969 20:17 UTC)", helperText)
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
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=ec09090060664cbda8d814e017337837")
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
