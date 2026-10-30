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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.toolkit.bookmarks.BookmarksListView
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Rule

class BookmarksTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBookmarkListItems() {
        var selectedBookmark by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            BookmarksListView(
                bookmarks = mutableListOf(
                    Bookmark("Bookmark 1", null),
                    Bookmark("Bookmark 2", null),
                    Bookmark("Bookmark 3", null)
                ),
                onSelected = { selectedBookmark = it.name }
            )
        }


        val bookmark1 = composeTestRule.onNodeWithContentDescription("Bookmark 1")
        val bookmark2 = composeTestRule.onNodeWithContentDescription("Bookmark 2")
        val bookmark3 = composeTestRule.onNodeWithContentDescription("Bookmark 3")

        bookmark1.assertIsDisplayed()
        bookmark2.assertIsDisplayed()
        bookmark3.assertIsDisplayed()

        bookmark3.performClick()
        assertEquals("Bookmark 3", selectedBookmark)

        bookmark1.performClick()
        assertEquals("Bookmark 1", selectedBookmark)

        bookmark2.performClick()
        assertEquals("Bookmark 2", selectedBookmark)
    }
}