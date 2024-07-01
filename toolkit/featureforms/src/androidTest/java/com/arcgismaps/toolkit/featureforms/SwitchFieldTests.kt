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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SwitchFieldTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=ff98f13b32b349adb55da5528d9174dc",
    objectId = 1
) {

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
     * Test case 5.1:
     * Given a SwitchField type with a pre-existing "on" value
     * When the switch is tapped
     * Then the switch toggles to an "off" state.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-51-test-switch-on
     */
    @Test
    fun testSwitchIsOn() {
        // get the switch form element
        val switchElement = featureForm.getFieldFormElementWithLabel("switch integer")
            ?: return fail("element not found")
        // find the field with the the label
        val switchField = composeTestRule.onNodeWithText(switchElement.label)
        // assert it is displayed and not focused
        switchField.assertIsDisplayed()
        switchField.assertIsNotFocused()
        // find the switch field
        val switch = switchField.onChildWithContentDescription("switch", recurse = true)
        switch.assertIsDisplayed()
        // assert that the switch is on
        switch.assertIsOn()
        // assert the value displayed is the current "on" value
        switchField.assertEditableTextEquals(switchElement.formattedValue)
        // tap on the switch
        switchField.performClick()
        // assert that the switch is on
        switch.assertIsOff()
        // assert the value displayed is the current "off" value
        switchField.assertEditableTextEquals(switchElement.formattedValue)
    }

    /**
     * Test case 5.2:
     * Given a SwitchField type with a pre-existing "off" value
     * When the switch is tapped
     * Then the switch toggles to an "on" state.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-52-test-switch-off
     */
    @Test
    fun testSwitchIsOff() {
        // get the switch form element
        val switchElement = featureForm.getFieldFormElementWithLabel("switch string")
            ?: return fail("element not found")
        // find the field with the the label
        val switchField = composeTestRule.onNodeWithText(switchElement.label)
        // assert it is displayed and not focused
        switchField.assertIsDisplayed()
        switchField.assertIsNotFocused()
        // find the switch control
        val switch = switchField.onChildWithContentDescription("switch", recurse = true)
        switch.assertIsDisplayed()
        // assert that the switch is off
        switch.assertIsOff()
        // assert the value displayed is the current "off" value
        switchField.assertEditableTextEquals(switchElement.formattedValue)
        // tap on the switch
        switchField.performClick()
        // assert that the switch is on
        switch.assertIsOn()
        // assert the value displayed is the current "on" value
        switchField.assertEditableTextEquals(switchElement.formattedValue)
    }

    /**
     * Test case 5.3:
     * Given a FieldFormElement with a SwitchFormInput type and no pre-existing value
     * When the FeatureForm is displayed
     * Then the FieldFormElement is displayed as a ComboBox.
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-53-test-switch-with-no-value
     */
    @Test
    fun testSwitchWithNoValue() {
        // get the switch form element
        val switchElement = featureForm.getFieldFormElementWithLabel("switch double")
            ?: return fail("element not found")
        // find the field with the the label
        val comboBoxField = composeTestRule.onNodeWithText(switchElement.label)
        // assert it is displayed and not focused
        comboBoxField.assertIsDisplayed()
        comboBoxField.assertIsNotFocused()
        // assert that this field does not have any switch control
        comboBoxField.assert(!hasAnyChild(hasContentDescription("switch")))
        // assert a "no value" placeholder is visible
        comboBoxField.assertTextEquals(
            switchElement.label,
            context.getString(R.string.no_value)
        )
        // validate that the options icon is visible
        // since combo box fields have an icon and switch fields do not
        comboBoxField.assertContentDescriptionContains("field icon").assertIsDisplayed()
    }
}

/**
 * Returns a [FieldFormElement] with the given [label] if it exists. Else a null is returned.
 */
internal fun FeatureForm.getFieldFormElementWithLabel(label: String): FieldFormElement? =
    elements.find {
        it is FieldFormElement && it.label == label
    } as? FieldFormElement

