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
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test

class UtilityNetworkNavigationTests : FeatureFormTestRunner(
    uri = "https://rt-server114.esri.com/portal/home/item.html?id=f997acc3f5894008b583307d55e1ae4e",
    objectId = 10000000008,
    user = BuildConfig.unTestUser,
    password = BuildConfig.unTestPassword,
    layerName = "Structure Boundary"
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Given a FeatureForm with a UtilityAssociationsFormElement
     * When the navigation is enabled
     * Then the user should be able to navigate to a new form if there are no edits made
     * And return to the original form
     */
    @Test
    fun testNavigationWithoutEdits() {
        val state = FeatureFormState(
            featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(
                featureFormState = state
            )
        }
        val element = featureForm.elements.first() as? UtilityAssociationsFormElement
        assertThat(element).isNotNull()
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            element!!.associationsFilterResults.isNotEmpty()
        }
        val filter = element!!.associationsFilterResults.first().filter
        val groupResult = element.associationsFilterResults.first().groupResults.first()
        // Check that the filter, group, and association results are displayed
        val filterNode = composeTestRule.onNodeWithText(text = filter.title)
        filterNode.assertIsDisplayed()
        filterNode.performClick()
        val groupNode = composeTestRule.onNodeWithText(text = groupResult.name)
        groupNode.assertIsDisplayed()
        groupNode.performClick()
        val associationNode =
            composeTestRule.onNode(hasTextExactly("MediumVoltage", "Content"))
        associationNode.assertIsDisplayed()
        // Navigate to a new Form
        associationNode.performClick()
        // Wait for the new form to load
        composeTestRule.waitUntil {
            state.activeFeatureForm != featureForm
        }
        // Check that the State object has the new form
        assertThat(state.activeFeatureForm).isNotEqualTo(featureForm)
        val newForm = state.activeFeatureForm
        val newFormTitleNode = composeTestRule.onNodeWithText(text = newForm.title.value)
        newFormTitleNode.assertIsDisplayed()
        // Navigate back to the original form
        Espresso.pressBack()
        // Wait for the original form to load
        composeTestRule.waitUntil {
            state.activeFeatureForm == featureForm
        }
        // Check that the State object has the original form
        assertThat(state.activeFeatureForm).isEqualTo(featureForm)
        // The association node should be displayed again
        associationNode.assertIsDisplayed()
    }

    /**
     * Given a FeatureForm with a UtilityAssociationsFormElement
     * When the navigation is disabled
     * Then the user should not be able to navigate to a new form
     */
    @Test
    fun testNavigationIsDisabled() {
        val state = FeatureFormState(
            featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(
                featureFormState = state,
                isNavigationEnabled = false
            )
        }
        val element = featureForm.elements.first() as? UtilityAssociationsFormElement
        assertThat(element).isNotNull()
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            element!!.associationsFilterResults.isNotEmpty()
        }
        val filter = element!!.associationsFilterResults.first().filter
        val groupResult = element.associationsFilterResults.first().groupResults.first()
        // Check that the filter, group, and association results are displayed
        val filterNode = composeTestRule.onNodeWithText(text = filter.title)
        filterNode.assertIsDisplayed()
        filterNode.performClick()
        val groupNode = composeTestRule.onNodeWithText(text = groupResult.name)
        groupNode.assertIsDisplayed()
        groupNode.performClick()
        val associationNode =
            composeTestRule.onNode(hasTextExactly("MediumVoltage", "Content"))
        associationNode.assertIsDisplayed()
        // Navigate to a new Form
        associationNode.performClick()
        // Check that the navigation is disabled
        assertThat(state.activeFeatureForm).isEqualTo(featureForm)
    }
}
