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
    includeBuild("build-logic")
    includeBuild("gradle-plugins")
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
}

// This enables the "Type Safe Project Accessors" feature
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Dynamically include all toolkit components
File("toolkit").listFiles()?.filter { it.isDirectory }?.forEach { toolkitModule ->
    include(":${toolkitModule.name}")
    project(":${toolkitModule.name}").projectDir = file("toolkit/${toolkitModule.name}")
}

// Dynamically include all microapp components
File("microapps").listFiles()?.filter { it.isDirectory && !it.name.contains("MicroappsLib") }?.forEach { microappFolder ->
    val appDir = File(microappFolder, "app")
    if (appDir.exists() && appDir.isDirectory) {
        val microappName = microappFolder.name.toLowerCase().replace("app", "-app")
        include(":$microappName")
        project(":$microappName").projectDir = appDir
    }
}

// Add any other specific includes here
include(":bom")
project(":bom").projectDir = File(rootDir, "bom")
include(":kdoc")
include(":microapps-lib")
project(":microapps-lib").projectDir = File(rootDir, "microapps/MicroappsLib")
