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
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class CalculatedFieldTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test case 9.1:
     * Given a `FieldFormElement` with `hasValueExpression` set to `true` and validation errors
     * When the `FeatureForm` is displayed
     * Then the appropriate validation error messages are displayed
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-91-test-validation-on-elements-with-expressions
     */
    @Test
    fun testValidationForCalculatedFields() = runTest {
        composeTestRule.setContent {
            MaterialTheme {
                FeatureForm(featureForm = featureForm)
            }
        }
        val contentDescription = "calculated field"
        var field = composeTestRule.onNodeWithText("singleCharacterString")
        field.assertIsDisplayed()
        field.assertTextContains("Value must be 1 character")
        field.assertContentDescriptionContains(contentDescription)

        field = composeTestRule.onNodeWithText("lengthRangeString")
        field.assertIsDisplayed()
        field.assertTextContains("Value must be 2 to 5 characters")
        field.assertContentDescriptionContains(contentDescription)

        field = composeTestRule.onNodeWithText("maxExceededString")
        field.assertIsDisplayed()
        field.assertTextContains("Maximum 5 characters")
        field.assertContentDescriptionContains(contentDescription)

        field = composeTestRule.onNodeWithText("numericalRange")
        field.assertIsDisplayed()
        field.assertTextContains("Value must be from 2 to 5")
        field.assertContentDescriptionContains(contentDescription)
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
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/apps/mapviewer/index.html?webmap=5f71b243b37e43a5ace3190241db0ac9")
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
