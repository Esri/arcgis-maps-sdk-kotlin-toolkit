/*
 * Copyright 2023 Esri
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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class GroupElementTests {

    private val featureForm by lazy {
        sharedFeatureForm!!
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getGroupElementWithLabel(label: String): GroupFormElement {
        return featureForm.elements
            .filterIsInstance<GroupFormElement>()
            .first {
                it.label == label
            }
    }

    @Before
    fun setContent() {
        composeTestRule.setContent {
            val state = FeatureFormState()
            state.setFeatureForm(featureForm)
            FeatureForm(featureFormState = state)
        }
    }

    /**
     * Test case 6.1:
     * Given a GroupFormElement with label, description and an initial expansion value
     * Then the GroupFormElement is initially expanded or collapsed appropriately
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-61-test-initially-expanded-and-collapsed
     */
    @Test
    fun testInitialState() {
        val groupFormElement1 = getGroupElementWithLabel("Group with Multiple Form Elements")
        val groupElement1 = composeTestRule.onNodeWithText(groupFormElement1.label)
        groupElement1.assertIsDisplayed()
        // assert description is displayed
        groupElement1.assertTextContains(groupFormElement1.description)
        assert(groupElement1.isToggled())
        // assert this group has children including the header
        assert(groupElement1.onParent().onChildren().fetchSemanticsNodes().count() > 1)

        val groupElement2 = composeTestRule.onNodeWithText("Group with Multiple Form Elements 2")
        groupElement2.assertIsDisplayed()
        assert(!groupElement2.isToggled())
        // assert that only the header is displayed
        assert(groupElement2.onParent().onChildren().fetchSemanticsNodes().count() == 1)
    }

    /**
     * Test case 6.2:
     * Given a GroupFormElement with label, description and the group's elements are not visible
     * When a FieldFormElement's visibility expression that controls this group's elements visibility
     * is triggered
     * Then the elements of this group are visible
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-62-test-visibility-of-empty-group
     */
    @Test
    fun testVisibility() {
        val groupElementToTest =
            getGroupElementWithLabel("Group with children that are visible dependent")
        // find the scrollable container
        val lazyColumn = composeTestRule.onNode(hasScrollAction())
        // scroll until the group is visible
        lazyColumn.performScrollToNode(hasText(groupElementToTest.label))
        // find the group and check if displayed
        val groupElement = composeTestRule.onNodeWithText(groupElementToTest.label)
        groupElement.assertIsDisplayed()
        // assert description is shown
        groupElement.assertTextContains(groupElementToTest.description)
        // assert only the header is visible and other field elements are not
        assert(groupElement.onParent().onChildren().fetchSemanticsNodes().count() == 1)
        // find and click on the radio button option
        composeTestRule.onNodeWithText("show invisible form element").performClick()
        // assert the and other field elements are visible
        assert(groupElement.onParent().onChildren().fetchSemanticsNodes().count() > 1)
    }

    companion object {
        private var sharedFeatureFormDefinition: FeatureFormDefinition? = null
        private var sharedFeatureForm: FeatureForm? = null
        private var sharedFeature: ArcGISFeature? = null
        private var sharedMap: ArcGISMap? = null

        @BeforeClass
        @JvmStatic
        fun setupClass() = runTest {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
                FeatureFormsTestChallengeHandler(
                    BuildConfig.webMapUser,
                    BuildConfig.webMapPassword
                )

            sharedMap =
                ArcGISMap("https://runtimecoretest.maps.arcgis.com/apps/mapviewer/index.html?webmap=97495f67bd2e442dbbac485232375b07")
            sharedMap?.load()
                ?.onFailure { TestCase.fail("failed to load webmap with ${it.message}") }
            val featureLayer = sharedMap?.operationalLayers?.first() as? FeatureLayer
            featureLayer?.let { layer ->
                layer.load().onFailure { TestCase.fail("failed to load layer with ${it.message}") }
                sharedFeatureFormDefinition = layer.featureFormDefinition!!
                val parameters = QueryParameters().also {
                    it.objectIds.add(1L)
                    it.maxFeatures = 1
                }
                layer.featureTable?.queryFeatures(parameters)?.onSuccess {
                    sharedFeature = it.filterIsInstance<ArcGISFeature>().firstOrNull()
                    if (sharedFeature == null) TestCase.fail("failed to fetch feature")
                    sharedFeature?.load()
                        ?.onFailure { TestCase.fail("failed to load feature with ${it.message}") }
                    sharedFeatureForm = FeatureForm(sharedFeature!!, sharedFeatureFormDefinition!!)
                    sharedFeatureForm!!.evaluateExpressions()
                }?.onFailure {
                    TestCase.fail("failed to query features on layer's featuretable with ${it.message}")
                }
            }
        }
    }
}

/**
 * Returns true if the [ToggleableState] property is [ToggleableState.On].
 */
internal fun SemanticsNodeInteraction.isToggled(): Boolean {
    val semantics = fetchSemanticsNode()
    return semantics.config[SemanticsProperties.ToggleableState] == ToggleableState.On
}
