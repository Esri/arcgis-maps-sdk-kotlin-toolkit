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

import com.esri.arcgismaps.kotlin.build_logic.extensions.getExtraProperty
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.internal.cc.base.logger

/**
 * Centralized version provider that implements the toolkit's versioning strategy.
 * This provider ensures consistent versioning across Kotlin ArcGISMapsSDK, BOM, KDoc, and artifact publication.
 */
object VersionProvider {

    /**
     * Version configuration for toolkit artifacts
     */
    data class VersionConfig(
        val version: String,
        val buildNumber: String?,
        val isFinalBuild: Boolean
    ) {
        /**
         * Returns the full version string for artifact publication
         */
        val artifactVersion: String
            get() = if (isFinalBuild || buildNumber.isNullOrBlank()) {
                version
            } else {
                "$version-$buildNumber"
            }
    }

    private fun resolveVersionNumber(
        rootProject: Project,
        isInternal: Boolean
    ): String {
        if (isInternal) {
            rootProject.getExtraProperty("versionNumber")
                ?.takeIf { it.toString().isNotBlank() }
                ?.let { return it.toString() }
        }
        rootProject.getExtraProperty("sdkVersionNumber")
            ?.takeIf { it.toString().isNotBlank() }
            ?.let { return it.toString() }
        // Fallback to version catalog
        return rootProject.libs.findVersion("arcgisMapsKotlinVersion").get().toString()
    }

    private fun resolveBuildNumber(rootProject: Project, isInternal: Boolean): String? {
        if (isInternal) {
            rootProject.getExtraProperty("buildNumber")
                ?.takeIf { it.toString().isNotBlank() }
                ?.let { return it.toString() }
        }
        rootProject.getExtraProperty("sdkBuildNumber")
            ?.takeIf { it.toString().isNotBlank() }
            ?.let { return it.toString() }
        return null
    }

    private fun createVersionConfig(
        rootProject: Project,
        isInternal: Boolean
    ): VersionConfig {
        // Check if this is a final build
        val finalBuild = rootProject.providers
            .gradleProperty("finalBuild")
            .orNull?.toBoolean() ?: false

        // Version resolution
        val versionNumber = resolveVersionNumber(rootProject, isInternal)
        val buildNumber = if (!finalBuild) resolveBuildNumber(rootProject, isInternal) else null

        return VersionConfig(
            version = versionNumber,
            buildNumber = buildNumber,
            isFinalBuild = finalBuild
        )
    }

    /**
     * Provides a version configuration provider for the given project.
     */
    private fun versionConfigProvider(
        rootProject: Project,
        isInternal: Boolean = false
    ): Provider<VersionConfig> {
        return rootProject.providers.provider {
            createVersionConfig(rootProject, isInternal)
        }
    }

    /**
     * Provides the artifact version string for the root project.
     * This is a convenience method for simple version string access.
     */
    fun artifactVersionProvider(
        project: Project,
        isInternal: Boolean = false
    ): Provider<String> {
        return versionConfigProvider(project.rootProject, isInternal).map { it.artifactVersion }
    }
}
