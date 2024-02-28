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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    private val descriptionSemanticLabel = "supporting text"
    private val clearTextSemanticLabel = "Clear text button"
    private val optionsIconSemanticLabel = "field icon"
    private val comboBoxDialogListSemanticLabel = "ComboBoxDialogLazyColumn"
    private val comboBoxDialogDoneButtonSemanticLabel = "combo box done selection"
    private val noValueRowSemanticLabel = "no value row"
    private lateinit var context: Context

    private val featureForm by lazy {
        sharedFeatureForm!!
    }

    private var errorTextColor : Color? = null

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
        featureForm.discardEdits()
        composeTestRule.setContent {
            errorTextColor = MaterialTheme.colorScheme.error
            FeatureForm(featureForm = featureForm)
        }
    }

    /**
     * Test case 3.1:
     * Given a ComboBoxField with a pre-existing value, description and a no value label
     * When the pre-existing value is cleared
     * Then the ComboBoxField shows the noValueLabel
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-31-pre-existing-value-description-clear-button-no-value-label
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
        comboBoxField.assertTextEquals(formElement.label, formElement.formattedValue)
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
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-32-no-pre-existing-value-no-value-label-options-button
     */
    @Test
    fun testNoValueAndNoValueLabel() {
        val formElement = getFormElementWithLabel("Combo Integer")
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
        // assert "no value" placeholder is visible
        // assertTextEquals matches the Text(the label) and Editable Text (the placeholder here
        // due to the use of the PlaceHolderTransformation)
        comboBoxField.assertTextEquals(
            formElement.label,
            input.noValueLabel.ifEmpty { context.getString(R.string.no_value) }
        )
        // validate that the options icon is visible
        val optionsIconNode =
            comboBoxField.assertContentDescriptionContains(optionsIconSemanticLabel)
        optionsIconNode.assertIsDisplayed()
    }

    /**
     * Test case 3.3:
     * Given a ComboBoxField with a pre-existing value, description and a no value label
     * When the ComboBoxField is tapped
     * Then a ComboBoxDialog is shown AND
     * When a coded value is selected from the dialog and dismissed
     * Then the ComboBoxField also shows this selected value
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-33-pick-a-value
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
        comboBoxField.assertTextEquals(formElement.label, formElement.formattedValue)
        // tap the value to bring up the picker
        comboBoxField.performClick()
        // find the dialog
        val comboBoxDialogList =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        comboBoxDialogList.assertIsDisplayed()
        val codedValueToSelect = input.codedValues.first().name
        // find the first list item and tap on it
        val listItem =
            comboBoxDialogList.onChildWithContentDescription("$codedValueToSelect list item")
        listItem.assertIsDisplayed()
        listItem.performClick()
        // find and tap the done button
        val doneButton =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogDoneButtonSemanticLabel)
        doneButton.performClick()
        // validate the selection has changed
        comboBoxField.assertTextEquals(formElement.label, codedValueToSelect)
    }

    /**
     * Test case 3.4:
     * Given a ComboBoxField with a pre-existing value, description and a no value label
     * When the ComboBoxField is tapped
     * Then the ComboBoxDialog is shown and a noValueLabel row is visible and selectable
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-34-picker-with-a-novaluelabel-row
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
        comboBoxField.assertTextEquals(formElement.label, formElement.formattedValue)
        // open the picker
        comboBoxField.performClick()
        // find the dialog
        val comboBoxDialogList =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        comboBoxDialogList.assertIsDisplayed()
        val noValueLabel = input.noValueLabel.ifEmpty { context.getString(R.string.no_value) }
        // this field has a no value label and not required, hence check for the row
        val noValueRow = comboBoxDialogList.onChildWithContentDescription(
            noValueRowSemanticLabel
        ).assertIsDisplayed()
        // select the no value row
        noValueRow.performClick()
        // find and tap the done button
        val doneButton =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogDoneButtonSemanticLabel)
        doneButton.performClick()
        // validate the selection has changed
        comboBoxField.assertTextEquals(formElement.label, noValueLabel)
    }

    /**
     * Test case 3.5:
     * Given a ComboBoxField with a pre-existing value, description and is required
     * When the ComboBoxField value is cleared
     * Then the helper text is visible, has the platform default error color and says "Required" AND
     * When the ComboBoxField is tapped
     * Then a noValueLabel row is not present
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-35-required-value
     */
    @Test
    fun testRequiredValueWithComboBoxPicker() {
        val formElement = getFormElementWithLabel("Required Combo Box")
        val input = formElement.input as ComboBoxFormInput
        val requiredLabel = "${formElement.label} *"
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(requiredLabel)
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // validate that the pre-populated value shown shown in accurate and as expected
        // assertTextEquals matches the Text(the label) and Editable Text (the actual editable input text)
        comboBoxField.assertTextEquals(requiredLabel, formElement.formattedValue)
        // set the value to null
        formElement.updateValue(null)
        // click on the element to open the dialog picker
        comboBoxField.performClick()
        // find and tap the done button
        val doneButton =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogDoneButtonSemanticLabel).performClick()
        // assert "Enter Value" placeholder is visible
        comboBoxField.assertTextEquals(requiredLabel, context.getString(R.string.enter_value))
        // validate required text is visible and is in error color
        comboBoxField.onChildWithText(context.getString(R.string.required)).assertTextColor(errorTextColor!!)
        // open the picker again
        comboBoxField.performClick()
        // find the dialog
        val comboBoxDialogList =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        comboBoxDialogList.assertIsDisplayed()
        // validate a noValueLabel row is not present
        assert(
            composeTestRule.onAllNodesWithContentDescription(noValueRowSemanticLabel)
                .fetchSemanticsNodes().isEmpty()
        )
        val codedValueToSelect = input.codedValues.first().name
        // find the first list item and tap on it
        val listItem =
            comboBoxDialogList.onChildWithContentDescription("$codedValueToSelect list item")
        listItem.assertIsDisplayed()
        listItem.performClick()
        doneButton.performClick()
        // validate the selection has changed
        comboBoxField.assertTextEquals(requiredLabel, codedValueToSelect)
    }

    /**
     * Test case 3.6:
     * Given a ComboBoxField with a pre-existing value, description and showNoValueOption is Hide
     * When the ComboBoxField is tapped
     * Then a noValueLabel row is not present
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-36-novalueoption-is-hide
     */
    @Test
    fun testNoValueOptionHidden() {
        val formElement = getFormElementWithLabel("Combo No Value False")
        val input = formElement.input as ComboBoxFormInput
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(formElement.label)
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // validate that the pre-populated value shown shown in accurate and as expected and that
        // no placeholder is visible. blank space is used here due to PlaceHolderTransformation
        comboBoxField.assertTextEquals(formElement.label, " ")

        // open the picker
        comboBoxField.performClick()
        // find the dialog
        val comboBoxDialogList =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        comboBoxDialogList.assertIsDisplayed()
        // validate a noValueLabel row is not displayed
        assert(
            composeTestRule.onAllNodesWithContentDescription(noValueRowSemanticLabel)
                .fetchSemanticsNodes().isEmpty()
        )
        val codedValueToSelect = input.codedValues.first().name
        // find the first list item and tap on it
        val listItem =
            comboBoxDialogList.onChildWithContentDescription("$codedValueToSelect list item")
        listItem.assertIsDisplayed()
        listItem.performClick()
        // find and tap the done button
        val doneButton =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogDoneButtonSemanticLabel)
        doneButton.performClick()
        // validate the selection has changed
        comboBoxField.assertTextEquals(formElement.label, codedValueToSelect)
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
