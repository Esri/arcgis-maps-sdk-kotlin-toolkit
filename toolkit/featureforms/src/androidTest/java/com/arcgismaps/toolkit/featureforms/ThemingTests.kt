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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ThemingTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getFormElementWithLabel(label: String): FormElement {
        return featureForm.elements.first {
                it.label == label
            }
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
        val formElement = getFormElementWithLabel("Text Box") as FieldFormElement
        val label = composeTestRule.onNodeWithText(formElement.label, useUnmergedTree = true)
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
        val formElement = getFormElementWithLabel("An empty field") as FieldFormElement
        val field = composeTestRule.onNodeWithText(formElement.label)
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
        // test focused text color
        text.assertTextColor(Color.Blue)
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
        val formElement = getFormElementWithLabel("Name") as FieldFormElement
        val label = composeTestRule.onNodeWithText(formElement.label, useUnmergedTree = true)
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
        val formElement = getFormElementWithLabel("Radio Button") as FieldFormElement
        val label = composeTestRule.onNodeWithText(formElement.label, useUnmergedTree = true)
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
        val groupElement = getFormElementWithLabel("Group One") as GroupFormElement
        val label = composeTestRule.onNodeWithText(groupElement.label, useUnmergedTree = true)
        label.assertIsDisplayed()
        label.assertTextStyle(
            TextStyle(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Blue
            )
        )
        val supportingText = composeTestRule.onNodeWithText(groupElement.description, useUnmergedTree = true)
        supportingText.assertIsDisplayed()
        supportingText.assertTextColor(Color.Green)
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
