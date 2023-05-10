# ArcGIS Maps SDK for Kotlin Toolkit


## Toolkit Components

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

The `bom` folder supports the release of a "bill of materials" (`BOM`). The releasable BOM is versioned and represents
a set of versions of the toolkit components which are compatible with one another. For example, a user may specify dependencies as follows

```
implementation(platform('com.esri:arcgis-maps-kotlin-toolkit-bom:0.1'))
implementation('com.esri:arcgis-maps-kotlin-toolkit-authenticator')
implementation('com.esri:arcgis-maps-kotlin-toolkit-compass')
```

Only the bom is versioned above. The components versions are defined in the bom's pom file.

The template and TemplateApp modules are for bootstrapping new modules and will be removed when there are more modules.

## Requirements
* ArcGIS Maps SDK for Kotlin    
  See [this guide](https://developers.arcgis.com/kotlin/install-and-set-up/) for complete instructions and
other options for installing the SDK.

## Issues

Find a bug or want to request a new feature enhancement? Please let us know by [submitting an issue](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/issues/new).

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Licensing

Copyright 2019-2022 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [LICENSE](LICENSE) file.
