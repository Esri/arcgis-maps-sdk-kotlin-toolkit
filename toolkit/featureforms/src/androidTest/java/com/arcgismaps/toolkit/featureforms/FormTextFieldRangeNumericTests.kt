/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextFieldState
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests for FormTextFields whose backing FormFeatureElement is associated with a numeric field and attribute type.
 */
class FormTextFieldRangeNumericTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=c6a4789e5dd64bc48941d8478c5940d2",
    objectId = 1
) {
    private val supportingTextSemanticLabel = "supporting text"
    private val outlinedTextFieldSemanticLabel = "outlined text field"

    private val integerField by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "ForRange"
            }
    }


    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given a FormTextField with a FormFeatureElement whose backing fieldType is a floating point type.
     * When a non numeric value is entered
     * Then the label is displayed with the expected error text
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/InputValidationDesign.md#text
     */
    @Test
    fun testEnterNumericValueOutOfRange() = runTest {
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val state = rememberFieldState(
                element = integerField,
                form = featureForm,
                scope = scope
            ) as FormTextFieldState
            FormTextField(state = state)
        }
        val outlinedTextField =
            composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = "9"
        outlinedTextField.performTextInput(text)
        val supportingText = composeTestRule.onNode(
            hasContentDescription(supportingTextSemanticLabel),
            useUnmergedTree = true
        )
        supportingText.assertIsDisplayed()
        TestCase.assertEquals("Enter value from 1 to 7", supportingText.getTextString())
    }
}
