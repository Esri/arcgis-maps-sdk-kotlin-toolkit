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

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UtilityAssociationsFormElementTests {

    private lateinit var map: ArcGISMap

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val comboBoxDialogListSemanticLabel = "ComboBoxDialogLazyColumn"
    private val comboBoxDialogDoneButtonSemanticLabel = "combo box dialog close button"

    init {
        // Set the authentication challenge handler
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            FeatureFormsTestChallengeHandler(
                username = BuildConfig.traceToolUser,
                password = BuildConfig.traceToolPassword,
            )
    }

    @Before
    fun setup() = runTest {
        if (::map.isInitialized.not()) {
            map = ArcGISMap(
                uri = "https://www.arcgis.com/home/item.html?id=a93ff75c66644c02bdb3a785cc0ba795"
            )
        }
        map.assertIsLoaded()
        map.utilityNetworks.forEach {
            it.assertIsLoaded()
        }
    }

    /**
     * Test case 12.1
     *
     * Given a `FeatureForm` with a `UtilityAssociationsFormElement`
     * When the `FeatureForm` is displayed
     * Then the associations are displayed with the correct terminal information
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-121-associations-show-terminal
     */
    @Test
    fun testTerminalIsDisplayed() = runTest {
        val groupLayer = map.operationalLayers.first()
        val layer = groupLayer.subLayerContents.value.find {
            it.name == "Electric Distribution Device"
        } as FeatureLayer?
        assertThat(layer).isNotNull()

        val queryResult = layer!!.featureTable!!.queryFeatures(
            QueryParameters().apply {
                objectIds.add(5050)
            }
        ).getOrNull()
        assertThat(queryResult).isNotNull()

        val feature = queryResult!!.firstOrNull() as? ArcGISFeature
        assertThat(feature).isNotNull()

        val featureForm = FeatureForm(feature!!)
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumnNode.performScrollToNode(hasText("Associations")).assertIsDisplayed()

        // Verify the association filters are displayed
        val connectedNode = composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Structure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Structure").assertIsDisplayed()

        // Click on the connected filter
        connectedNode.performClick()

        // Verify the groups are displayed
        var listView = composeTestRule.onNode(hasScrollAction())
        listView.onChildWithText("Electric Distribution Junction").assertIsDisplayed()
        val groupNode = listView.onChildWithText("Electric Distribution Device").assertIsDisplayed()
        groupNode.performClick()

        // Verify the associations are displayed
        listView = composeTestRule.onNode(hasScrollAction())
        val firstElement = listView.onChildWithText("Object ID : 3907").assertIsDisplayed()
        // Verify the terminal is displayed
        firstElement.assert(hasText("Terminal : Single Terminal"))
        val secondElement = listView.onChildWithText("Object ID : 1392").assertIsDisplayed()
        // Verify the terminal is displayed
        secondElement.assert(hasText("Terminal : Single Terminal"))
        // Click on the first association
        firstElement.performClick()
        // Verify the new feature form is displayed by checking for a form element
        composeTestRule.onNode(hasTextExactly("Asset group", "Fuse")).assertIsDisplayed()
    }

    /**
     * Test case 12.3
     *
     * Given a `FeatureForm` with a `UtilityAssociationsFormElement`
     * When the `FeatureForm` is displayed
     * Then the containment visibility is displayed for containment associations
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-123-containment-association-shows-containment-visibility
     */
    @Test
    fun testContainmentVisibilityIsDisplayed() = runTest {
        val groupLayer = map.operationalLayers.first()
        val layer = groupLayer.subLayerContents.value.find {
            it.name == "Structure Boundary"
        } as FeatureLayer?
        assertThat(layer).isNotNull()

        val queryResult = layer!!.featureTable!!.queryFeatures(
            QueryParameters().apply {
                objectIds.add(2)
            }
        ).getOrNull()
        assertThat(queryResult).isNotNull()

        val feature = queryResult!!.firstOrNull() as? ArcGISFeature
        assertThat(feature).isNotNull()

        val featureForm = FeatureForm(feature!!)
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumnNode.performScrollToNode(hasText("Associations")).assertIsDisplayed()

        val containmentNode = composeTestRule.onNodeWithText("Content").assertIsDisplayed()
        containmentNode.performClick()

        // Verify the groups are displayed
        var listView = composeTestRule.onNode(hasScrollAction())
        listView.onChildWithText("Electric Distribution Device").assertIsDisplayed().performClick()

        // Verify the associations are displayed
        listView = composeTestRule.onNode(hasScrollAction())
        listView.onChildWithText("Containment Visible : false").assertIsDisplayed()
    }

    /**
     * Test case 12.4
     *
     * Given a `FeatureForm` with a `UtilityAssociationsFormElement`
     * When the `FeatureForm` is displayed
     * Then the containment visibility is not displayed for container associations
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-124-containment-association-doesnt-show-containment-visibility
     */
    @Test
    fun testContainmentVisibilityIsNotDisplayed() = runTest {
        val groupLayer = map.operationalLayers.first()
        val layer = groupLayer.subLayerContents.value.find {
            it.name == "Electric Distribution Device"
        } as FeatureLayer?
        assertThat(layer).isNotNull()

        val queryResult = layer!!.featureTable!!.queryFeatures(
            QueryParameters().apply {
                objectIds.add(2584)
            }
        ).getOrNull()
        assertThat(queryResult).isNotNull()

        val feature = queryResult!!.firstOrNull() as? ArcGISFeature
        assertThat(feature).isNotNull()

        val featureForm = FeatureForm(feature!!)
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumnNode.performScrollToNode(hasText("Associations")).assertIsDisplayed()

        val containerNode = composeTestRule.onNodeWithText("Container").assertIsDisplayed()
        containerNode.performClick()

        // Verify the groups are displayed
        var listView = composeTestRule.onNode(hasScrollAction())
        listView.onChildWithText("Structure Boundary").assertIsDisplayed().performClick()

        // Verify the associations are displayed
        listView = composeTestRule.onNode(hasScrollAction())
        val associationNode = listView.onChildAt(0)

        associationNode.assertIsDisplayed()
        associationNode.assert(hasText("Containment Visible").not())
    }

    /**
     * Test case 12.5
     *
     * Given a `FeatureForm` with a `UtilityAssociationsFormElement`
     * When the `FeatureForm` is displayed
     * Then any edits to the `FeatureForm` prevent navigation to associated features
     *
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-125-edits-prevent-navigating
     */
    @Test
    fun testEditsPreventNavigation() = runTest {
        val groupLayer = map.operationalLayers.first()
        val layer = groupLayer.subLayerContents.value.find {
            it.name == "Electric Distribution Device"
        } as FeatureLayer?
        assertThat(layer).isNotNull()

        val queryResult = layer!!.featureTable!!.queryFeatures(
            QueryParameters().apply {
                objectIds.add(3321)
            }
        ).getOrNull()
        assertThat(queryResult).isNotNull()

        val feature = queryResult!!.firstOrNull() as? ArcGISFeature
        assertThat(feature).isNotNull()

        val featureForm = FeatureForm(feature!!)
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumnNode.performScrollToNode(hasText("Associations")).assertIsDisplayed()

        val connectedNode = composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        connectedNode.performClick()

        // Verify the groups are displayed
        var listView = composeTestRule.onNode(hasScrollAction())
        listView.onChildWithText("Electric Distribution Device").assertIsDisplayed().performClick()

        listView = composeTestRule.onNode(hasScrollAction())
        val firstElement = listView.onChildWithText("Object ID : 2552").assertIsDisplayed()
        firstElement.performClick()

        val formElementNode = composeTestRule.onNodeWithText("Asset type *")
        formElementNode.assertIsDisplayed()
        formElementNode.performClick()

        // find the combo box dialog
        val comboBoxDialogList =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogListSemanticLabel)
        comboBoxDialogList.assertIsDisplayed()
        // tap on the "unknown" option
        val listItem =
            comboBoxDialogList.onChildWithContentDescription("Unknown list item")
        listItem.assertIsDisplayed()
        listItem.performClick()
        // find and tap the done button
        val doneButton =
            composeTestRule.onNodeWithContentDescription(comboBoxDialogDoneButtonSemanticLabel)
        doneButton.performClick()

        // Verify the edit actions are displayed
        composeTestRule.onNodeWithText("Save").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("Discard").assertIsDisplayed().assertHasClickAction()

        // Press the back button to trigger the save edits dialog
        Espresso.pressBack()
        // Verify the save edits dialog is displayed
        val dialog = composeTestRule.onNode(isDialog()).assertExists()
        // Find and click the "Discard" button
        val discardButton = dialog.onChildWithText("Discard", recurse = true).assertIsDisplayed()
        discardButton.performClick()
        // Verify the initial association is displayed again
        firstElement.assertIsDisplayed()
    }
}
