# Display scenes in augmented reality

Augmented reality experiences are designed to "augment" the physical world with virtual content that respects real world scale, position, and orientation of a device. In the case of AR in the Kotlin Toolkit, a SceneView displays 3D geographic data as virtual content on top of a camera feed which represents the real, physical world.

The Augmented Reality (AR) toolkit module allows quick and easy integration of AR into your application with `@Composable` components that render a `SceneView` in augmented reality using [ARCore](https://github.com/google-ar/arcore-android-sdk).

View the API Reference for the AR module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.ar/index.html).

## TableTopSceneView

The `TableTopSceneView` component renders `ArcGISScene` content anchored to a physical surface, as if it were a 3D-printed model.

<TODO - screen shot>

### Behaviour

To see it in action, check out the microapp <TODO - link>.

### Features

- A composable function that displays a camera feed overlayed by a [SceneView](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/blob/main/toolkit/geoview-compose/src/main/java/com/arcgismaps/toolkit/geoviewcompose/SceneView.kt).
- Detects physical horizontal surfaces in the camera feed, which a user can select by tapping on the screen. The tap location determines where the scene data is anchored on the detected surface.
- Provides parameters specific to table top scenarios to configure the placement and visualization of scene data:
    - `arcGISSceneAnchor` - A point in the `SceneView` to use as the anchor point of the scene data on the selected physical surface
    - `translationFactor` - Determines how many meters the scene view translates as the device moves. A useful formula for determining this value is `translation factor = virtual content width / desired physical content width`. The virtual content width is the real-world size of the scene content and the desired physical content width is the physical table top width. The virtual content width is determined by the clipping distance in meters around the camera. For example, in order to setup a table top scene where scene data should be displayed by up to 500 meter around the `arcGISSceneAnchor` and this data should be placed on a physical table that is 1 meter wide: `translation factor = 500 meter / 1 meter`.
    - `clippingDistance` - The distance in meters that the ArcGIS Scene data will be clipped around the `arcGISSceneAnchor`.
- Provides parameters to configure and interact with the `SceneView`, such specifying an `ArcGISScene`, graphics overlays, lighting etc.
- A `TableTopSceneViewProxy` can be passed to the `TableTopSceneView` composable function to perform operations such as identify, setting a viewpoint etc.
- A `TableTopSceneViewScope` provided as the receiver by the `TableTopSceneView`'s `content` lambda can be used to display a callout.

### Usage

TODO