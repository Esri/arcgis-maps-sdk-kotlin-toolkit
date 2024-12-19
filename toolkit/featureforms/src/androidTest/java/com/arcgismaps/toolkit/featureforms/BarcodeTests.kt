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

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.test.rule.GrantPermissionRule
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class BarcodeTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=a14a825c22884dfe9998ac964bd1cf89",
    objectId = 2L
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Grant camera permission for barcode scanning
    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    /**
     * Test case 11.1:
     * Given a `FeatureForm` with a `BarcodeScannerFormInput`
     * When the `FeatureForm` is displayed
     * Then the barcode form element is displayed with the scan icon
     * And displays the helper text when focused
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-barcode-input-type
     */
    @Test
    fun testBarcodeTextField(): Unit = runBlocking {
        composeTestRule.setContent {
            FeatureForm(featureForm = featureForm)
        }
        val barcodeFormElement = composeTestRule.onNodeWithText("Barcode")
        // Check the barcode form element is displayed
        barcodeFormElement.assertIsDisplayed()
        // Check the scan icon is displayed
        barcodeFormElement.onChildWithContentDescription("scan barcode").assertIsDisplayed()
        // Perform text input
        barcodeFormElement.performTextInput("https://esri.com")
        barcodeFormElement.requestFocus()
        barcodeFormElement.assertIsFocused()
        // verify helper text is displayed
        barcodeFormElement.onChildWithText("Maximum 50 characters").assertIsDisplayed()
    }

    /**
     * Given a `FeatureForm` with `FieldFormElement` and a custom barcode click action
     * When the scan icon on a barcode form element is clicked
     * Then the custom barcode click action is triggered
     * And the correct `FieldFormElement` is passed to the custom barcode click action
     */
    @Test
    fun testCustomBarcodeClickAction() = runTest {
        var fieldFormElement: FieldFormElement? = null
        composeTestRule.setContent {
            FeatureForm(
                featureForm = featureForm,
                onBarcodeButtonClick = {
                    // Custom barcode click event
                    fieldFormElement = it
                }
            )
        }
        val firstBarcodeElement = composeTestRule.onNodeWithText("Barcode")
        // Check the barcode form element is displayed
        firstBarcodeElement.assertIsDisplayed()
        // Check the scan icon is displayed
        val firstScanIcon = firstBarcodeElement.onChildWithContentDescription("scan barcode")
        firstScanIcon.assertIsDisplayed()
        firstScanIcon.performClick()
        // Check the custom barcode click action is triggered
        assertThat(fieldFormElement).isNotNull()
        assertThat(fieldFormElement!!.label).isEqualTo("Barcode")
        assertThat(fieldFormElement!!.input).isInstanceOf(BarcodeScannerFormInput::class.java)
        val barcodeScannerFormInput = fieldFormElement!!.input as BarcodeScannerFormInput
        assertThat(barcodeScannerFormInput.maxLength).isEqualTo(50)
        // set a value to the barcode field
        var barcodeValue = "0123456789"
        fieldFormElement!!.updateValue(barcodeValue)
        firstBarcodeElement.assertEditableTextEquals(barcodeValue)

        val secondBarcodeElement = composeTestRule.onNodeWithText("Model Number")
        // Check the barcode form element is displayed
        secondBarcodeElement.assertIsDisplayed()
        // Check the scan icon is displayed
        val secondScanIcon = secondBarcodeElement.onChildWithContentDescription("scan barcode")
        secondScanIcon.assertIsDisplayed()
        secondScanIcon.performClick()
        // Check the custom barcode click action is triggered
        assertThat(fieldFormElement).isNotNull()
        assertThat(fieldFormElement!!.label).isEqualTo("Model Number")
        assertThat(fieldFormElement!!.input).isInstanceOf(BarcodeScannerFormInput::class.java)
        val secondBarcodeScannerFormInput = fieldFormElement!!.input as BarcodeScannerFormInput
        assertThat(secondBarcodeScannerFormInput.maxLength).isEqualTo(10)
        // set a value to the barcode field
        barcodeValue = "9876543210"
        fieldFormElement!!.updateValue(barcodeValue)
        secondBarcodeElement.assertEditableTextEquals(barcodeValue)
    }

    /**
     * Given a `FeatureForm` with a `FieldFormElement` in a `GroupFormElement` and a custom barcode click action
     * When the scan icon on a barcode form element is clicked
     * Then the custom barcode click action is triggered
     * And the correct `FieldFormElement` is passed to the custom barcode click action
     */
    @Test
    fun testGroupCustomBarcodeClickAction() = runTest {
        var fieldFormElement: FieldFormElement? = null
        composeTestRule.setContent {
            FeatureForm(
                featureForm = featureForm,
                onBarcodeButtonClick = {
                    // Custom barcode click event
                    fieldFormElement = it
                }
            )
        }
        val barcodeElement = composeTestRule.onNodeWithText("Barcode in Group")
        // Check the barcode form element is displayed
        barcodeElement.assertIsDisplayed()
        // Check the scan icon is displayed
        val scanIcon = barcodeElement.onChildWithContentDescription("scan barcode")
        scanIcon.assertIsDisplayed()
        scanIcon.performClick()
        // Check the custom barcode click action is triggered
        assertThat(fieldFormElement).isNotNull()
        assertThat(fieldFormElement!!.label).isEqualTo("Barcode in Group")
        assertThat(fieldFormElement!!.input).isInstanceOf(BarcodeScannerFormInput::class.java)
        val barcodeScannerFormInput = fieldFormElement!!.input as BarcodeScannerFormInput
        assertThat(barcodeScannerFormInput.maxLength).isEqualTo(25)
        assertThat(barcodeScannerFormInput.minLength).isEqualTo(10)
        val barcodeValue = "0123456789"
        fieldFormElement!!.updateValue(barcodeValue)
        barcodeElement.assertEditableTextEquals(barcodeValue)
    }

    /**
     * Given a `FeatureForm` with `FieldFormElement` and no custom barcode click action
     * When the scan icon on a barcode form element is clicked
     * Then the default barcode click action is triggered
     */
    @Test
    fun testDefaultBarcodeClickAction() = runTest {
        composeTestRule.setContent {
            FeatureForm(
                featureForm = featureForm,
                onBarcodeButtonClick = null
            )
        }
        val barcodeFormElement = composeTestRule.onNodeWithText("Barcode")
        // Check the barcode form element is displayed
        barcodeFormElement.assertIsDisplayed()
        // Check the scan icon is displayed
        val scanIcon = barcodeFormElement.onChildWithContentDescription("scan barcode")
        scanIcon.assertIsDisplayed()
        scanIcon.performClick()
        // Check the default barcode scanner is displayed
        val scanner = composeTestRule.onNodeWithContentDescription("MLKit Barcode Scanner")
        scanner.assertIsDisplayed()
    }

    /**
     * Given a `FeatureForm` with a `FieldFormElement` in a `GroupFormElement` and no custom barcode click action
     * When the scan icon on a barcode form element is clicked
     * Then the default barcode click action is triggered
     */
    @Test
    fun testDefaultBarcodeClickActionInGroup() = runTest {
        composeTestRule.setContent {
            FeatureForm(
                featureForm = featureForm,
                onBarcodeButtonClick = null
            )
        }
        val barcodeElement = composeTestRule.onNodeWithText("Barcode in Group")
        // Check the barcode form element is displayed
        barcodeElement.assertIsDisplayed()
        // Check the scan icon is displayed
        val scanIcon = barcodeElement.onChildWithContentDescription("scan barcode")
        scanIcon.assertIsDisplayed()
        scanIcon.performClick()
        // Check the default barcode scanner is displayed
        val scanner = composeTestRule.onNodeWithContentDescription("MLKit Barcode Scanner")
        scanner.assertIsDisplayed()
    }
}
