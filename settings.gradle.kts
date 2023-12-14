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
include(":template")
include(":template-app")
include(":authentication")
include(":authentication-app")
include(":compass")
include(":compass-app")
include(":composable-map")
include(":indoors")
include(":floor-filter-app")

project(":authentication").projectDir = File(rootDir, "toolkit/authentication")
project(":authentication-app").projectDir = File(rootDir, "microapps/AuthenticationApp/app")
project(":bom").projectDir = File(rootDir, "bom")
project(":compass").projectDir = File(rootDir, "toolkit/compass")
project(":compass-app").projectDir = File(rootDir, "microapps/CompassApp/app")
project(":composable-map").projectDir = File(rootDir, "toolkit/composable-map")
project(":indoors").projectDir = File(rootDir, "toolkit/indoors")
project(":floor-filter-app").projectDir = File(rootDir, "microapps/FloorFilterApp/app")
project(":template").projectDir = File(rootDir, "toolkit/template")
project(":template-app").projectDir = File(rootDir, "microapps/TemplateApp/app")

