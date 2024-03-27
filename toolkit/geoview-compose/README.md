# GeoView-Compose

The GeoView-Compose module provides `@Composable` implementations of the `MapView` and `SceneView` with a Compose-idiomatic API.

|GeoView-Compose|
|:--:|
|![image](screenshot.png)|

*View the API Reference for the `geoview-compose` module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.geoviewcompose/index.html).*

## Features

### Display a Map

Displaying a map on the screen looks like this:

```kotlin
val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
MapView(
	modifier = Modifier.fillMaxSize(),
	arcGISMap = arcGISMap
)
```

### Respond to User Input

The composable `MapView` and `SceneView` expose gesture events as lambda callback parameters:

```kotlin
val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
MapView(
	modifier = Modifier.fillMaxSize(),
	arcGISMap = arcGISMap,
	onSingleTapConfirmed = { singleTapConfirmedEvent ->
		val x = singleTapConfirmedEvent.screenCoordinate.x
		val y = singleTapConfirmedEvent.screenCoordinate.y
		Log.i("MapView", "Single tap at $x, $y")
	}
)
```

### Set a Viewpoint

To set a viewpoint, create a `MapViewProxy` and call `setViewpoint()` on it after the `MapView` is displayed on screen:

```kotlin
val point = Point(-117.182541, 34.055569, SpatialReference.wgs84())
val scale = 170000.0
val mapViewProxy = remember { MapViewProxy() }

Button(
    onClick = {
        mapViewProxy.setViewpoint(point, scale)
    }
) {
    Text("Set Viewpoint")
}
MapView(
    modifier = Modifier.fillMaxSize(),
    arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) },
    mapViewProxy = mapViewProxy,
)
```

Note that the viewpoint of the MapView will automatically be persisted across configuration changes and process death. How this behaves can be customized by supplying the `viewpointPersistence` parameter to the `MapView`

```kotlin
MapView(
    modifier = Modifier.fillMaxSize(),
    arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) },
    viewpointPersistence = ViewpointPersistence.ByBoundingGeometry
)
```

Examples of how to use `MapViewpointOperation` and `SceneViewpointOperation` are available in the respective microapps:

- [MapView Set Viewpoint App](../../microapps/MapViewSetViewpointApp/README.md)
- [SceneView Set Viewpoint App](../../microapps/SceneViewSetViewpointApp/README.md)

### Display the Device Location

A `LocationDisplay` can be used to display the device's location as a blue dot on a `MapView`:

```kotlin
val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
val scope = rememberCoroutineScope()
val locationDisplay = rememberLocationDisplay {
	start(scope)
}
MapView(
	modifier = Modifier.fillMaxSize(),
	arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) },
	locationDisplay = locationDisplay
)
```

An example of how to display the device location is available in the [MapView Location Display App](../../microapps/MapViewLocationDisplayApp/README.md).

### Identify a Feature

To identify a feature, create a `MapViewProxy` and call `identify()` on it after the `MapView` is displayed on screen

```kotlin
val mapViewProxy = remember { MapViewProxy() }
val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
val scope = rememberCoroutineScope()
MapView(
	modifier = Modifier.fillMaxSize(),
	arcGISMap = arcGISMap,
	mapViewProxy = mapViewProxy,
	onSingleTapConfirmed = { singleTapConfirmedEvent ->
		scope.launch {
			mapViewProxy.identify(featureLayer, singleTapConfirmedEvent.screenCoordinate, 20.dp)
		}
	}
)
```

An example of how to identify features and graphics is available in the [MapView Identify App](../../microapps/MapViewIdentifyApp/README.md).

### Other Examples:

Other microapps that demonstrate various workflows with the composable `MapView` and `SceneView` are available:

- [MapView Geometry Editor App](../../microapps/MapViewGeometryEditorApp/README.md) demonstrates the use of `GeometryEditor` and `GraphicsOverlay`
- [MapView Insets App](../../microapps/MapViewInsetsApp/README.md) demonstrates the use of `Insets`
- [SceneView Analysis Overlay App](../../microapps/SceneViewAnalysisOverlayApp/README.md) demonstrates the use of `AnalysisOverlay`
- [SceneView Camera Controller App](../../microapps/SceneViewCameraControllerApp/README.md) demonstrates the use of the `CameraController`
- [SceneView Lighting Options App](../../microapps/SceneViewLightingOptionsApp/README.md) demonstrates the use of various lighting options with the `SceneView`


