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
class FormTextFieldNumericTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=ba55368fb465488b82076aec4077ec70",
    objectId = 1
) {
    private val supportingTextSemanticLabel = "supporting text"
    private val outlinedTextFieldSemanticLabel = "outlined text field"

    private val integerField by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "Number Integer"
            }
    }

    private val floatingPointField by lazy {
        featureForm.elements
            .filterIsInstance<FieldFormElement>()
            .first {
                it.label == "Number Double"
            }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given a FormTextField with a FormFeatureElement whose backing fieldType is an integer type.
     * When a non numeric value is entered
     * Then the label is displayed with the expected error text
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/InputValidationDesign.md#text
     */
    @Test
    fun testEnterNonNumericValueIntegerField() = runTest {
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
        val text = "lorem ipsum"
        outlinedTextField.performTextInput(text)
        val supportingText = composeTestRule.onNode(
            hasContentDescription(supportingTextSemanticLabel),
            useUnmergedTree = true
        )
        supportingText.assertIsDisplayed()
        TestCase.assertEquals("Value must be a whole number", supportingText.getTextString())
    }

    /**
     * Given a FormTextField with a FormFeatureElement whose backing fieldType is a floating point type.
     * When a non numeric value is entered
     * Then the label is displayed with the expected error text
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/InputValidationDesign.md#text
     */
    @Test
    fun testEnterNonNumericValueFloatingPointField() = runTest {
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val state = rememberFieldState(
                element = floatingPointField,
                form = featureForm,
                scope = scope
            ) as FormTextFieldState
            FormTextField(state = state)
        }
        val outlinedTextField =
            composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = "lorem ipsum"
        outlinedTextField.performTextInput(text)
        val supportingText = composeTestRule.onNode(
            hasContentDescription(supportingTextSemanticLabel),
            useUnmergedTree = true
        )
        supportingText.assertIsDisplayed()
        TestCase.assertEquals("Value must be a number", supportingText.getTextString())
    }
}
