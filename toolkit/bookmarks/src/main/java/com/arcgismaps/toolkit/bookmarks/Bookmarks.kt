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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.toolkit.geoviewcompose.GeoViewProxy
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * A composable that displays a list of bookmarks.
 * @param geoViewProxy is the proxy for the GeoView that will be used to set the viewpoint when a bookmark is selected.
 * @param bookmarks is used to allow for dynamic updates to the list of bookmarks.
 * @param modifier is the modifier to be applied to the list.
 * @param onBookmarkSelected is a callback that is invoked when a bookmark is selected.
 * @param backgroundColor is the background color of the list.
 * @param textStyle is the style to be applied to the bookmark text.
 * @since 300.2.0
 */
@Composable
public fun BookmarksListView(
    geoViewProxy : GeoViewProxy,
    bookmarks: SnapshotStateList<Bookmark>,
    modifier: Modifier = Modifier,
    onBookmarkSelected : (Bookmark) -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    textStyle : TextStyle = LocalTextStyle.current
) {
    val scope = rememberCoroutineScope()
    Surface(
        modifier = modifier,
        color = backgroundColor
    ) {
        //If there is no bookmarks show an empty state
        if(bookmarks.isEmpty()){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.no_bookmarks)
                )
                Text(
                    text = stringResource(R.string.no_bookmarks)
                )
            }
        }else{
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                //Display bookmarks.
                //If a bookmark is selected, move GeoView to viewpoint and return selected bookmark for callback
                items(bookmarks.size) { index ->
                    val bookmark = bookmarks.elementAt(index)
                    BookmarkItem(
                        bookmark = bookmark,
                        onZoom = {
                            scope.launch {
                                bookmark.viewpoint?.let {
                                    geoViewProxy.setViewpointAnimated(
                                        viewpoint = it
                                    )
                                }
                            }
                            onBookmarkSelected(bookmark)
                        },
                        textStyle = textStyle
                    )
                }
            }
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
            .heightIn(min = 48.dp)
            .semantics{
                contentDescription = bookmark.name
            }
    ) {
        Text(
            text = bookmark.name,
            style = textStyle.copy(lineHeight = 1.2.em)
        )
    }
    HorizontalDivider()
}


@Preview
@Composable
public fun BookmarkPreview(){
    val geoViewProxy = MapViewProxy()
    BookmarksListView(
        geoViewProxy = geoViewProxy,
        bookmarks = emptyList<Bookmark>().toMutableStateList()
    )
}
