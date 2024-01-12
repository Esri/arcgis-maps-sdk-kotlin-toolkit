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
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import com.arcgismaps.toolkit.featureforms.utils.valueFlow
import junit.framework.TestCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/**
 * Tests for FormTextFields whose backing FormFeatureElement is associated with a numeric field and attribute type.
 */
class FormTextFieldRangeNumericTests {
    private val helperSemanticLabel = "helper"
    private val outlinedTextFieldSemanticLabel = "outlined text field"
    
    private val featureForm by lazy {
        sharedFeatureForm!!
    }
    
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
            val textFieldProperties = TextFieldProperties(
                label = integerField.label,
                placeholder = integerField.hint,
                description = integerField.description,
                value = integerField.valueFlow(scope),
                editable = integerField.isEditable,
                required = integerField.isRequired,
                singleLine = integerField.input is TextBoxFormInput,
                domain = integerField.domain,
                fieldType = featureForm.fieldType(integerField),
                minLength = (integerField.input as TextBoxFormInput).minLength.toInt(),
                maxLength = (integerField.input as TextBoxFormInput).maxLength.toInt(),
                visible = integerField.isVisible
            )
            FormTextField(
                state = FormTextFieldState(
                    textFieldProperties,
                    scope = scope,
                    onEditValue = {
                        featureForm.editValue(integerField, it)
                        scope.launch { featureForm.evaluateExpressions() }
                    },
                    defaultValidator = {integerField.getValidationErrors()}
                )
            )
        }
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        val text = "9"
        outlinedTextField.performTextInput(text)
        val helper = composeTestRule.onNode(hasContentDescription(helperSemanticLabel), useUnmergedTree = true)
        val helperText = helper.getTextString()
        helper.assertIsDisplayed()
        TestCase.assertEquals("Enter value from 1 to 7", helperText)
    }
    
    companion object {
        var sharedFeatureFormDefinition: FeatureFormDefinition? = null
        var sharedFeatureForm: FeatureForm? = null
        var sharedFeature: ArcGISFeature? = null
        var sharedMap: ArcGISMap? = null
        
        @BeforeClass
        @JvmStatic
        fun setupClass() = runTest {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
                FeatureFormsTestChallengeHandler(
                    BuildConfig.webMapUser,
                    BuildConfig.webMapPassword
                )
            
            sharedMap =
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=225ffaf1091d48fcbe31be0146808b2b")
            sharedMap?.load()?.onFailure { TestCase.fail("failed to load webmap with ${it.message}") }
            val featureLayer = sharedMap?.operationalLayers?.first() as? FeatureLayer
            featureLayer?.let { layer ->
                layer.load().onFailure { TestCase.fail("failed to load layer with ${it.message}") }
                sharedFeatureFormDefinition = layer.featureFormDefinition!!
                val parameters = QueryParameters().also {
                    it.whereClause = "1=1"
                    it.maxFeatures = 1
                }
                layer.featureTable?.queryFeatures(parameters)?.onSuccess {
                    sharedFeature = it.filterIsInstance<ArcGISFeature>().first()
                    sharedFeature?.load()?.onFailure { TestCase.fail("failed to load feature with ${it.message}") }
                    sharedFeatureForm = FeatureForm(sharedFeature!!, sharedFeatureFormDefinition!!)
                    sharedFeatureForm?.evaluateExpressions()
                }?.onFailure {
                    TestCase.fail("failed to query features on layer's featuretable with ${it.message}")
                }
            }
        }
    }
    
}