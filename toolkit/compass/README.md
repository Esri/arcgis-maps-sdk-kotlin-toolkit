# Compass
The Compass displays the current viewpoint rotation of a MapView by displaying a compass icon that points north.
The Compass supports resetting the rotation by tapping the icon, which resets the map to it's default 0 degree orientation.

By default the Compass is set to auto hide, hence it will only appear when the map is rotated and is hidden when the current map orientation is 0 degrees. This behavior is configurable.

Note that the MapView auto-snaps back to north when it's within a threshold of north, and in that case the compass also auto hides.

![Screenshot](screenshot.jpg)

## Features

Compass:
- Built with Jetpack Compose.
- Automatically hides when the rotation is zero.
- Can be configured to be always visible.
- Will reset the map rotation to North when tapped.

## Behavior

The `autoHide` property can be used to configure the visibility behavior of the compass. With `autoHide` enabled, whenever the map is not oriented north (non-zero orientation)
the compass appears. When reset to north, it disappears. When `autoHide` is disabled, the compass is always visible.

## Usage

### Basic usage for displaying a `Compass` on a `MapView`

The simplest workflow is to add the `Compass` composable as a child element to the content of a `ComposableMap`. 
The tap action is provided as a callback using the `onClick()` lambda. This can be used to reset the rotation of the MapView.

```kotlin
// create an ArcGISMap
val map = ArcGISMap(BasemapStyle.ArcGISImagery)

// create a MapInterface
val mapInterface = MapInterface(map)

// get the current map rotation from the MapInterface and hoist it as a state
val mapRotation by mapInterface.mapRotation.collectAsState(flowType = DuplexFlow.Type.Read)

// show a composable map using the MapInterface
ComposableMap(
    modifier = Modifier.fillMaxSize(),
    mapInterface = mapInterface
) {
    Row(modifier = Modifier
        .height(IntrinsicSize.Max)
        .fillMaxWidth()
        .padding(25.dp)) {

        // show the compass and pass the current mapRotation
        Compass(rotation = mapRotation) {
            // reset the ComposableMap viewpoint rotation to point north using the MapInterface
            mapInterface.setViewpointRotation(0.0)
        }
    }
}
```

## Example
To see it in action, try out the [Compass micro-app](../../microapps/CompassApp) and refer to [MainScreen.kt](../../microapps/CompassApp/app/src/main/java/com/arcgismaps/toolkit/compassapp/screens/MainScreen.kt) in the project.
