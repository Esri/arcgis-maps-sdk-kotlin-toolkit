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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class BarcodeTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=a14a825c22884dfe9998ac964bd1cf89",
    objectId = 2L
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test case 11.1:
     * Given a `FeatureForm` with a `BarcodeScannerFormInput`
     * When the `FeatureForm` is displayed
     * Then the barcode form element is displayed with the scan and clear icons
     * And receives length validation errors when the input length is out of range
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-barcode-input-type
     */
    @Test
    fun testBarcodeTextField() = runTest {
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
        // Check the clear icon is displayed
        barcodeFormElement.onChildWithContentDescription("clear text").assertIsDisplayed()
        // Check the scan icon is displayed
        barcodeFormElement.onChildWithContentDescription("scan barcode").assertIsDisplayed()
        // Perform text input
        barcodeFormElement.performTextInput("https://runtimecoretest.maps.arcgis.com/apps/mapviewer/index.html?layers=a9155494098147b9be2fc52bcf825224")
        // Check for validation error
        barcodeFormElement.onChildWithText("Maximum 50 characters").assertIsDisplayed()
    }

    @Test
    fun testCustomBarcodeClickEvent() = runTest {
        var fieldFormElement : FieldFormElement? = null
        composeTestRule.setContent {
            FeatureForm(
                featureForm = featureForm,
                onBarcodeAccessoryClicked = {
                    // Custom barcode click event
                    fieldFormElement = it
                }
            )
        }
        val barcodeFormElement = composeTestRule.onNodeWithText("Barcode")
        // Check the barcode form element is displayed
        barcodeFormElement.assertIsDisplayed()
        // Check the scan icon is displayed
        val scanIcon = barcodeFormElement.onChildWithContentDescription("scan barcode")
        scanIcon.assertIsDisplayed()
        scanIcon.performClick()
        assertThat(fieldFormElement).isNotNull()
    }
}
