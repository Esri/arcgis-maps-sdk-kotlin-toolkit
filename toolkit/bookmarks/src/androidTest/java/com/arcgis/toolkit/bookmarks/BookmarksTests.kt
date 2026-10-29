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