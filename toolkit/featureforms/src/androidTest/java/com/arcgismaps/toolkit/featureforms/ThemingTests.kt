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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.theme.EditableTextFieldColors
import com.arcgismaps.toolkit.featureforms.theme.EditableTextFieldTypography
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ThemingTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getFormElementWithLabel(label: String): FieldFormElement {
        return featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == label
            }
    }

    @Test
    fun testEditableFieldTheming() {
        var colorScheme : FeatureFormColorScheme
        var typography : FeatureFormTypography
        composeTestRule.setContent {
            colorScheme = FeatureFormColorScheme.createDefaults(
                editableTextFieldColors = EditableTextFieldColors.createDefaults(
                    focusedTextColor = Color.Red
                )
            )
            typography = FeatureFormTypography.createDefaults(
                EditableTextFieldTypography.createDefaults(
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
        val formElement = getFormElementWithLabel("Text Box")
        val label = composeTestRule.onNodeWithText(formElement.label, useUnmergedTree = true)
        label.assertIsDisplayed()
        label.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Green
            )
        )
        val text = composeTestRule.onNodeWithText(formElement.formattedValue, useUnmergedTree = true)
        text.assertIsDisplayed()
        text.performClick()
        text.assertTextColor(Color.Red)
    }

    @Test
    fun testPlaceHolderTransformation() {

    }

    @Test
    fun testReadOnlyFieldTheming() {

    }

    @Test
    fun testRadioButtonFieldTheming() {

    }

    @Test
    fun testGroupElementTheming() {

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
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=615e8fe546ef4d139fb9298515c2f40a")
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
