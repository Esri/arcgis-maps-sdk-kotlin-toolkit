package com.esri.arcgismaps.kotlin.build_logic.convention

import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Centralized version provider that implements the toolkit's versioning strategy.
 * This provider ensures consistent versioning across BOM, KDoc, and artifact publication.
 */
object VersionProvider {

    /**
     * Version configuration for toolkit artifacts
     */
    data class VersionConfig(
        val version: String,
        val isFinalBuild: Boolean,
        val buildNumber: String?
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

    /**
     * Provides a version configuration provider for the given project.
     *
     * @param project The project to resolve version configuration for
     * @return Provider that resolves version configuration when needed
     */
    fun getVersionConfig(project: Project): Provider<VersionConfig> {
        return project.providers.provider {
            resolveVersionConfig(project)
        }
    }

    /**
     * Provides the artifact version string for the given project.
     * This is a convenience method for simple version string access.
     *
     * @param project The project to resolve version for
     * @return Provider that resolves to the artifact version string
     */
    fun getArtifactVersion(project: Project): Provider<String> {
        return getVersionConfig(project).map { it.artifactVersion }
    }

    private fun resolveVersionConfig(project: Project): VersionConfig {
        // Check if this is a final build
        val finalBuild = project.rootProject.providers
            .gradleProperty("finalBuild")
            .orNull?.toBoolean() ?: false

        // Version resolution priority
        val versionNumber = resolveVersionNumber(project)
        val buildNumber = resolveBuildNumber(project)

        return VersionConfig(
            version = versionNumber,
            isFinalBuild = finalBuild,
            buildNumber = buildNumber
        )
    }

    private fun resolveVersionNumber(project: Project): String {
        // 1. Command line properties (highest priority)
        project.rootProject.providers.gradleProperty("versionNumber").orNull?.let { return it }
        project.rootProject.providers.gradleProperty("sdkVersionNumber").orNull?.let { return it }

        // 2. Root project extra properties (includes local.properties values and buildnum.txt)
        val extraSdkVersion = project.rootProject.extensions.extraProperties.get("sdkVersionNumber") as? String
        if (extraSdkVersion != null) {
            return extraSdkVersion
        }

        val extraVersion = project.rootProject.extensions.extraProperties.get("versionNumber") as? String
        if (extraVersion != null && extraVersion != "0.0.0") {
            return extraVersion
        }

        // 3. Fallback to libs.versions.toml
        return project.libs.findVersion("arcgisMapsKotlinVersion").get().toString()
    }

    private fun resolveBuildNumber(project: Project): String? {
        // Don't use build number for final builds
        val finalBuild = project.rootProject.providers
            .gradleProperty("finalBuild")
            .orNull?.toBoolean() ?: false

        if (finalBuild) return null

        // 1. Command line properties (highest priority)
        project.rootProject.providers.gradleProperty("buildNumber").orNull?.let {
            return it.ifBlank { null }
        }
        project.rootProject.providers.gradleProperty("sdkBuildNumber").orNull?.let {
            return it.ifBlank { null }
        }

        // 2. Root project extra properties (includes local.properties values and buildnum.txt)
        val extraSdkBuildNumber = project.rootProject.extensions.extraProperties.get("sdkBuildNumber") as? String
        if (extraSdkBuildNumber != null && extraSdkBuildNumber.isNotBlank()) {
            return extraSdkBuildNumber
        }

        val extraBuildNumber = project.rootProject.extensions.extraProperties.get("buildNumber") as? String
        if (extraBuildNumber != null && extraBuildNumber != "SNAPSHOT" && extraBuildNumber.isNotBlank()) {
            return extraBuildNumber
        }

        return null
    }
}
