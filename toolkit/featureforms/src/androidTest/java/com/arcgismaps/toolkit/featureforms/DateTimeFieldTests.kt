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

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.printToLog
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class DateTimeFieldTests {
    
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
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }
    
    @Before
    fun setContent() {
        composeTestRule.setContent {
            val state = FeatureFormState()
            state.setFeatureForm(featureForm)
            FeatureForm(
                featureFormState = state
            )
        }
    }
    
    /**
     * Given a required datetime field with a value
     * when it is not focused
     * the text values are as expected
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-21-unfocused-and-focused-state-no-value-date-required
     * (slightly modified because the field has a value and the table cannot be edited)
     */
    @Test
    fun testRequiredUnfocusedValue() {
        val formElement = getFormElementWithLabel("Required Date")
        // find the field with the the label
        val col = composeTestRule.onNodeWithContentDescription("lazy column")
        col.performScrollToIndex(8)
        val dateTimeField = composeTestRule.onNodeWithText("${formElement.label} *")
        val textMatcher = hasText("Nov 02, 2023 1:01 PM")
        assert(textMatcher.matches(dateTimeField.fetchSemanticsNode())) {
            "expected text Nov 02, 2023 1:01 PM"
        }
        dateTimeField.assertIsDisplayed()
        dateTimeField.performClick()
        val helper = dateTimeField.onChildWithContentDescription("supporting text")
        val helperMatcher = hasText("Date Entry is Required")
        assert(helperMatcher.matches(helper.fetchSemanticsNode())) {
            "expected helper text: Date Entry is Required"
        }
        val clearButton = dateTimeField.onChildWithContentDescription("Clear text button")
        clearButton.assertIsDisplayed()
    }
    
    /**
     * Given a required datetime field with a value
     * when it is not focused
     * the text values are as expected
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-21-unfocused-and-focused-state-no-value-date-required
     * (slightly modified because the field has a value and the table cannot be edited)
     */
    @Test
    fun testRequiredFocusedValue() {
        val formElement = getFormElementWithLabel("Required Date")
        // find the field with the the label
        val col = composeTestRule.onNodeWithContentDescription("lazy column")
        col.performScrollToIndex(8)
        val dateTimeField = composeTestRule.onNodeWithText("${formElement.label} *")
        val textMatcher = hasText("Nov 02, 2023 1:01 PM")
        assert(textMatcher.matches(dateTimeField.fetchSemanticsNode())) {
            "expected text Nov 02, 2023 1:01 PM"
        }
        dateTimeField.assertIsDisplayed()
        
        val helper = dateTimeField.onChildWithContentDescription("supporting text")
        val helperMatcher = hasText("Date Entry is Required")
        assert(helperMatcher.matches(helper.fetchSemanticsNode())) {
          "expected helper text: Date Entry is Required"
        }
        val clearButton = dateTimeField.onChildWithContentDescription("Clear text button")
        clearButton.assertIsDisplayed()
    
        dateTimeField.performClick()
        dateTimeField.assertIsFocused()
    
        val dialogSurface =
            composeTestRule.onNodeWithContentDescription("DateTimePickerDialogSurface", useUnmergedTree = true)
        dialogSurface.assertIsDisplayed()
        dialogSurface.printToLog("TAGGGG", maxDepth = 100000)
        val today = dialogSurface.onChildWithContentDescription("current date or time button")
        today.assertIsDisplayed()
        val helperTextInDialog = dialogSurface.onChildWithText("Date Entry is Required", true)
        helperTextInDialog.assertIsDisplayed()
    }
    
    companion object {
        var sharedFeatureFormDefinition: FeatureFormDefinition? = null
        var sharedFeatureForm: FeatureForm? = null
        var sharedFeature: ArcGISFeature? = null
        var sharedMap: ArcGISMap? = null
        
        @BeforeClass
        @JvmStatic
        fun setupClass() = runTest {
            sharedMap =
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=ec09090060664cbda8d814e017337837")
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
