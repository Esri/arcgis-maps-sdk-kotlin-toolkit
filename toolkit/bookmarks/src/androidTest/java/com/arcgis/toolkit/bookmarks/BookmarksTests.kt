/*
 *
 *  Copyright 2026 Esri
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

package com.arcgis.toolkit.bookmarks

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arcgismaps.toolkit.bookmarks.BookmarksListView
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Rule

/**
 * Tests for the [BookmarksListView] composable.
 * Shows bookmark lists and makes sure expected bookmarks are being selected
 * @since 300.2.0
 */
class BookmarksTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBookmarkListItems() = runTest {

        val mapViewModel = MapViewModel("16f1b8ba37b44dc3884afc8d5f454dd2")
        val bookmarks = mapViewModel.load()

        composeTestRule.setContent {
            BookmarksScenario(
                mapViewModel = mapViewModel,
                bookmarks = bookmarks
            )
        }

        val bookmark1 = composeTestRule.onNodeWithContentDescription("Giant Sequoias of Willamette Blvd")
        val bookmark2 = composeTestRule.onNodeWithContentDescription("Large Douglas-fir")
        val bookmark3 = composeTestRule.onNodeWithContentDescription("Historic Ladd's Addition")
        val bookmark4 = composeTestRule.onNodeWithContentDescription("Irvington neighborhood")

        bookmark1.assertIsDisplayed()
        bookmark2.assertIsDisplayed()
        bookmark3.assertIsDisplayed()
        bookmark4.assertIsDisplayed()

        assertEquals(null,mapViewModel.bookmarkSelected.value)

        bookmark3.performClick()

        assertEquals("Historic Ladd's Addition",mapViewModel.bookmarkSelected.value?.name)

        bookmark2.performClick()

        assertEquals("Large Douglas-fir",mapViewModel.bookmarkSelected.value?.name)

    }
}
