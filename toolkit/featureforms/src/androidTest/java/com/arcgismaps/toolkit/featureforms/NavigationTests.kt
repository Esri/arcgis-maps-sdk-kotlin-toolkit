/*
 * Copyright 2025 Esri
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
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class NavigationTests : FeatureFormTestRunner(
    uri = "https://rt-server114.esri.com/portal/home/item.html?id=f997acc3f5894008b583307d55e1ae4e",
    objectId = 10000000025,
    user = BuildConfig.unTestUser,
    password = BuildConfig.unTestPassword,
    layerName = "Structure Junction"
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val scope = CoroutineScope(StandardTestDispatcher())

    @Test
    fun test(): Unit = runBlocking {
        val state = FeatureFormState(
            featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(
                featureFormState = state
            )
        }
        assertThat(featureForm.elements.first()).isInstanceOf(UtilityAssociationsFormElement::class.java)
        assertThat(featureForm.elements[1]).isInstanceOf(FieldFormElement::class.java)
//        delay(5000)
//        val filterNode = composeTestRule.onNodeWithText(text = "All Content")
//        filterNode.assertIsDisplayed()
    }
}
