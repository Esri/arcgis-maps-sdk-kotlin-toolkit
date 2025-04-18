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

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.internal.components.base.formattedValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValidationErrors
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFieldProperties
import junit.framework.TestCase
import kotlinx.coroutines.launch
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
            val state = remember {
                FormTextFieldState(
                    id = integerField.hashCode(),
                    properties = TextFieldProperties(
                        label = integerField.label,
                        placeholder = integerField.hint,
                        description = integerField.description,
                        value = integerField.formattedValueAsStateFlow(scope),
                        validationErrors = integerField.mapValidationErrors(scope),
                        editable = integerField.isEditable,
                        required = integerField.isRequired,
                        visible = integerField.isVisible,
                        singleLine = integerField.input is TextBoxFormInput,
                        fieldType = integerField.fieldType,
                        domain = integerField.domain as? RangeDomain,
                        minLength = (integerField.input as TextBoxFormInput).minLength.toInt(),
                        maxLength = (integerField.input as TextBoxFormInput).maxLength.toInt()
                    ),
                    hasValueExpression = integerField.hasValueExpression,
                    scope = scope,
                    updateValue = integerField::updateValue,
                    evaluateExpressions = {
                        featureForm.evaluateExpressions()
                    },
                )
            }
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
            val state = remember {
                FormTextFieldState(
                    id = floatingPointField.hashCode(),
                    properties = TextFieldProperties(
                        label = floatingPointField.label,
                        placeholder = floatingPointField.hint,
                        description = floatingPointField.description,
                        value = floatingPointField.formattedValueAsStateFlow(scope),
                        validationErrors = floatingPointField.mapValidationErrors(scope),
                        editable = floatingPointField.isEditable,
                        required = floatingPointField.isRequired,
                        visible = floatingPointField.isVisible,
                        singleLine = floatingPointField.input is TextBoxFormInput,
                        fieldType = floatingPointField.fieldType,
                        domain = floatingPointField.domain as? RangeDomain,
                        minLength = (floatingPointField.input as TextBoxFormInput).minLength.toInt(),
                        maxLength = (floatingPointField.input as TextBoxFormInput).maxLength.toInt()
                    ),
                    hasValueExpression = floatingPointField.hasValueExpression,
                    scope = scope,
                    updateValue = floatingPointField::updateValue,
                    evaluateExpressions = {
                        featureForm.evaluateExpressions()
                    },
                )
            }
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
