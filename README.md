# ArcGIS Maps SDK for Kotlin Toolkit

The ArcGIS Maps SDK for Kotlin Toolkit contains components that will simplify your Kotlin app development. It is built off of the new ArcGIS Maps SDK for Kotlin.

## Toolkit Components

* **[Authenticator](toolkit/authentication)** - Displays a user interface when network and ArcGIS authentication challenges occur.
* **[Compass](toolkit/compass)** - Shows a compass direction when the map is rotated. Auto-hides when the map points north.
* **[FeatureForms](toolkit/featureforms)** - Provides a UI for editing feature attributes.
* **[GeoView-Compose](toolkit/geoview-compose)** - Compose wrappers for the MapView and SceneView.
* **[FloorFilter](toolkit/indoors)** - Allows filtering of floor plan data in a geo view by a site, a facility in the site, or a floor in the facility.

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
implementation(platform('com.esri:arcgis-maps-kotlin-toolkit-bom:200.4.0'))
implementation('com.esri:arcgis-maps-kotlin-toolkit-authentication')
implementation('com.esri:arcgis-maps-kotlin-toolkit-compass')
implementation('com.esri:arcgis-maps-kotlin-toolkit-indoors')
```

The template and TemplateApp modules are for bootstrapping new modules.

Please see the [package structure](doc/general/developer_setup.md#package-structure) documentation for more details.

## Issues

Find a bug or want to request a new feature enhancement? Please let us know by [submitting an issue](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/issues/new).

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

### Creating a New Toolkit Component

A new toolkit component can be added to this project by running the bash script at the top level
`./new-component-starter.sh -n NameOfNewComponent"`

This will create a new library module for the new component, and a new app module which depends on the library.
Capitalization isn't necessary, but it won't capitalize anything but the first letter if none is specified.

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

## Licensing

Copyright 2019-2022 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [LICENSE](LICENSE) file.
