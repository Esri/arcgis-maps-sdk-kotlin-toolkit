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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.bookmarks.BookmarksListView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

/**
 * [MapViewModel] provides the map, proxy, bookmarks and bookmark selection state for [BookmarksTests].
 * @param itemId is the portal item ID of the web map containing the bookmarks.
 * @since 300.2.0
 */
class MapViewModel internal constructor(internal val itemId: String) : ViewModel() {

    val map = ArcGISMap(
        item = PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            itemId = itemId
        )
    )
    val mapViewProxy = MapViewProxy()
    private val _bookmarks = mutableListOf<Bookmark>()
    var bookmarkSelected = mutableStateOf<Bookmark?>(null)
    suspend fun load(): List<Bookmark> {
        map.load().getOrThrow()
        val bookmarks = map.bookmarks
        _bookmarks.addAll(bookmarks)
        return _bookmarks.toList()
    }
}

/**
 * Composable Scenario for the [BookmarksTests]
 *
 * @since 300.2.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScenario(mapViewModel: MapViewModel, bookmarks : List<Bookmark>) {

    val scope = rememberCoroutineScope()

    Surface {
        BookmarksListView(
            geoViewProxy = mapViewModel.mapViewProxy,
            bookmarks = remember { mutableStateListOf(*bookmarks.toTypedArray()) },
            onBookmarkSelected = { bookmark ->
                scope.launch {
                    mapViewModel.bookmarkSelected.value = bookmark
                }
            }
        )
    }
}
