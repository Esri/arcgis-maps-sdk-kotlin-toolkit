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

@file:Suppress("DEPRECATION")

package com.arcgismaps.toolkit.composablemap

import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import com.arcgismaps.mapping.ArcGISMap
import org.junit.Rule
import org.junit.Test

class ComposableMapTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposableMapLayout() {
        val mockMapInterface = MapInterface(ArcGISMap())

        composeTestRule.setContent {
            ComposableMap(mapInterface = mockMapInterface) {
                Card(modifier = Modifier.semantics { contentDescription = "Card" }) {}
            }
        }
        val mapContainerSemanticLabel = "MapContainer"
        val mapViewSemanticLabel = "MapView"
        val contentSemanticLabel = "Content"

        val mapContainer = composeTestRule.onNodeWithContentDescription(mapContainerSemanticLabel)
        mapContainer.assertIsDisplayed()
        mapContainer.onChildAt(0).assert(hasContentDescription(mapViewSemanticLabel))
        mapContainer.onChildAt(1).assert(hasContentDescription(contentSemanticLabel))

        composeTestRule.onNodeWithContentDescription("Card")
            .assert(hasParent(hasContentDescription(contentSemanticLabel)))
    }
}
