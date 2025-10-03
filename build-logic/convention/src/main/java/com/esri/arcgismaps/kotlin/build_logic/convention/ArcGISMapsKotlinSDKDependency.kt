/*
 *
 *  Copyright 2025 Esri
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

package com.esri.arcgismaps.kotlin.build_logic.convention

import com.esri.arcgismaps.kotlin.build_logic.extensions.api
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import org.gradle.kotlin.dsl.dependencies

/**
 * A helper object to configure the logic for applying the ArcGIS Maps SDK dependency,
 * allowing for simple one-line calls in convention plugins or build scripts.
 * It also handles dynamic versioning based on Gradle properties.
 */
object ArcGISMapsKotlinSDKDependency {

    /**
     * Applies the ArcGIS Maps SDK for Kotlin dependency to the project.
     *
     * @param target The Gradle Project to which the dependency will be added.
     */
    fun configureArcGISMapsDependencies(target: Project) {
        target.dependencies {
            val sdkVersionNumber = target.rootProject.properties["versionNumber"].toString()
            // ArcGIS Maps SDK dependency with build override support
            if (sdkVersionNumber.isNotBlank() && sdkVersionNumber != "0.0.0") {
                // Use centralized version provider
                val dependencyVersion = VersionProvider.artifactVersionProvider(target).get()
                api("com.esri:arcgis-maps-kotlin:$dependencyVersion")
            } else {
                // Use libs.versions.toml if no gradle property is provided - for `main` branch.
                api(target.libs.findLibrary("arcgis-maps-kotlin").get())
            }
        }
    }
}
