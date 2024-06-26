#  FloorFilter

The FloorFilter component simplifies visualization of GIS data for a specific floor of a building in your application. 
It allows you to filter down the floor aware data displayed in your GeoView to a site, a building in the site, or a floor
in the building. 

![image](screenshot.png)

*View the API Reference for the `indoors` module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.indoors/index.html).*

##  Workflow

The simplest workflow is for the app to instantiate a `FloorFilter` using an instance of the `FloorFilterState` to ideally display it on top of the GeoView using a `Box`.

```kotlin

Box(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
    contentAlignment = Alignment.BottomStart
) {
    MapView(
        arcgGISMap = floorAwareMap,
        modifier = Modifier.fillMaxSize(),
    )
    FloorFilter(floorFilterState = mapViewModel.floorFilterState)
}
```

Optionally, the default UI settings of the `FloorFilter` may be overridden by modifying `UIProperties` defined in the [FloorFilterState].
The `UIProperties` enable the customization of the colors, visibility, typography, and more.

```kotlin
// in the MapViewModel
private val uiProperties = UIProperties().apply {
    selectedForegroundColor = Color.Red
    typography = MaterialTheme.typography
    maxDisplayLevels = 2
    closeButtonPosition = ButtonPosition.Bottom
}
// create the floor filter state
val floorFilterState = FloorFilterState(geoModel, coroutineScope, uiProperties)
// pass the floor filter state in the compose layout
FloorFilter(floorFilterState = floorFilterState)
```

To see the FloorFilter in action, try out the [floor-filter-app](../../microapps/FloorFilterApp) micro app.
