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

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class BarcodeTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=a14a825c22884dfe9998ac964bd1cf89",
    objectId = 2L
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBarcodeTextField(): Unit = runBlocking {
        composeTestRule.setContent {
            FeatureForm(featureForm = featureForm)
        }
        composeTestRule.onRoot().printToLog("BarcodeTests")
        delay(5000)
        val barcodeFormElement = composeTestRule.onNodeWithText("Barcode")
        barcodeFormElement.assertIsDisplayed()
        // Check only the scan icon is displayed
        barcodeFormElement.assertContentDescriptionEquals("scan barcode")
        // Perform text input
        barcodeFormElement.performTextInput("https://esri.com")
        // Check the clear icon is displayed in addition to the scan icon
        barcodeFormElement.assertContentDescriptionEquals("clear text", "scan barcode")
        // Perform text input
        barcodeFormElement.performTextInput("https://runtimecoretest.maps.arcgis.com/apps/mapviewer/index.html?layers=a9155494098147b9be2fc52bcf825224")
        // Check for validation error
    }
}
