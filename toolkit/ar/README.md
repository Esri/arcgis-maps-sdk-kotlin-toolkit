# Display scenes in Augmented Reality

Augmented reality experiences are designed to "augment" the physical world with virtual content that respects real world scale, position, and orientation of a device. In the case of AR in the Kotlin Toolkit, a SceneView displays 3D geographic data as virtual content on top of a camera feed which represents the real, physical world.

The Augmented Reality (AR) toolkit module allows quick and easy integration of AR into your application with `@Composable` components that render a `SceneView` in augmented reality using [ARCore](https://github.com/google-ar/arcore-android-sdk).

View the API Reference for the AR module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.ar/index.html). 

## TableTopSceneView

The `TableTopSceneView` composable function renders `ArcGISScene` content anchored to a physical surface, as if it were a 3D-printed model.

![Screenshot](screenshot.png)

### Features

- A composable function that displays a camera feed overlayed by a [SceneView](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/blob/main/toolkit/geoview-compose/src/main/java/com/arcgismaps/toolkit/geoviewcompose/SceneView.kt).
- Detects physical horizontal surfaces in the camera feed, which a user can select by tapping on the screen. The tap location determines where the scene data is anchored on the detected surface.
- Provides parameters specific to table top scenarios to configure the placement and visualization of scene data:
    - `arcGISSceneAnchor` - A point in the `SceneView` to use as the anchor point of the scene data on the selected physical surface
    - `translationFactor` - Determines how many meters the scene camera moves as the device moves. A useful formula for determining this value is `translation factor = virtual content width / desired physical content width`. The virtual content width is the real-world size of the scene content and the desired physical content width is the physical table top width. The virtual content width is determined by the clipping distance in meters around the camera. For example, in order to setup a table top scene where scene data should be displayed within a 400 meter radius around the `arcGISSceneAnchor` and be placed on a table top that is 1 meter wide: `translation factor = 400 meter / 1 meter`.
    - `clippingDistance` - The distance in meters that the ArcGIS Scene data will be clipped around the `arcGISSceneAnchor`.
- Provides parameters to configure and interact with the `SceneView`, such as specifying an `ArcGISScene`, graphics overlays, lighting etc.
- A `TableTopSceneViewProxy` can be passed to the `TableTopSceneView` composable function to perform operations such as identify.
- A `TableTopSceneViewScope` provided as the receiver by the `TableTopSceneView`'s `content` lambda can be used to display a callout.

### Prerequisites

`TableTopSceneView` requires an [ARCore](https://github.com/google-ar/arcore-android-sdk) supported device that has installed Google Play Services for AR. An application must call [ArCoreApk.requestInstall](https://developers.google.com/ar/develop/java/enable-arcore#check_if_google_play_services_for_ar_is_installed) before using the `TableTopSceneView`. For an example see how it is done in the micro app's [MainActivity](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/blob/main/microapps/ArTabletopApp/app/src/main/java/com/arcgismaps/toolkit/artabletopapp/MainActivity.kt).
Note - the `TableTopSceneView` checks for availability of ARCore when it enters the composition. If ARCore is not supported by the device or not installed, the `TableTopSceneView` will fail to initialize with `TableTopSceneViewStatus.FailedToInitialize`.

Note that apps using ARCore must comply with ARCore's user privacy requirements. See [this page](https://developers.google.com/ar/develop/privacy-requirements) for more information.

### Usage

The `TableTopSceneView` requires camera permissions, which are requested by default when the `TableTopSceneView` enters composition. The following camera-related settings need to be specified in the `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />

<!-- Limits app visibility in the Google Play Store to ARCore supported devices
    (https://developers.google.com/ar/devices). -->
<uses-feature android:name="android.hardware.camera.ar" />
<uses-feature
    android:name="android.hardware.camera"
    android:required="true" />
```

If ARCore is not optional for your application to function (as is the case with the [microapp](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/tree/main/microapps/ArTabletopApp)), you also need to add the following to your `AndroidManifest.xml`:

```xml
<!-- "AR Required" app, requires "Google Play Services for AR" (ARCore)
    to be installed, as the app does not include any non-AR features. -->
<meta-data android:name="com.google.ar.core" android:value="required" />
```

Configure an `ArcGISScene` with the data you want to render in the table top scene:

```kotlin

@Composable
fun MainScreen() {
    ...
    val arcGISScene = remember {
        ArcGISScene().apply {
            operationalLayers.add(
                ArcGISSceneLayer("https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/DevA_BuildingShells/SceneServer")
            )
            baseSurface = Surface().apply {
                elevationSources.add(
                    ElevationSource.fromTerrain3dService()
                )
            }
        }
    }
    ...
}
```

Define a location in your scene that should serve as the anchor point with the table top surface:

```kotlin
val arcGISSceneAnchor = remember {
    Point(-122.68350326165559, 45.53257485106716, 0.0, arcGISScene.spatialReference)
}
```

Determine a translation factor and clipping distance for your scene data as described above. Call the `TableTopSceneView` composable function with these parameters:

```kotlin
TableTopSceneView(
    arcGISScene = arcGISScene,
    arcGISSceneAnchor = arcGISSceneAnchor,
    translationFactor = 400.0,
    modifier = Modifier.fillMaxSize(),
    clippingDistance = 400.0,
    ...
)
```

Pass an `onInitializationStatusChanged` callback to the `TableTopSceneView` composable function to get notified about initialization status changes.

```kotlin
TableTopSceneView(
    arcGISScene = arcGISScene,
    arcGISSceneAnchor = arcGISSceneAnchor,
    translationFactor = 400.0,
    modifier = Modifier.fillMaxSize(),
    clippingDistance = 400.0,
    onInitializationStatusChanged = { status ->
       updateStatus(status)
    },
    ...
)
```

Make use of other features of a SceneView, for example handle `onSingleTapConfirmed` events and display a `Callout` at the tapped location:

```kotlin
var tappedLocation by remember { mutableStateOf<Point?>(null) }

TableTopSceneView(
    arcGISScene = arcGISScene,
    arcGISSceneAnchor = arcGISSceneAnchor,
    translationFactor = 400.0,
    modifier = Modifier.fillMaxSize(),
    clippingDistance = 400.0,
    onInitializationStatusChanged = { status ->
       updateStatus(status)
    },
    onSingleTapConfirmed = {
        val location = tableTopSceneViewProxy.screenToBaseSurface(it.screenCoordinate)
        location?.let { point ->
            tappedLocation = point
        }
    }
    ...
) {
    tappedLocation?.let {
        Callout(location = it, modifier = Modifier.wrapContentSize()) {
            Text(stringResource(R.string.lat_lon, it.y.roundToInt(), it.x.roundToInt()))
        }
    }
}
```

### Behaviour

To see it in action, check out the [microapp](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/tree/main/microapps/ArTabletopApp).
