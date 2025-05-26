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

// For finalBuilds ignore the build number and pick up the released version of the SDK dependency
val finalBuild: Boolean = (providers.gradleProperty("finalBuild").orNull ?: "false")
    .run { this == "true" }

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

// The version of the ArcGIS Maps SDK for Kotlin dependency.
// First look for the version number provided via command line (for CI builds), if not found,
// take the one defined in gradle.properties.
// CI builds pass -PversionNumber=${BUILDVER}
val sdkVersionNumber: String =
    providers.gradleProperty("versionNumber").orNull
        ?: providers.gradleProperty("sdkVersionNumber").orNull
        ?: throw IllegalStateException("sdkVersionNumber must be set either via command line or in gradle.properties")

// The build number of the ArcGIS Maps SDK for Kotlin dependency.
// First look for the version number provided via command line (for CI builds), if not found,
// take the one defined in local.properties.
// CI builds pass -PbuildNumber=${BUILDNUM}
val sdkBuildNumber: String =
    providers.gradleProperty("buildNumber").orNull
        ?: localProperties.getProperty("sdkBuildNumber")
        ?: ""

// The Artifactory credentials for the ArcGIS Maps SDK for Kotlin repository.
// First look for the credentials provided via command line (for CI builds), if not found,
// take the one defined in local.properties.
// CI builds pass -PartifactoryURL=${ARTIFACTORY_URL} -PartifactoryUsername=${ARTIFACTORY_USER} -PartifactoryPassword=${ARTIFACTORY_PASSWORD}
val artifactoryUrl: String =
    providers.gradleProperty("artifactoryUrl").orNull
        ?: localProperties.getProperty("artifactoryUrl")
        ?: ""

val artifactoryUsername: String =
    providers.gradleProperty("artifactoryUsername").orNull
        ?: localProperties.getProperty("artifactoryUsername")
        ?: ""

val artifactoryPassword: String =
    providers.gradleProperty("artifactoryPassword").orNull
        ?: localProperties.getProperty("artifactoryPassword")
        ?: ""

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
        if (!artifactoryUrl.isBlank()) {
            maven {
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
    }

    versionCatalogs {
        create("arcgis") {
            val versionAndBuild = if (finalBuild) {
                logger.warn(
                    "Requested release candidate for the SDK dependency $sdkVersionNumber"
                )
                sdkVersionNumber
            } else {
                if (sdkBuildNumber.isBlank()) {
                    logger.warn("Maps SDK dependency: $sdkVersionNumber")
                    sdkVersionNumber
                } else {
                    logger.warn("Maps SDK dependency: $sdkVersionNumber-$sdkBuildNumber")
                    "$sdkVersionNumber-$sdkBuildNumber"
                }
            }

            version("mapsSdk", versionAndBuild)
            library("mapsSdk", "com.esri", "arcgis-maps-kotlin").versionRef("mapsSdk")
        }
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
include(":composable-map")
project(":composable-map").projectDir = File(rootDir, "toolkit/composable-map")
include(":indoors")
project(":indoors").projectDir = File(rootDir, "toolkit/indoors")
include(":floor-filter-app")
project(":floor-filter-app").projectDir = File(rootDir, "microapps/FloorFilterApp/app")
include(":geoview-compose")
project(":geoview-compose").projectDir = File(rootDir, "toolkit/geoview-compose")
include(":map-view-callout-app")
project(":map-view-callout-app").projectDir = File(rootDir, "microapps/MapViewCalloutApp/app")
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
include(":scene-view-callout-app")
project(":scene-view-callout-app").projectDir = File(rootDir, "microapps/SceneViewCalloutApp/app")
include(":scene-view-analysis-overlay-app")
project(":scene-view-analysis-overlay-app").projectDir = File(rootDir, "microapps/sceneviewanalysisoverlayapp/app")
include(":scene-view-set-viewpoint-app")
project(":scene-view-set-viewpoint-app").projectDir = File(rootDir, "microapps/sceneviewsetviewpointapp/app")
include(":scene-view-camera-controller-app")
project(":scene-view-camera-controller-app").projectDir = File(rootDir, "microapps/sceneviewcameracontrollerapp/app")
include(":scene-view-lighting-options-app")
project(":scene-view-lighting-options-app").projectDir = File(rootDir, "microapps/sceneviewlightingoptionsapp/app")
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
include(":ar-flyover-app")
project(":ar-flyover-app").projectDir = File(rootDir, "microapps/ArFlyoverApp/app")

