# OverviewMap

The Overview Map is a small, secondary Map View (sometimes called an "inset map"), that can be
superimposed on an existing Map View or Scene View, which shows a representation of the main view's
current viewpoint.

![Screenshot](mapview_screenshot.png) ![Screenshot](sceneview_screenshot.png)

### Basic usage for displaying an Overview Map

A simple workflow is to display the Overview Map on top of a Map View and use the viewpoint and
visible area callbacks to update the overview.

There are two overloads of the Overview Map - one to use when creating an overview of a Map View and
the other to use when creating an overview of a Scene View.

Use the following code to create the UI for a Map View overview

```kotlin
val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }
val visibleArea: MutableState<Polygon?> = remember { mutableStateOf(null) }

Box {
    MapView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = remember {
            ArcGISMap(BasemapStyle.ArcGISDarkGray)
        },
        onViewpointChangedForCenterAndScale = {
            viewpoint.value = it
        },
        onVisibleAreaChanged = {
            visibleArea.value = it
        }
    )
    OverviewMap(
        viewpoint = viewpoint.value,
        visibleArea = visibleArea.value,
        modifier = Modifier
            .size(250.dp, 200.dp)
            .padding(20.dp)
            .align(Alignment.TopEnd)
    )
}
```

or, for a Scene View overview

```kotlin
val viewpoint: MutableState<Viewpoint?> = remember { mutableStateOf(null) }

Box {
    SceneView(
        modifier = Modifier.fillMaxSize(),
        arcGISScene = remember {
            ArcGISScene(BasemapStyle.ArcGISDarkGray)
        },
        onViewpointChangedForCenterAndScale = {
            viewpoint.value = it
        },
    )
    OverviewMap(
        viewpoint = viewpoint.value,
        modifier = Modifier
            .size(250.dp, 200.dp)
            .padding(20.dp)
            .align(Alignment.TopEnd)
    )
}
```

Note that the overloads for `OverviewMap` can take a `Symbol` that defines how the parent view's
visible map is symbolized in the overview. Any symbol provided must be suitable for a polygon
geometry if the overview is for a Map View and suitable for a point geometry if the overview is for
a Scene View.

## Example

To see it in action, try out the [Overview Map micro-app](../../microapps/OverviewMapApp) and refer
to [MainScreen.kt](../../microapps/OverviewMapApp/app/src/main/java/com/arcgismaps/toolkit/overviewmapapp/screens/MainScreen.kt)
in the project.