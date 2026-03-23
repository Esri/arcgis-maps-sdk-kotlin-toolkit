/*
 *
 *  Copyright 2026 Esri
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
        // the variables are duplicated here because the pluginManagement block is evaluated before
        // the rest of the settings.gradle.kts, so it can't access the variables defined in the
        // dependencyResolutionManagement block below.
        // This is a known limitation of Gradle's settings script evaluation order.
        val localProperties = java.util.Properties().apply {
            val localPropertiesFile = file("../local.properties")
            if (localPropertiesFile.exists()) {
                load(localPropertiesFile.inputStream())
            }
        }

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

        if (!artifactoryUrl.isBlank()) {
            maven {
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = file("../local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

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
    repositories {
        if (!artifactoryUrl.isBlank()) {
            maven {
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic-internal"
include(":convention")
