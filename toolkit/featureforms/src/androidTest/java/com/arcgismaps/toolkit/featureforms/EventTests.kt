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
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
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
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EventTests {
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
     * Given a FeatureForm with a [UtilityAssociationsFormElement]
     * When navigation events occur
     * Then the appropriate [FeatureFormNavigationRoute] events are fired
     *
     * @since 300.0.0
     */
    @Test
    fun testUtilityAssociationNavigationEvents() = runTest {
        val groupLayer = map.operationalLayers.first()
        val layer = groupLayer.subLayerContents.value.find {
            it.name == "Electric Distribution Device"
        } as FeatureLayer?
        assertThat(layer).isNotNull()

        val queryResult = layer!!.featureTable!!.queryFeatures(
            QueryParameters().apply {
                objectIds.add(3221)
            }
        ).getOrNull()
        assertThat(queryResult).isNotNull()

        val feature = queryResult!!.firstOrNull() as? ArcGISFeature
        assertThat(feature).isNotNull()

        val featureForm = FeatureForm(feature!!)
        val state = FeatureFormState(
            featureForm = featureForm,
            coroutineScope = scope
        )
        var lastEvent: FeatureFormNavigationRoute? = null
        composeTestRule.setContent {
            FeatureForm(
                featureFormState = state,
                onNavigationEvent = {
                    lastEvent = it
                }
            )
        }

        // Wait for the initial FeatureForm event
        composeTestRule.waitUntil {
            lastEvent == FeatureFormNavigationRoute.Form
        }

        val element = featureForm.elements.first {
            it is UtilityAssociationsFormElement
        } as UtilityAssociationsFormElement

        // Wait for the associations to load
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            element.associationsFilterResults.isNotEmpty()
        }

        val lazyColumnNode = composeTestRule.onNodeWithContentDescription("lazy column")
        lazyColumnNode.performScrollToNode(hasText("Associations")).assertIsDisplayed()

        val filterResult = element.associationsFilterResults.first {
            it.filter.title == "Connected"
        }

        // Find and click the filter node
        val filterNode = composeTestRule.onNodeWithText(text = filterResult.filter.title)
        filterNode.performClick()

        // Verify that the UtilityAssociationsFilterResultNav event is received
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.AssociationsFilterResult
        }
        var filterEvent = lastEvent as FeatureFormNavigationRoute.AssociationsFilterResult
        assertThat(filterEvent.element).isEqualTo(element)
        assertThat(filterEvent.filterResult.filter).isEqualTo(filterResult.filter)

        val groupResult = filterResult.groupResults.first()

        val groupNode = composeTestRule.onNode(hasText(groupResult.name))
        groupNode.performClick()

        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.AssociationGroupResult
        }
        var groupEvent = lastEvent as FeatureFormNavigationRoute.AssociationGroupResult
        assertThat(groupEvent.element).isEqualTo(element)
        assertThat(groupEvent.groupResult.name).isEqualTo(groupResult.name)
        assertThat(groupEvent.groupResult.featureFormSource).isEqualTo(groupResult.featureFormSource)

        val associationResult = groupResult.associationResults.first()
        var listView = composeTestRule.onNode(hasScrollAction())
        val associationNode = listView.onChildAt(0)
        // Click the details button on the association node
        associationNode.onChildWithContentDescription("details").performClick()

        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.AssociationResult
        }
        val associationEvent = lastEvent as FeatureFormNavigationRoute.AssociationResult
        assertThat(associationEvent.element).isEqualTo(element)
        assertThat(associationEvent.result.title).isEqualTo(associationResult.title)
        assertThat(associationEvent.result.association.globalId).isEqualTo(
            associationResult.association.globalId
        )
        assertThat(associationEvent.result.associatedFeature).isEqualTo(
            associationResult.associatedFeature
        )

        Espresso.pressBack()

        // Verify that we are back on the group result screen and the event is fired again
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.AssociationGroupResult
        }
        groupEvent = lastEvent as FeatureFormNavigationRoute.AssociationGroupResult
        assertThat(groupEvent.element).isEqualTo(element)
        assertThat(groupEvent.groupResult.name).isEqualTo(groupResult.name)
        assertThat(groupEvent.groupResult.featureFormSource).isEqualTo(groupResult.featureFormSource)

        Espresso.pressBack()

        // Verify that we are back on the filter result screen and the event is fired again
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.AssociationsFilterResult
        }
        filterEvent = lastEvent as FeatureFormNavigationRoute.AssociationsFilterResult
        assertThat(filterEvent.element).isEqualTo(element)
        assertThat(filterEvent.filterResult.filter).isEqualTo(filterResult.filter)

        // Get and click the add button
        val addButton = composeTestRule.onNodeWithContentDescription("Add Associations")
        addButton.performClick()

        // Tap the from network data source option
        val options = composeTestRule.onNode(isDialog())
        options.onChildWithText("From Network Data Source", recurse = true).performClick()

        // Verify that the SelectAssociationFeatureSource event is received
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.SelectAssociationFeatureSource
        }
        val sourceEvent = lastEvent as FeatureFormNavigationRoute.SelectAssociationFeatureSource
        assertThat(sourceEvent.element).isEqualTo(element)

        // Get the list view
        listView = composeTestRule.onNode(hasScrollAction())
        // Wait for the list to populate
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            listView.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Assert the expected sources are present
        val source = listView.onChildWithText("Electric Distribution Device")
        // Click the source
        source.performClick()

        // Verify that the SelectUtilityAssetType event is received
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.SelectUtilityAssetType
        }
        val assetTypeEvent = lastEvent as FeatureFormNavigationRoute.SelectUtilityAssetType
        assertThat(assetTypeEvent.element).isEqualTo(element)
        assertThat(assetTypeEvent.featureSource.name).isEqualTo("Electric Distribution Device")

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

        // Verify that the SelectAssociationFeatureCandidate event is received
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.SelectAssociationFeatureCandidate
        }
        val candidateEvent = lastEvent as FeatureFormNavigationRoute.SelectAssociationFeatureCandidate
        assertThat(candidateEvent.element).isEqualTo(element)
        assertThat(candidateEvent.featureSource.name).isEqualTo("Electric Distribution Device")
        assertThat(candidateEvent.assetType.name).isEqualTo("Cabinet Fuse")

        // Wait for the list of feature candidates to load
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onNode(hasScrollAction()).isDisplayed()
        }

        // Update the list view
        listView = composeTestRule.onNode(hasScrollAction())
        val featureNode = listView.onChildAt(0)
        featureNode.assert(hasText("Fuse"))
        featureNode.assertHasClickAction().performClick()

        // Verify that the CreateAssociation event is received
        composeTestRule.waitUntil {
            lastEvent is FeatureFormNavigationRoute.CreateAssociation
        }
        val createEvent = lastEvent as FeatureFormNavigationRoute.CreateAssociation
        assertThat(createEvent.element).isEqualTo(element)
        assertThat(createEvent.featureSource.name).isEqualTo("Electric Distribution Device")
        assertThat(createEvent.candidate.title).isEqualTo("Fuse")

        // Wait for the add button to be enabled
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithText("Add").isEnabled()
        }
        // Click the add button to create the association
        composeTestRule.onNodeWithText("Add").performClick()

        // Verify that we are back on the filter result screen and the event is fired again
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            lastEvent is FeatureFormNavigationRoute.AssociationsFilterResult
        }
        filterEvent = lastEvent as FeatureFormNavigationRoute.AssociationsFilterResult
        assertThat(filterEvent.element).isEqualTo(element)
        assertThat(filterEvent.filterResult.filter).isEqualTo(filterResult.filter)
    }
}
