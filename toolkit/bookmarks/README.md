# Bookmarks

## Description

The `BookmarksListView` composable displays the bookmarks from a map or scene. Selecting a bookmark animates the associated `GeoView` to the bookmark's viewpoint.

![Screenshot](screenshot.png)

## Features

- Displays a scrollable list of bookmarks.
- Animates the `GeoView` to a bookmark's viewpoint when it is selected.
- Provides an `onBookmarkSelected` callback for responding to selections.
- Supports dynamically updating the list through a `SnapshotStateList`.
- Shows an empty state when there are no bookmarks.
- Allows customization of the background color, text style, and layout modifier.

## Usage

*View the Toolkit API Reference for the `bookmarks` module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.bookmarks/index.html).*

`BookmarksListView` needs a `GeoViewProxy` for the map or scene and a `SnapshotStateList` of `Bookmark` objects. For example, after loading a map, its bookmarks can be copied into a state list and displayed alongside a `MapView`:

```kotlin
@Composable
fun MapWithBookmarks(arcGISMap: ArcGISMap) {
    
    // A public webmap with bookmarks
    val portalItem = PortalItem(
        Portal.arcGISOnline(Portal.Connection.Anonymous),
        "16f1b8ba37b44dc3884afc8d5f454dd2"
    )
    val arcGISMap = remember { ArcGISMap(portalItem) }
    
    val mapViewProxy = remember { MapViewProxy() }
    
    // Bookmarks from webmap
    val bookmarks = remember(arcGISMap) {
        arcGISMap.bookmarks.toMutableStateList()
    }

    // Create a container to show BookmarksListView with the composable MapView
    BottomSheetScaffold(
        sheetContent = {
            BookmarksListView(
                geoViewProxy = mapViewProxy,
                bookmarks = bookmarks,
                modifier = Modifier.weight(1f),
                onBookmarkSelected = { bookmark ->
                    // Optional responses to the selected bookmark.
                }
            )
        },
        ...
    ) { padding ->
        MapView(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            arcGISMap = arcGISMap,
            mapViewProxy = mapViewProxy
        )
    }
}
```

The map must be loaded before reading its bookmarks. To add or remove bookmarks later, update the `SnapshotStateList`; the list recomposes when its contents change. The callback is optional, and the component handles navigating the `GeoView` to the selected bookmark.

Use `backgroundColor` and `textStyle` to customize the list's appearance:

```kotlin
BookmarksListView(
    geoViewProxy = mapViewProxy,
    bookmarks = bookmarks,
    backgroundColor = MaterialTheme.colorScheme.surface,
    textStyle = MaterialTheme.typography.titleMedium
)
```
