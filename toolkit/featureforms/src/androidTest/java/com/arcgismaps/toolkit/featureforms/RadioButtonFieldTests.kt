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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.layers.FeatureLayer
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
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
     * Given a RadioFormInput with a pre-existing value and a no value label
     * When the FeatureForm is displayed
     * Then the RadioButtonField shows a no value option and indicates the pre-existing value is selected
     * And a new option is selected
     * Then the new selection is visible
     */
    @Test
    fun testRadioButtonSelection() {
        val radioElement = featureForm.getFieldFormElementWithLabel("Radio Button Text")
            ?: return fail("element not found")
        val input = radioElement.input as RadioButtonsFormInput
        // find the field with the the label
        val radioField = composeTestRule.onNodeWithText(radioElement.label)
        // assert it is displayed
        radioField.assertIsDisplayed()
        // assert the node has group selection indicating it is a radio button field
        radioField.assert(SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit))
        // assert "no value" option is visible
        radioField.onChildWithText(input.noValueLabel.ifEmpty { context.getString(R.string.no_value) }).assertExists()
        // check if the current value of the element is visible selected
        radioField.onChildWithText(radioElement.formattedValue).assertIsSelected()
        // select the "dog" option
        radioField.onChildWithText("dog").performClick()
        // assert that the selected value has persisted
        assert(radioElement.formattedValue == "dog")
        // check if the current value of the element is visible selected
        radioField.onChildWithText(radioElement.formattedValue).assertIsSelected()
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
                    val feature = featureQueryResult.filterIsInstance<ArcGISFeature>().firstOrNull()
                    if (feature == null) TestCase.fail("failed to fetch feature")
                    feature?.load()
                        ?.onFailure { TestCase.fail("failed to load feature with ${it.message}") }
                    featureForm = FeatureForm(feature!!, featureFormDefinition)
                    featureForm.evaluateExpressions()
                }?.onFailure {
                    TestCase.fail("failed to query features on layer's featuretable with ${it.message}")
                }
            }
        }
    }
}
