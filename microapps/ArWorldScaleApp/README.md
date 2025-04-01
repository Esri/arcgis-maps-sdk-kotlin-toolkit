# AR World Scale Micro-app

This micro-app demonstrates the use of the `WorldScaleSceneView` toolkit component which renders an `ArcGISScene` from the perspective of the device's camera at real-world scale over the device's camera feed.

![Screenshot](../../toolkit/ar/worldscale-screenshot.png)
## Prerequisites

- `WorldScaleSceneView` requires a device that [supports ARCore](https://developers.google.com/ar/devices)
- In order to use the [Geospatial](../../toolkit/ar/src/main/java/com/arcgismaps/toolkit/ar/WorldScaleTrackingMode.kt) tracking mode, an ARCore API key needs to be configured in the micro app's [manifest](app/src/main/AndroidManifest.xml). See [steps for setting up an ARCore API Key](https://developers.google.com/ar/develop/authorization?platform=android#api-key-android).

## Usage

Launch the app and wait for the scene to load. Choose to use `World` tracking or `Geospatial` tracking from the overflow menu in the top bar. In `World` tracking mode, the device's location will be determined using the device's GPS. You may need to use the Calibration View, accessible via the floating action button at the bottom of the screen, to adjust the heading and elevation of the scene. In `Geospatial` tracking mode, the device's location will be determined to high precision using [Google's ARCore Geospatial API](https://developers.google.com/ar/develop/geospatial).
Note, in `World` tracking mode, the `WorldScaleSceneView` makes use of ARCore, however, authentication with ARCore services is not required. On the other hand, using `Geospatial` tracking mode requires authentication with ARCore services, see [#Prerequisites].

For more information on the `WorldScaleSceneView` component and how it works, see its [Readme](../../toolkit/ar/README.md).
