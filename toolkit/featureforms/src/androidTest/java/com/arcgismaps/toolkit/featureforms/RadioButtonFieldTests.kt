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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.layers.FeatureLayer
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class RadioButtonFieldTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        composeTestRule.setContent {
            context = LocalContext.current
            FeatureForm(featureForm = featureForm)
        }
    }

    /**
     * Given a RadioFormInput with a pre-existing value
     * When the FeatureForm is displayed
     * Then the RadioButtonField indicates the pre-existing value is selected
     * And when a new option is selected
     * Then the new selection is visible
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-41-test-regular-selection
     */
    @Test
    fun testRadioButtonSelection() {
        val radioElement = featureForm.getFieldFormElementWithLabel("Radio Button Text")
            ?: return fail("element not found")
        // find the field with the the label
        val radioField = composeTestRule.onNodeWithText("${radioElement.label} *")
        // assert it is displayed
        radioField.assertIsDisplayed()
        // assert the node has group selection indicating it is a radio button field
        radioField.assert(SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit))
        // check if the current value of the element is visible and selected
        radioField.onChildWithText(radioElement.formattedValue).assertIsSelected()
        // select the "dog" option
        radioField.onChildWithText("dog").performClick()
        // assert that the selected value has persisted
        assert(radioElement.formattedValue == "dog")
        // check if the current value of the element is visible and selected
        radioField.onChildWithText(radioElement.formattedValue).assertIsSelected()
    }

    /**
     * Given a RadioFormInput with a pre-existing value that is not in the given domain
     * When the FeatureForm is displayed
     * Then the FieldFormElement is rendered as a ComboBoxField
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-42-test-radio-button-fallback-to-combo-box-and-no-value-label
     */
    @Test
    fun testFallback() {
        // find the lazy column and scroll to the appropriate index
        val column = composeTestRule.onNodeWithContentDescription("lazy column")
        column.performScrollToIndex(7)
        val radioElement = featureForm.getFieldFormElementWithLabel("Fallback 1")
            ?: return fail("element not found")
        // find the field with the the label
        val radioField = composeTestRule.onNodeWithText(radioElement.label)
        // assert it is displayed
        radioField.assertIsDisplayed()
        // assert the node does not have a  group selection indicating it is not a radio field
        radioField.assert(!SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit))
        // also assert that this is rendered with an outlined text field which also assures
        // the fallback behavior
        radioField.assertContentDescriptionContains("outlined text field")
    }

    /**
     * Given a RadioFormInput with no pre-existing value and a noValueLabel
     * When the FeatureForm is displayed
     * Then the RadioButtonField displays the noValueLabel
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-42-test-radio-button-fallback-to-combo-box-and-no-value-label
     */
    @Test
    fun testNoValueLabelExists() {
        // find the lazy column and scroll to the appropriate index
        val column = composeTestRule.onNodeWithContentDescription("lazy column")
        column.performScrollToIndex(8)
        val radioElement = featureForm.getFieldFormElementWithLabel("No Value Enabled")
            ?: return fail("element not found")
        val input = radioElement.input as RadioButtonsFormInput
        // find the field with the the label
        val radioField = composeTestRule.onNodeWithText(radioElement.label)
        // assert it is displayed
        radioField.assertIsDisplayed()
        // assert the node has group selection indicating it is a radio button field
        radioField.assert(SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit))
        // assert the no value label is visible
        radioField.onChildWithText(input.noValueLabel).assertExists()
    }

    /**
     * Given a RadioFormInput with no pre-existing value and no noValueLabel
     * When the FeatureForm is displayed
     * Then the RadioButtonField does not display the noValueLabel
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-42-test-radio-button-fallback-to-combo-box-and-no-value-label
     */
    @Test
    fun testNoValueLabelDoesNotExist() {
        // find the lazy column and scroll to the appropriate index
        val column = composeTestRule.onNodeWithContentDescription("lazy column")
        column.performScrollToIndex(8)
        val radioElement = featureForm.getFieldFormElementWithLabel("No Value Disabled")
            ?: return fail("element not found")
        val input = radioElement.input as RadioButtonsFormInput
        // find the field with the the label
        val radioField = composeTestRule.onNodeWithText(radioElement.label)
        // assert it is displayed
        radioField.assertIsDisplayed()
        // assert the node has group selection indicating it is a radio button field
        radioField.assert(SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit))
        // assert the no value label is not visible by catching the assertion error
        assertThrows(AssertionError::class.java) {
            radioField.onChildWithText(input.noValueLabel)
        }
    }

    companion object {
        private lateinit var featureForm: FeatureForm

        @BeforeClass
        @JvmStatic
        fun setupClass() = runTest {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
                FeatureFormsTestChallengeHandler(
                    BuildConfig.webMapUser,
                    BuildConfig.webMapPassword
                )
            val map =
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=476e9b4180234961809485c8eff83d5d")
            map.load().onFailure { TestCase.fail("failed to load webmap with ${it.message}") }
            val featureLayer = map.operationalLayers.first() as? FeatureLayer
            featureLayer?.let { layer ->
                layer.load().onFailure { TestCase.fail("failed to load layer with ${it.message}") }
                val featureFormDefinition = layer.featureFormDefinition!!
                val parameters = QueryParameters().also {
                    it.objectIds.add(1L)
                    it.maxFeatures = 1
                }
                layer.featureTable?.queryFeatures(parameters)?.onSuccess { featureQueryResult ->
                    val feature = featureQueryResult.find {
                        it is ArcGISFeature
                    } as? ArcGISFeature
                    if (feature == null) TestCase.fail("failed to fetch feature")
                    feature?.load()?.onFailure {
                        TestCase.fail("failed to load feature with ${it.message}")
                    }
                    featureForm = FeatureForm(feature!!, featureFormDefinition)
                    featureForm.evaluateExpressions()
                }?.onFailure {
                    TestCase.fail("failed to query features on layer's featuretable with ${it.message}")
                }
            }
        }
    }
}
