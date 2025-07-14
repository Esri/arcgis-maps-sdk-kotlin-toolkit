# ArcGIS Maps SDK for Kotlin Toolkit

The ArcGIS Maps SDK for Kotlin Toolkit contains components that will simplify your Kotlin app development. It is built off of the new ArcGIS Maps SDK for Kotlin.

## Toolkit Components

* **[Authenticator](toolkit/authentication)** - Displays a user interface when network and ArcGIS authentication challenges occur.
* **[Basemap Gallery](toolkit/basemapgallery)** - Provides a control for selecting basemaps
* **[Compass](toolkit/compass)** - Shows a compass direction when the map is rotated. Auto-hides when the map points north.
* **[FeatureForms](toolkit/featureforms)** - Provides a UI for editing feature attributes.
* **[FloorFilter](toolkit/indoors)** - Allows filtering of floor plan data in a geo view by a site, a facility in the site, or a floor in the facility.
* **[GeoView-Compose](toolkit/geoview-compose)** - Compose wrappers for the MapView and SceneView.
  * **[Callout](toolkit/geoview-compose#display-a-callout)** - Draws a callout on the GeoView to display Composable content.
  * **[OverviewMap](toolkit/geoview-compose#display-an-overviewmap)** - a small, secondary Map View which shows a representation of the main view's current viewpoint.
* **[Legend](toolkit/legend)** - Displays a legend for a map or a scene.  
* **[OfflineMapAreas](toolkit/offline)** - Allows you to take a web map offline by downloading map areas.
* **[Popup](toolkit/popup)** - View field values of features in a layer using the Popup API.
* **[Scalebar](toolkit/scalebar)** - Displays current scale reference.
* **[UtilityNetworkTrace](toolkit/utilitynetworks)** - Configure, run, and visualize UtilityNetworkTraces on a composable MapView.
* **[Augmented Reality](toolkit/ar)** - Provides components to "augment" the physical world with virtual content.

## API Reference

View the API reference for the toolkit [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/index.html).

## Requirements

Please see the [developer setup](doc/general/developer_setup.md) for a complete description of requirements
* ArcGIS Maps SDK for Kotlin    
  See [this guide](https://developers.arcgis.com/kotlin/install-and-set-up/) for complete instructions and
other options for installing the SDK.
* JDK 17

The *ArcGIS Maps SDK for Kotlin Toolkit* has a *Minimum SDK* version of *26*, meaning that it can run on devices with *Android Oreo (8.0)* or newer.

## Installation

### Using the BOM

Add the maven repository to your project's gradle file:

```
repositories {
    maven {
        url = uri("https://esri.jfrog.io/artifactory/arcgis")
    }
}
```

The *ArcGIS Maps SDK for Kotlin Toolkit* is released with a "bill of materials" (`BOM`). The releasable BOM is versioned and represents a set of versions of the toolkit components which are compatible with one another. You may specify dependencies as follows

```
implementation(platform("com.esri:arcgis-maps-kotlin-toolkit-bom:200.8.0"))
implementation("com.esri:arcgis-maps-kotlin-toolkit-authentication")
implementation("com.esri:arcgis-maps-kotlin-toolkit-basemapgallery")
implementation("com.esri:arcgis-maps-kotlin-toolkit-compass")
implementation("com.esri:arcgis-maps-kotlin-toolkit-featureforms")
implementation("com.esri:arcgis-maps-kotlin-toolkit-geoview-compose")
implementation("com.esri:arcgis-maps-kotlin-toolkit-legend")
implementation("com.esri:arcgis-maps-kotlin-toolkit-offline")
implementation("com.esri:arcgis-maps-kotlin-toolkit-indoors")
implementation("com.esri:arcgis-maps-kotlin-toolkit-popup")
implementation("com.esri:arcgis-maps-kotlin-toolkit-scalebar")
implementation("com.esri:arcgis-maps-kotlin-toolkit-utilitynetworks")
implementation("com.esri:arcgis-maps-kotlin-toolkit-ar")
```

The template and TemplateApp modules are for bootstrapping new modules.

Please see the [package structure](doc/general/developer_setup.md#package-structure) documentation for more details.

## Accessing Esri location services

Some toolkit components may require access to Esri location services, including basemaps, routing,
and geocoding, which requires authentication using either an API Key or an ArcGIS identity. The
toolkit code is set up to easily use an API key.

### API key

A long-lived access token that gives your application access to ArcGIS location services. Go to
the [Create an API key](https://links.esri.com/create-an-api-key) tutorial to obtain a new API key
access token. Ensure that the following privileges are enabled:

* **Location services** > **Basemaps**
* **Location services** > **Geocoding**
* **Location services** > **Routing**

The components in this repository have been structured to use an access token, set once.
Set your access token in the `local.properties` in the same folder as the
`secrets.defaults.properties` as shown below.

```gradle
API_KEY=YOUR_ACCESS_TOKEN
```

### ArcGIS identity

An ArcGIS named user account that is a member of an organization in ArcGIS Online or ArcGIS
Enterprise can be used as an alternative to API keys. Most toolkit micro apps don't make use of
named user authentication.
See [User authentication](https://developers.arcgis.com/kotlin/security-and-authentication/#user-authentication)
for more information.

## Issues

Find a bug or want to request a new feature enhancement? Please let us know by [submitting an issue](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/issues/new).

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

### Creating a New Toolkit Component

A new toolkit component can be added to this project by running the bash script at the top level
`./new-component-starter.sh`

This script will prompt for you to input a name for the new component and ask if you want to create a new app module for it.
Capitalization isn't necessary, but it won't capitalize anything but the first letter if none is specified.

A new microapp can be added by running the bash script `./new-microapp-starter.sh`. 
This script will prompt for you to input a name for the new microapp.

This script requires bash 4.0 or higher.

### About this Repo

The project is structured into two folders, `microapps` and `toolkit` each containing many modules, and a folder for releasing a bill of materials (BOM).

```
arcgis-maps-sdk-kotlin-toolkit
├── bom
│   └── build.gradle.kts
├── microapps
│   └── TemplateApp
│   ├── ...App
├── toolkit
│   ├── template
│   ├── ...
├── build.gradle.kts
└── settings.gradle.kts
```
The `toolkit` folder contains releasable libraries -- these are the components of the toolkit.
For each module in the `toolkit` folder there is a corresponding app in the `microapps` folder which depends on it.
The apps are not released but are useful for working on a particular component in isolation.

The template and TemplateApp modules are for bootstrapping new modules.

Please see the [package structure](doc/general/developer_setup.md#package-structure) documentation for more details.

### Testing

#### Running Local Tests

In order to run *local* (non-instrumented) tests of all modules and get an aggregated test report, run the following at the root folder of the project:
```
./gradlew testAggregatedReport --continue
```
The test report for local tests can be located under `arcgis-maps-sdk-kotlin-toolkit/build/reports`.

#### Running Connected Tests

In order to run *connected* (instrumented) tests of all modules and get the test reports in a centralized folder, run the following at the root folder of the project:
```
./gradlew connectedDebugAndroidTest --continue
```
The test reports for connected tests can be located under `arcgis-maps-sdk-kotlin-toolkit/connectedTestReports`.

## Licensing

Copyright 2019-2022 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [LICENSE](LICENSE) file.
