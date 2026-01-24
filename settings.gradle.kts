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

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("arcgis") {
            library("mapsSdk", "com.esri", "arcgis-maps-kotlin").withoutVersion()
        }
    }
}

// Use local ArcGIS Maps SDK & MockingJay sources via a composite build.
includeBuild("../kotlin/android-api") {
    dependencySubstitution {
        substitute(module("com.esri:arcgis-maps-kotlin")).using(project(":arcgis-maps-kotlin"))
    }
}

// fixes https://devtopia.esri.com/runtime/kotlin/issues/3863#issuecomment-4715101
// fixes https://issuetracker.google.com/issues/315023802
gradle.startParameter.excludedTaskNames.addAll(listOf(":buildSrc:testClasses"))

include(":bom")
project(":bom").projectDir = File(rootDir, "bom")
include(":kdoc")
include(":microapps-lib")
project(":microapps-lib").projectDir = File(rootDir, "microapps/MicroappsLib")
include(":authentication-app")
project(":authentication-app").projectDir = File(rootDir, "microapps/AuthenticationApp/app")
include(":authentication")
project(":authentication").projectDir = File(rootDir, "toolkit/authentication")
include(":compass-app")
project(":compass-app").projectDir = File(rootDir, "microapps/CompassApp/app")
include(":compass")
project(":compass").projectDir = File(rootDir, "toolkit/compass")
include(":featureforms-app")
project(":featureforms-app").projectDir = File(rootDir, "microapps/FeatureFormsApp/app")
include(":featureforms")
project(":featureforms").projectDir = File(rootDir, "toolkit/featureforms")
include(":template-app")
project(":template-app").projectDir = File(rootDir, "microapps/TemplateApp/app")
include(":template")
project(":template").projectDir = File(rootDir, "toolkit/template")
include(":indoors")
project(":indoors").projectDir = File(rootDir, "toolkit/indoors")
include(":floor-filter-app")
project(":floor-filter-app").projectDir = File(rootDir, "microapps/FloorFilterApp/app")
include(":geoview-compose")
project(":geoview-compose").projectDir = File(rootDir, "toolkit/geoview-compose")
include(":callout-app")
project(":callout-app").projectDir = File(rootDir, "microapps/CalloutApp/app")
include(":map-view-location-display-app")
project(":map-view-location-display-app").projectDir = File(rootDir, "microapps/MapViewLocationDisplayApp/app")
include(":map-view-insets-app")
project(":map-view-insets-app").projectDir = File(rootDir, "microapps/MapViewInsetsApp/app")
include(":map-view-geometry-editor-app")
project(":map-view-geometry-editor-app").projectDir = File(rootDir, "microapps/MapViewGeometryEditorApp/app")
include(":map-view-set-viewpoint-app")
project(":map-view-set-viewpoint-app").projectDir = File(rootDir, "microapps/MapViewSetViewpointApp/app")
include(":map-view-identify-app")
project(":map-view-identify-app").projectDir = File(rootDir, "microapps/MapViewIdentifyApp/app")
include(":scene-view-analysis-overlay-app")
project(":scene-view-analysis-overlay-app").projectDir = File(rootDir, "microapps/SceneViewAnalysisOverlayApp/app")
include(":scene-view-set-viewpoint-app")
project(":scene-view-set-viewpoint-app").projectDir = File(rootDir, "microapps/SceneViewSetViewpointApp/app")
include(":scene-view-camera-controller-app")
project(":scene-view-camera-controller-app").projectDir = File(rootDir, "microapps/SceneViewCameraControllerApp/app")
include(":scene-view-lighting-options-app")
project(":scene-view-lighting-options-app").projectDir = File(rootDir, "microapps/SceneViewLightingOptionsApp/app")
include(":popup")
project(":popup").projectDir = File(rootDir, "toolkit/popup")
include(":popup-app")
project(":popup-app").projectDir = File(rootDir, "microapps/PopupApp/app")
include(":utility-network-trace-app")
project(":utility-network-trace-app").projectDir = File(rootDir, "microapps/UtilityNetworkTraceApp/app")
include(":utilitynetworks")
project(":utilitynetworks").projectDir = File(rootDir, "toolkit/utilitynetworks")
include(":ar")
project(":ar").projectDir = File(rootDir, "toolkit/ar")
include(":ar-tabletop-app")
project(":ar-tabletop-app").projectDir = File(rootDir, "microapps/ArTabletopApp/app")
include(":ar-worldscale-app")
project(":ar-worldscale-app").projectDir = File(rootDir, "microapps/ArWorldScaleApp/app")
include(":scalebar-app")
project(":scalebar-app").projectDir = File(rootDir, "microapps/ScalebarApp/app")
include(":scalebar")
project(":scalebar").projectDir = File(rootDir, "toolkit/scalebar")
include(":legend")
project(":legend").projectDir = File(rootDir, "toolkit/legend")
include(":legend-app")
project(":legend-app").projectDir = File(rootDir, "microapps/LegendApp/app")
include(":basemapgallery-app")
project(":basemapgallery-app").projectDir = File(rootDir, "microapps/BasemapGalleryApp/app")
include(":basemapgallery")
project(":basemapgallery").projectDir = File(rootDir, "toolkit/basemapgallery")
include(":overviewmap-app")
project(":overviewmap-app").projectDir = File(rootDir, "microapps/OverviewMapApp/app")
include(":offline")
project(":offline").projectDir = File(rootDir, "toolkit/offline")
include(":offlinemapareas-app")
project(":offlinemapareas-app").projectDir = File(rootDir, "microapps/OfflineMapAreasApp/app")
include(":ar-flyover-app")
project(":ar-flyover-app").projectDir = File(rootDir, "microapps/ArFlyoverApp/app")
include(":localsceneview-app")
project(":localsceneview-app").projectDir = File(rootDir, "microapps/LocalSceneViewApp/app")
