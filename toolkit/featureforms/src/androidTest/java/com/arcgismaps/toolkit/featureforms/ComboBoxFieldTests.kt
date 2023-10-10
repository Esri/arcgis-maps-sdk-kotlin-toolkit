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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
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
import junit.framework.TestCase.fail
import kotlinx.coroutines.test.runTest
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
    private val optionsIconSemanticLabel = "field icon"
    private val comboBoxDialogListSemanticLabel = "ComboBoxDialogLazyColumn"
    private lateinit var context: Context

    private val featureForm by lazy {
        sharedFeatureForm!!
    }

    private fun getFormElementWithLabel(label: String): FieldFormElement {
        return featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == label
            }
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setContent() {
        composeTestRule.setContent {
            val state = FeatureFormState()
            state.setFeatureForm(featureForm)
            FeatureForm(featureFormState = state)
        }
    }

    /**
     * Test case 3.1:
     * Given a ComboBoxField with a pre-existing value, description and a no value label
     * When the pre-existing value is cleared
     * Then the ComboBoxField shows the noValueLabel
     * https://devtopia.esri.com/runtime/common-toolkit/blob/ace0cb49775dd2bdeae88a0d1e8b2695ed820feb/designs/Forms/FormsTestDesign.md#test-case-31-pre-existing-value-description-clear-button-no-value-label
     */
    @Test
    fun testClearValueNoValueLabel() {
        val formElement = getFormElementWithLabel("Combo String")
        val input = formElement.input as ComboBoxFormInput
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(formElement.label)
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // find the child node with the description semantic label
        val descriptionNode = comboBoxField.onChildWithContentDescription(descriptionSemanticLabel)
        val hasDescriptionMatcher = hasText(formElement.description)
        // validate the correct description is visible
        assert(hasDescriptionMatcher.matches(descriptionNode.fetchSemanticsNode())) {
            "Failed to assert the following: ${hasDescriptionMatcher.description}"
        }
        // validate that the pre-populated value shown shown in accurate and as expected
        // assertTextEquals matches the Text(the label) and Editable Text (the actual editable input text)
        comboBoxField.assertTextEquals(formElement.label, formElement.value.value)
        // find the clear text node within its children
        val clearButton = comboBoxField.onChildWithContentDescription(clearTextSemanticLabel)
        // validate the clear icon is visible
        clearButton.assertIsDisplayed()
        // clear the value
        clearButton.performClick()
        // assert "no value" placeholder is visible
        // assertTextEquals matches the Text(the label) and Editable Text (the placeholder here
        // due to the use of the PlaceHolderTransformation)
        comboBoxField.assertTextEquals(
            formElement.label,
            input.noValueLabel.ifEmpty { context.getString(R.string.no_value) }
        )
    }

    /**
     * Test case 3.2:
     * Given a ComboBoxField with no pre-existing value, description and a no value label
     * When the field is observed
     * Then the ComboBoxField shows the noValueLabel and the options menu icon is visible
     * https://devtopia.esri.com/runtime/common-toolkit/blob/ace0cb49775dd2bdeae88a0d1e8b2695ed820feb/designs/Forms/FormsTestDesign.md#test-case-32-no-pre-existing-value-no-value-label-options-button
     */
    @Test
    fun testNoValueAndNoValueLabel() {
        val formElement = getFormElementWithLabel("Combo Integer")
        val input = formElement.input as ComboBoxFormInput
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(formElement.label)
        comboBoxField.printToLog("TAG")
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // find the child node with the description semantic label
        val descriptionNode = comboBoxField.onChildWithContentDescription(descriptionSemanticLabel)
        val hasDescriptionMatcher = hasText(formElement.description)
        // validate the correct description is visible
        assert(hasDescriptionMatcher.matches(descriptionNode.fetchSemanticsNode())) {
            "Failed to assert the following: ${hasDescriptionMatcher.description}"
        }
        // assert "no value" placeholder is visible
        // assertTextEquals matches the Text(the label) and Editable Text (the placeholder here
        // due to the use of the PlaceHolderTransformation)
        comboBoxField.assertTextEquals(
            formElement.label,
            input.noValueLabel.ifEmpty { context.getString(R.string.no_value) }
        )
        // validate that the options icon is visible
        val optionsIconNode = comboBoxField.assertContentDescriptionContains(optionsIconSemanticLabel)
        optionsIconNode.assertIsDisplayed()
    }

    /**
     * Test case 3.3:
     * Given a ComboBoxField with a pre-existing value, description and a no value label
     * When the ComboBoxField is tapped
     * Then the ComboBoxDialog is shown with the noValueLabel row AND all coded values are visible
     * with the selected value marked with a check
     * https://devtopia.esri.com/runtime/common-toolkit/blob/ace0cb49775dd2bdeae88a0d1e8b2695ed820feb/designs/Forms/FormsTestDesign.md#test-case-33-pick-a-value
     */
    @Test
    fun testEnteredValueWithComboBoxPicker() {
        val formElement = getFormElementWithLabel("Combo String")
        val input = formElement.input as ComboBoxFormInput
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(formElement.label)
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // find the child node with the description semantic label
        val descriptionNode = comboBoxField.onChildWithContentDescription(descriptionSemanticLabel)
        val hasDescriptionMatcher = hasText(formElement.description)
        // validate the correct description is visible
        assert(hasDescriptionMatcher.matches(descriptionNode.fetchSemanticsNode())) {
            "Failed to assert the following: ${hasDescriptionMatcher.description}"
        }
        // validate that the pre-populated value shown shown in accurate and as expected
        // assertTextEquals matches the Text(the label) and Editable Text (the actual editable input text)
        comboBoxField.assertTextEquals(formElement.label, formElement.value.value)
        // tap the value to bring up the picker
        comboBoxField.performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)
        // find the dialog
        val comboBoxDialogList = composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        val listItem = comboBoxDialogList.onChildWithContentDescription("String 1 list item")
        listItem.assertIsDisplayed()
    }

    /**
     * Test case 3.4: Picker with a noValueLabel row
     * Steps:
     * load the webmap listed below
     * access the form definition on the first operational layer
     * access the feature with object ID 2
     * access the FormElement with label "Combo String"
     * Expectation: the value is visible and equal to "String 3"
     * tap the value to bring up the picker
     * Expectation: the picker appears and a "No value" row (or ComboBoxFormInput.noValueLabel) is added to the top of the list
     * Expectation: and all the coded values for this field are visible as selectable rows
     * tap the ""No value" option
     * tap the "Done" button
     * Expectation: the value is visible and equal to "No value"
     */
    @Test
    fun testNoValueRow() {
        val formElement = getFormElementWithLabel("Combo String")
        val input = formElement.input as ComboBoxFormInput
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(formElement.label)
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // validate that the pre-populated value shown shown in accurate and as expected
        // assertTextEquals matches the Text(the label) and Editable Text (the actual editable input text)
        comboBoxField.assertTextEquals(formElement.label, formElement.value.value)
        // open the picker
        comboBoxField.performClick()
        // find the dialog
        val comboBoxDialogList = composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        comboBoxDialogList.assertIsDisplayed()
        // this field has a no value label and not required, hence check for the row
        comboBoxDialogList.onChildWithContentDescription(
            "${input.noValueLabel.ifEmpty { context.getString(R.string.no_value) }} list item"
        ).assertIsDisplayed()
        // validate all coded values rows are displayed
        input.codedValues.forEach {
            val listItem = comboBoxDialogList.onChildWithContentDescription("${it.name} list item")
            listItem.assertIsDisplayed()
            // if this item is selected, then validate the check mark is shown
            if (it.name == formElement.value.value) {
                listItem.onChildWithContentDescription("list item check").assertIsDisplayed()
            }
        }
    }

    /**
     * Test case 3.5: Required Value
     * Steps:
     * load the webmap listed below
     * access the form definition on the first operational layer
     * access the feature with object ID 2
     * access the FormElement with label Required Combo Box
     * Expectation: the value is visible and equal to "Pine"
     * tap the clear icon to clear the value
     * Expectation: the value is visible and equal to "Enter Value"
     * Expectation: the helper text is visible, has the platform default error color and says "Required"
     * tap the value to bring up the picker
     * Expectation: only the coded values for this field are visible as selectable rows
     * tap the "Oak" option
     * tap the "Done" button
     * Expectation: the value is visible and equal to "Oak"
     */
    @Test
    fun testRequiredValue() {

    }

    /**
     * Test case 3.6: noValueOption is 'Hide'
     * Steps:
     * load the webmap listed below
     * access the form definition on the first operational layer
     * access the feature with object ID 2
     * access the FormElement with label Combo No Value False
     * Expectation: the value is empty
     * tap the value to bring up the picker
     * Expectation: the picker appears and only the coded values for this field are visible as selectable rows
     * tap the "First" option
     * tap the "Done" button
     * Expectation: the value is visible and equal to "First"
     */
    @Test
    fun testRequiredValueWithComboBoxPicker() {

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


/**
 * Returns the child node with the given content description [value]. This only checks for the
 * children with a depth of 1. An exception is thrown if the child with the content description
 * does not exist.
 */
internal fun SemanticsNodeInteraction.onChildWithContentDescription(value: String): SemanticsNodeInteraction {
    val nodes = onChildren()
    val count = nodes.fetchSemanticsNodes().count()

    for (i in 0 until count) {
        val semanticsNode = nodes[i].fetchSemanticsNode()
        if (semanticsNode.config[SemanticsProperties.ContentDescription].contains(value)) {
            return nodes[i]
        }
    }
    throw AssertionError("No node exists with the given content description : $value")
}
