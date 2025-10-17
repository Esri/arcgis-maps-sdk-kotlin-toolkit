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

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
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
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UtilityAssociationsFormElementAddTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var map: ArcGISMap

    private val scope = CoroutineScope(Dispatchers.Main)

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
     * Helper method to get a FeatureForm for a given object ID and layer name.
     */
    suspend fun getFeatureFormForObjectId(
        objectId: Long,
        layerName: String
    ): FeatureForm {
        val groupLayer = map.operationalLayers.first()
        val layer = groupLayer.subLayerContents.value.find {
            it.name == layerName
        } as FeatureLayer?
        assertThat(layer).isNotNull()

        val queryResult = layer!!.featureTable!!.queryFeatures(
            QueryParameters().apply {
                objectIds.add(objectId)
            }
        ).getOrNull()
        assertThat(queryResult).isNotNull()

        val feature = queryResult!!.firstOrNull() as? ArcGISFeature
        assertThat(feature).isNotNull()

        return FeatureForm(feature!!)
    }

    /**
     * Given a FeatureForm with a UtilityAssociationsFormElement
     * When the user adds an connected association from a network data source
     * Then the association should be added to the group
     *
     * Design : https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-131-add-an-association
     *
     * @since 300.0.0
     */
    @Test
    fun addAssociationAndDiscard() = runTest {
        val featureForm = getFeatureFormForObjectId(
            objectId = 3321,
            layerName = "Electric Distribution Device"
        )
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }

        // Get the UtilityAssociationsFormElement
        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        // Get the lazy column
        val lazyColumn = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumn.performScrollToNode(hasText("Associations")).assertIsDisplayed()
        // Get the connected result
        val connectedNode = composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        connectedNode.performClick()

        // Verify the group is displayed
        composeTestRule.onNode(hasTextExactly("Electric Distribution Device", "1")).isDisplayed()

        // Get and click the add button
        val addButton = composeTestRule.onNodeWithContentDescription("Add Associations")
        addButton.assertIsDisplayed().performClick()

        // Tap the from network data source option
        val options = composeTestRule.onNode(isDialog())
        options.assertIsDisplayed()
        options.onChildWithText("From Network Data Source", recurse = true).performClick()

        // Get the list view
        var listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list to populate
        composeTestRule.waitUntil {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Assert the expected sources are present
        val source = listView.onChildWithText("Electric Distribution Device").assertIsDisplayed()
        listView.onChildWithText("Electric Distribution Line").assertIsDisplayed()
        listView.onChildWithText("Electric Distribution Junction").assertIsDisplayed()
        // Click the source
        source.performClick()
        composeTestRule.waitForIdle()

        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list to populate with asset types
        composeTestRule.waitUntil {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Might have to scroll to find the asset type
        listView.performScrollToNode(hasText("Cabinet Fuse"))
        val assetType = listView.onChildWithText("Cabinet Fuse").assertIsDisplayed()
        assetType.performClick()

        // Wait for the list of feature candidates to load
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onNode(hasScrollAction()).isDisplayed()
        }

        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        val featureNode = listView.onChildAt(0)
        featureNode.assert(hasText("Fuse"))
        featureNode.assertHasClickAction().performClick()

        // Wait for the add button to be enabled
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithText("Add").isEnabled()
        }

        // Verify the add association screen is visible
        composeTestRule.onNodeWithText("New Association").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connectivity").assertIsDisplayed()
        composeTestRule.onNodeWithText("From Element").assertIsDisplayed()
        composeTestRule.onNodeWithText("Electric Distribution Device").assertIsDisplayed()
        composeTestRule.onNodeWithText("To Element").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fuse").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertIsDisplayed().performClick()

        // Wait for the group to update
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            // Verify the association was added
            composeTestRule.onNode(hasTextExactly("Electric Distribution Device", "2"))
                .isDisplayed()
        }

        // Assert the save and discard buttons are enabled
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
    }

    /**
     * Given a FeatureForm with a UtilityAssociationsFormElement
     * When the user adds a connected association from a network data source with terminal and fraction
     * Then the association should be added to the group
     *
     * Design : https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-132-add-connectivity-association-with-terminal-and-fraction-along
     *
     * @since 300.0.0
     */
    @Test
    fun addAssociationWithTerminalAndFraction() = runTest {
        val featureForm = getFeatureFormForObjectId(
            objectId = 5050,
            layerName = "Electric Distribution Device"
        )
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }
        // Get the UtilityAssociationsFormElement
        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        // Get the lazy column
        val lazyColumn = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumn.performScrollToNode(hasText("Associations")).assertIsDisplayed()
        // Get and click the connected result
        val connectedNode = composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        connectedNode.performClick()

        // Verify the groups are displayed
        composeTestRule.onNode(
            hasTextExactly("Electric Distribution Junction", "5")
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasTextExactly("Electric Distribution Device", "2")
        ).assertIsDisplayed()

        // Get and click the add button
        val addButton = composeTestRule.onNodeWithContentDescription("Add Associations")
        addButton.assertIsDisplayed().performClick()

        // Tap the from network data source option
        val options = composeTestRule.onNode(isDialog())
        options.assertIsDisplayed()
        options.onChildWithText("From Network Data Source", recurse = true).performClick()
        composeTestRule.waitForIdle()

        // Get the list of network sources
        var listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list to populate
        composeTestRule.waitUntil {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }

        // Assert the expected sources are present
        val source = listView.onChildWithText("Electric Distribution Line").assertIsDisplayed()
        listView.onChildWithText("Electric Distribution Device").assertIsDisplayed()
        listView.onChildWithText("Electric Distribution Junction").assertIsDisplayed()
        // Click the source
        source.performClick()
        composeTestRule.waitForIdle()

        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list of asset types to populate
        composeTestRule.waitUntil {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }

        val assetType = listView.onChildWithContentDescription(
            value = "Underground Three Phase:85"
        ).assertIsDisplayed()
        assetType.performClick()

        // Wait for the list of feature candidates to load
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onNode(hasScrollAction()).isDisplayed()
        }

        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        val featureNode = listView.onChildAt(0)
        featureNode.assert(hasText("Low Voltage"))
        featureNode.assertHasClickAction().performClick()

        // Wait for the add button to be enabled
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithText("Add").isEnabled()
        }

        // Verify the add association screen is visible
        composeTestRule.onNodeWithText("Association Type").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connectivity").assertIsDisplayed()
        composeTestRule.onNodeWithText("From Element").assertIsDisplayed()
        composeTestRule.onNodeWithText("Electric Distribution Device").assertIsDisplayed()
        composeTestRule.onNodeWithText("To Element").assertIsDisplayed()
        composeTestRule.onNodeWithText("Low Voltage").assertIsDisplayed()
        // Verify the terminal info is displayed
        val terminalControl = composeTestRule.onNode(hasTextExactly("Terminal", "High"))
        terminalControl.assertIsDisplayed()
        // Verify the fraction control is displayed
        composeTestRule.onNodeWithText("Fraction Along Edge").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 %").assertIsDisplayed()
        val fractionControl =
            composeTestRule.onNodeWithContentDescription("fraction along edge slider")
        fractionControl.assertIsDisplayed()
        // Verify the slider has the correct initial state
        fractionControl.assertRangeInfoEquals(ProgressBarRangeInfo(0.0f, 0.0f..1.0f, 0))
        // Select the terminal
        terminalControl.performClick()
        // Wait for the terminal dialog to appear
        composeTestRule.waitForIdle()
        val terminalDialog = composeTestRule.onNode(isPopup())
        // Select the "Low" terminal
        terminalDialog.onChildWithText("Low", recurse = true).performClick()
        // Verify the terminal was selected
        composeTestRule.onNode(hasTextExactly("Terminal", "Low")).assertIsDisplayed()
        // Set the fraction to 75%
        fractionControl.performSemanticsAction(SemanticsActions.SetProgress) { progress ->
            progress(0.75f)
        }
        composeTestRule.onNodeWithText("75 %").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").performClick()

        // Wait for the group to update
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            // Verify the association was added
            composeTestRule.onNode(
                hasTextExactly("Electric Distribution Line", "1")
            ).isDisplayed()
        }

        // Assert the save and discard buttons are enabled
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
    }

    /**
     * Given a FeatureForm with a UtilityAssociationsFormElement
     * When the user adds a containment association from a network data source
     * Then the association should be added to the group
     *
     * Design : https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-133-add-containment-association-with-content-visible-option
     *
     * @since 300.0.0
     */
    @Test
    fun addContainmentAssociation() = runTest {
        val featureForm = getFeatureFormForObjectId(
            objectId = 311,
            layerName = "Electric Distribution Junction"
        )
        val featureFormState = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        composeTestRule.setContent {
            FeatureForm(featureFormState = featureFormState)
        }
        // Get the UtilityAssociationsFormElement
        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement
        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        // Get the lazy column
        val lazyColumn = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumn.performScrollToNode(hasText("Associations")).assertIsDisplayed()

        // Get and click the containment result
        val containmentNode = composeTestRule.onNodeWithText("Container").assertIsDisplayed()
        containmentNode.performClick()

        // Verify the group is displayed
        composeTestRule.onNode(hasTextExactly("Structure Junction", "1")).assertIsDisplayed()

        // Get and click the add button
        val addButton = composeTestRule.onNodeWithContentDescription("Add Associations")
        addButton.assertIsDisplayed().performClick()

        // Tap the from network data source option
        val options = composeTestRule.onNode(isDialog())
        options.assertIsDisplayed()
        options.onChildWithText("From Network Data Source", recurse = true).performClick()
        composeTestRule.waitForIdle()

        // Get the list of network sources
        var listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list to populate
        composeTestRule.waitUntil {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Assert the expected source is present
        val source = listView.onChildWithText("Structure Junction").assertIsDisplayed()
        listView.onChildWithText("Structure Boundary").assertIsDisplayed()
        listView.onChildWithText("Electric Distribution Assembly").assertIsDisplayed()
        // Click the source
        source.performClick()
        composeTestRule.waitForIdle()

        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list of asset types to populate
        composeTestRule.waitUntil {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Select the asset type
        val assetType = listView.onChildWithText(value = "Vault").assertIsDisplayed()
        assetType.performClick()

        // Wait for the list of feature candidates to load
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onNode(hasScrollAction()).isDisplayed()
        }
        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        val featureNode = listView.onChildAt(0)
        featureNode.assert(hasText("Vault"))
        featureNode.assertHasClickAction().performClick()

        // Wait for the add button to be enabled
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithText("Add").isEnabled()
        }

        // Verify the add association screen is visible
        composeTestRule.onNodeWithText("Association Type").assertIsDisplayed()
        composeTestRule.onNodeWithText("Container").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content Visible").assertIsDisplayed()
        composeTestRule.onNodeWithText("From Element").assertIsDisplayed()
        composeTestRule.onNodeWithText("Electric Distribution Junction").assertIsDisplayed()
        composeTestRule.onNodeWithText("To Element").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vault").assertIsDisplayed()


        val switch = composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        ).assertIsDisplayed()
        // Verify the switch is initially off
        switch.assertIsOff()
        // Toggle the content visible switch
        switch.performClick()
        // Verify the switch is toggled on
        switch.assertIsOn()

        composeTestRule.onNodeWithText("Add").assertIsDisplayed().performClick()

        // Wait for the group to update
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            // Verify the association was added
            composeTestRule.onNode(
                hasTextExactly("Structure Junction", "2")
            ).isDisplayed()
        }

        // Assert the save and discard buttons are enabled
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
    }
}
