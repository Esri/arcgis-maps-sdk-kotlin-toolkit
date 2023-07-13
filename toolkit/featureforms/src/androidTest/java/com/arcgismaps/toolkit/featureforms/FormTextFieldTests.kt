/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureforms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.formInfoJson
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class FormsArcGISAuthenticationChallengeHandler(
    private val username: String,
    private val password: String
) : ArcGISAuthenticationChallengeHandler {
    override suspend fun handleArcGISAuthenticationChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val result: Result<TokenCredential> =
            TokenCredential.create(
                challenge.requestUrl,
                username,
                password,
                tokenExpirationInterval = 0
            )
        return result.let {
            if (it.isSuccess) {
                ArcGISAuthenticationChallengeResponse.ContinueWithCredential(it.getOrThrow())
            } else {
                ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(it.exceptionOrNull()!!)
            }
        }
    }
}

fun SemanticsNodeInteraction.assertTextColor(
    color: Color
): SemanticsNodeInteraction = assert(isOfColor(color))

private fun isOfColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} is of color '$color'"
) {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    it.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action
        ?.invoke(textLayoutResults)
    return@SemanticsMatcher if (textLayoutResults.isEmpty()) {
        false
    } else {
        textLayoutResults.first().layoutInput.style.color == color
    }
}

class FormTextFieldTests {
    lateinit var featureFormDefinition: FeatureFormDefinition
    val labelSemanticLabel = "label"
    val helperSemanticLabel = "helper"
    val outlinedTextFieldSemanticLabel = "outlined text field"
    
    @Before
    fun setUp() = runTest {
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            FormsArcGISAuthenticationChallengeHandler(
                //TODO: run these tests from json in a file.
                "c_api_publisher",
                "c_api_pub1"
            )
        val map =
            ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=5d69e2301ad14ec8a73b568dfc29450a")
        map.load()
        val layer = map.operationalLayers.first() as FeatureLayer
        featureFormDefinition = FeatureFormDefinition.fromJsonOrNull(layer.formInfoJson!!)!!
    }
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-unfocused-and-focused-state-no-value
     */
    @Test
    fun testNoValueUnfocusedState() = runTest {
        
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
    
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement))
        }
        
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
    
        try {
            composeTestRule.onNodeWithContentDescription(helperSemanticLabel)
         } catch (e: AssertionError) {
             //expected -- no such node
         }
    }
    
    /**
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-11-unfocused-and-focused-state-no-value
     */
    @Test
    fun testNoValueFocusedState() = runTest {
        
        val fieldFeatureFormElement = featureFormDefinition.formElements
            .filterIsInstance<FieldFeatureFormElement>()
            .first {
                it.label == "Single Line No Value, Placeholder or Description"
            }
        
        composeTestRule.setContent {
            FormTextField(state = FormTextFieldState(fieldFeatureFormElement))
        }
        
        val outlinedTextField = composeTestRule.onNodeWithContentDescription(outlinedTextFieldSemanticLabel)
        outlinedTextField.performClick()
        outlinedTextField.assertIsFocused()
        val label = composeTestRule.onNodeWithContentDescription(labelSemanticLabel)
        label.assertIsDisplayed()
        
        val helper = composeTestRule.onNodeWithContentDescription(helperSemanticLabel)
        val helperNode = helper.fetchSemanticsNode().config.first {
            println("SROTH name ${it.key.name}")
            it.key.name == "Text"
        }
        helper.assertIsDisplayed()
        //assertEquals("Maximum 256 characters", helperNode.value.toString())
        //helperNode.assertTextColor(Color.Red)
    }
}