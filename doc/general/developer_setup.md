# Developer Setup

Requirements: 

* [Android Studio](https://developer.android.com/studio)
* [Android SDK Platform 37](https://developer.android.com/studio/intro/update#sdk-manager)
* [JDK 17](https://adoptium.net/temurin/releases/)

and an internet connection to download dependencies expressed in gradle.

For production use, [API Key authentication](https://developers.arcgis.com/documentation/security-and-authentication/api-key-authentication/) or [user authentication](https://developers.arcgis.com/documentation/security-and-authentication/user-authentication/) is required to access the ArcGIS location services provided by the ArcGIS Maps SDK for Kotlin toolkit components. Sign up for an ArcGIS account [here](https://developers.arcgis.com/kotlin/get-started/#1-sign-up-for-an-account).

### Get Started

Clone this project from github, and open it in Android Studio. Choose any of the templates, build and run it on a device or in an emulator.

### Package structure
The top level folder structure looks like this
<pre>
arcgis-maps-sdk-kotlin-toolkit
├── bom
├── build
├── doc
├── gradle
├── microapps
└── toolkit
</pre>

The actual toolkit components are releasable libraries whose modules can be found in the `toolkit` folder.

<pre>
toolkit
├── ar
├── authentication
├── basemapgallery
├── compass
├── featureforms
├── geoview-compose
├── indoors
├── legend
├── offline
├── popup
├── scalebar
├── utilitynetworks
└── template
</pre>

These libraries have package names like `com.arcgismaps.toolkit.authentication`. Note that the `template` module is used internally and is not released.

The `microapps` folder contains example apps for the toolkit components and composable GeoViews:

<pre>
microapps
├── ArFlyoverApp
├── ArTabletopApp
├── ArWorldScaleApp
├── AuthenticationApp
├── BasemapGalleryApp
├── CalloutApp
├── CompassApp
├── FeatureFormsApp
├── FloorFilterApp
├── LegendApp
├── LocalSceneViewApp
├── MapViewGeometryEditorApp
├── MapViewIdentifyApp
├── MapViewInsetsApp
├── MapViewLocationDisplayApp
├── MapViewSetViewpointApp
├── OfflineMapAreasApp
├── OverviewMapApp
├── PopupApp
├── ScalebarApp
├── SceneViewAnalysisOverlayApp
├── SceneViewCameraControllerApp
├── SceneViewLightingOptionsApp
├── SceneViewSetViewpointApp
└── UtilityNetworkTraceApp
</pre>

The apps have package names such as `com.arcgismaps.toolkit.featureformsapp`. The folder also
contains `MicroappsLib`, a shared library, and `TemplateApp`, which is used to bootstrap new
microapps.

### Creating new toolkit components

To add a new toolkit component, use the  "new component starter" script. It can be found at the top level of the repo.

<pre>
================================================================================
Usage: new-component-starter.sh -n component-name

Description: generates a new toolkit component and microapp with the given name
 -n <name> the name of the new toolkit component
 -h        this help message
 ./new-component-starter.sh -n FloorFilter
================================================================================
</pre>

This script will 
* copy the `template` toolkit component module, and the `TemplateApp` microapp into new modules with the name provided to the script.
* update `settings.gradle.kts` to add these two new modules.

This script requires bash 4.0.
