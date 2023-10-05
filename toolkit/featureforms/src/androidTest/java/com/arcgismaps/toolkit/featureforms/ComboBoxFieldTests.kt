/*
 * Copyright 2023 Esri
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextClearance
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxField
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxFieldProperties
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxFieldState
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ComboBoxFieldTests {
    private val labelSemanticLabel = "label"
    private val descriptionSemanticLabel = "description"
    private val outlinedTextFieldSemanticLabel = "outlined text field"
    private val charCountSemanticLabel = "char count"
    private val clearTextSemanticLabel = "Clear text button"
    private lateinit var context : Context

    private val featureForm by lazy {
        sharedFeatureForm!!
    }

    private val formElement by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "Combo String"
            }
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setContent() = runTest {
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val input = (formElement.input as ComboBoxFormInput)
            val state = ComboBoxFieldState(
                properties = ComboBoxFieldProperties(
                    label = formElement.label,
                    placeholder = formElement.hint,
                    description = formElement.description,
                    value = formElement.value,
                    required = formElement.isRequired,
                    editable = formElement.isEditable,
                    codedValues = input.codedValues,
                    showNoValueOption = input.noValueOption,
                    noValueLabel = input.noValueLabel
                ),
                scope = scope,
                context = context,
                onEditValue = {}
            )
            ComboBoxField(state = state)
        }
    }

    /**
     * Test case 3.1:
     * Given a ComboBoxField with a pre-existing value, description and a no value label
     * When the pre-existing value is cleared
     * Then the ComboBoxField shows "No Value"
     * https://devtopia.esri.com/runtime/common-toolkit/blob/ace0cb49775dd2bdeae88a0d1e8b2695ed820feb/designs/Forms/FormsTestDesign.md#test-case-31-pre-existing-value-description-clear-button-no-value-label
     */
    @Test
    fun testClearValueNoValueLabel() {
        val comboBoxField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        comboBoxField.assertIsNotFocused()

        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        comboBoxField.assertTextEquals(formElement.label, formElement.value.value, includeEditableText = true)

        val description = composeTestRule.onNode(hasContentDescription(descriptionSemanticLabel), useUnmergedTree = true)
        val descriptionText = description.getTextString()
        description.assertIsDisplayed()
        assertEquals(descriptionText, formElement.description)
        // clear the value
        comboBoxField.performTextClearance()

        // assert "no value" placeholder is visible, this method is needed due to the use of
        // a PlaceHolderTransformation
        for ((k, v) in comboBoxField.fetchSemanticsNode().config) {
            if (k.name == "EditableText"){
                assertThat(v.toString(), equalToIgnoringCase("no value"))
            }
        }
    }

    @Test
    fun testNoValueAndNoValueLabel() {

    }

    @Test
    fun testEnteredValueWithComboBoxPicker() {

    }

    companion object {
        private var sharedFeatureFormDefinition: FeatureFormDefinition? = null
        private var sharedFeatureForm: FeatureForm? = null
        private var sharedFeature: ArcGISFeature? = null
        private var sharedMap: ArcGISMap? = null

        @BeforeClass
        @JvmStatic
        fun setupClass() = runTest {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
                FeatureFormsTestChallengeHandler(
                    BuildConfig.webMapUser,
                    BuildConfig.webMapPassword
                )

            sharedMap =
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=ed930cf0eb724ea49c6bccd8fd3dd9af")
            sharedMap?.load()?.onFailure { fail("failed to load webmap with ${it.message}") }
            val featureLayer = sharedMap?.operationalLayers?.first() as? FeatureLayer
            featureLayer?.let { layer ->
                layer.load().onFailure { fail("failed to load layer with ${it.message}") }
                sharedFeatureFormDefinition = layer.featureFormDefinition!!
                val parameters = QueryParameters().also {
                    it.objectIds.add(2L)
                    it.maxFeatures = 1
                }
                layer.featureTable?.queryFeatures(parameters)?.onSuccess {
                    sharedFeature = it.filterIsInstance<ArcGISFeature>().firstOrNull()
                    if (sharedFeature == null) fail("failed to fetch feature")
                    sharedFeature?.load()
                        ?.onFailure { fail("failed to load feature with ${it.message}") }
                    sharedFeatureForm = FeatureForm(sharedFeature!!, sharedFeatureFormDefinition!!)
                    sharedFeatureForm!!.evaluateExpressions()
                }?.onFailure {
                    fail("failed to query features on layer's featuretable with ${it.message}")
                }
            }
        }
    }
}
