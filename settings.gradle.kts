/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// The version of the ArcGIS Maps SDK for Kotlin dependency
val sdkVersionNumber: String by settings
// The build number of the ArcGIS Maps SDK for Kotlin dependency
val sdkBuildNumber: String by settings

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven {
            url = java.net.URI(
                "https://esri.jfrog.io/artifactory/arcgis"
            )
        }
        maven {
            url = java.net.URI(
                "https://olympus.esri.com/artifactory/arcgisruntime-repo/"
            )
        }
    }
    
    versionCatalogs {
        create("arcgis") {
            val versionAndBuild = if (sdkBuildNumber.isNotEmpty()) {
                "$sdkVersionNumber-$sdkBuildNumber"
            } else {
                sdkVersionNumber
            }
            version("mapsSdk", versionAndBuild)
            library("mapsSdk", "com.esri", "arcgis-maps-kotlin").versionRef("mapsSdk")
        }
    }
}

include(":bom")
project(":bom").projectDir = File(rootDir, "bom")
include(":kdoc")
include(":composable-map")
project(":composable-map").projectDir = File(rootDir, "toolkit/composable-map")
include(":indoors")
project(":indoors").projectDir = File(rootDir, "toolkit/indoors")
include(":floor-filter-app")
project(":floor-filter-app").projectDir = File(rootDir, "microapps/FloorFilterApp/app")
include(":geoview-compose")
project(":geoview-compose").projectDir = File(rootDir, "toolkit/geoview-compose")
include(":map-view-location-display-app")
project(":map-view-location-display-app").projectDir = File(rootDir, "microapps/mapviewlocationdisplayapp/app")
include(":map-view-insets-app")
project(":map-view-insets-app").projectDir = File(rootDir, "microapps/mapviewinsetsapp/app")
include(":map-view-geometry-editor-app")
project(":map-view-geometry-editor-app").projectDir = File(rootDir, "microapps/mapviewgeometryeditorapp/app")
include(":map-view-set-viewpoint-app")
project(":map-view-set-viewpoint-app").projectDir = File(rootDir, "microapps/mapviewsetviewpointapp/app")
include(":map-view-identify-app")
project(":map-view-identify-app").projectDir = File(rootDir, "microapps/mapviewidentifyapp/app")
include(":scene-view-analysis-overlay-app")
project(":scene-view-analysis-overlay-app").projectDir = File(rootDir, "microapps/sceneviewanalysisoverlayapp/app")
include(":scene-view-set-viewpoint-app")
project(":scene-view-set-viewpoint-app").projectDir = File(rootDir, "microapps/sceneviewsetviewpointapp/app")
include(":scene-view-camera-controller-app")
project(":scene-view-camera-controller-app").projectDir = File(rootDir, "microapps/sceneviewcameracontrollerapp/app")
include(":scene-view-lighting-options-app")
project(":scene-view-lighting-options-app").projectDir = File(rootDir, "microapps/sceneviewlightingoptionsapp/app")
