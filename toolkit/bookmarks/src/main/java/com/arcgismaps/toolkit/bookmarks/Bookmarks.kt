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

package com.arcgismaps.toolkit.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.Bookmark
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * A composable that displays a list of bookmarks.
 * @param bookmarks is used to allow for dynamic updates to the list of bookmarks.
 * @param onSelected is a callback that is invoked when a bookmark is selected.
 * @param backgroundColor is the background color of the list.
 * @textStyle is the style to be applied to the bookmark text.
 * @since 300.2.0
 */
@Composable
public fun BookmarksListView(
    bookmarks: SnapshotStateList<Bookmark>,
    onSelected: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    textStyle : TextStyle = LocalTextStyle.current
) {
    LazyColumn(
        modifier =
            modifier.fillMaxWidth().background(color = backgroundColor)
    ) {
        items(bookmarks.size) { index ->
            val bookmark = bookmarks.elementAt(index)
            BookmarkItem(
                bookmark = bookmark,
                onZoom = { onSelected(bookmark) },
                textStyle = textStyle
            )
        }
    }
}


/**
 * A composable that displays a single bookmark item.
 * @param bookmark is the bookmark to be displayed.
 * @param onZoom is a callback that is invoked when the bookmark is selected.
 * @param textStyle is the style to be applied to the bookmark text.
 * @since 300.2.0
 */
@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onZoom: () -> Unit,
    textStyle: TextStyle
){
    Row(
        modifier = Modifier
            .clickable(
                onClick = onZoom
            )
            .fillMaxWidth()
            .padding(16.dp)
            .semantics{
                contentDescription = bookmark.name
            }
    ) {
        Text(text = bookmark.name, style = textStyle)
    }
    HorizontalDivider()
}

@Preview
@Composable
private fun BookmarksListViewPreview() {
    val bookmarks = remember { mutableStateListOf(
        Bookmark("Bookmark 1", null)
    ) }
    BookmarksListView(
        bookmarks = bookmarks,
        onSelected = {},
    )
    LaunchedEffect(Unit) {
        listOf(1, 2, 3).forEach {
            delay(1500.milliseconds)
            bookmarks.add(Bookmark("Bookmark $it", null))
        }
    }
}


@Preview
@Composable
private fun BookmarkItemPreview() {
    BookmarkItem(
        bookmark = Bookmark("Bookmark 1", null),
        onZoom = {},
        textStyle = LocalTextStyle.current
    )
}
