package com.arcgis.toolkit.bookmarks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Bookmark
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.bookmarks.BookmarksListView
import com.arcgismaps.toolkit.geoviewcompose.GeoViewProxy
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

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
 * Composable Scenarios for the [BookmarksTests]
 *
 * @since 300.2.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScenario(mapViewModel: MapViewModel, bookmarks : List<Bookmark>) {

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = false
        )
    )

    BottomSheetScaffold(
        sheetContent = {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "trace tool",
                modifier = Modifier.heightIn(min = 0.dp, max = 350.dp)
            ) {
                BookmarksListView(
                    geoViewProxy = mapViewModel.mapViewProxy,
                    bookmarks = remember { mutableStateListOf(*bookmarks.toTypedArray()) },
                    onBookmarkSelected = { bookmark ->
                        scope.launch {
                            mapViewModel.bookmarkSelected.value = bookmark
                            scaffoldState.bottomSheetState.hide()
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 100.dp,
        sheetSwipeEnabled = true,
        topBar = null
    ) {
        Column {
            MapView(
                arcGISMap = mapViewModel.map,
                mapViewProxy = mapViewModel.mapViewProxy,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
