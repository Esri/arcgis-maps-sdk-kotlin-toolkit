package com.esri.arcgismaps.kotlin.build_logic.convention

import com.esri.arcgismaps.kotlin.build_logic.extensions.api
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Project
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
     * This function checks for Gradle properties (`versionNumber` or `sdkVersionNumber`)
     * to determine the SDK version. If a version is provided, it can be combined with a
     * `buildNumber` to specify a specific build. If no version property is found,
     * it falls back to the version defined in `libs.versions.toml`.
     *
     * @param target The Gradle Project to which the dependency will be added.
     */
    fun configureArcGISMapsDependencies(target: Project) {
        target.dependencies {

            // For finalBuilds ignore the build number and pick up the released version of the SDK dependency
            val finalBuild: Boolean = (target.rootProject.providers.gradleProperty("finalBuild").orNull ?: "false")
                .toBoolean()

            // First look for the version number provided via command line (for CI builds), if not found,
            // take the one from project extras (which includes local.properties values).
            // CI builds pass -PversionNumber=${BUILDVER}
            val sdkVersionNumber: String? =
                target.rootProject.providers.gradleProperty("versionNumber").orNull
                    ?: target.rootProject.providers.gradleProperty("sdkVersionNumber").orNull
                    ?: target.rootProject.extensions.extraProperties.get("sdkVersionNumber") as? String

            // The build number of the ArcGIS Maps SDK for Kotlin dependency.
            // First look for the version number provided via command line (for CI builds), if not found,
            // take the one from project extras (which includes local.properties values).
            // CI builds pass -PbuildNumber=${BUILDNUM}
            val sdkBuildNumber: String? =
                target.rootProject.providers.gradleProperty("buildNumber").orNull
                    ?: target.rootProject.providers.gradleProperty("sdkBuildNumber").orNull
                    ?: target.rootProject.extensions.extraProperties.get("sdkBuildNumber") as? String

            // ArcGIS Maps SDK dependency with build override support
            if (sdkVersionNumber != null) {
                val dependencyVersion = if (finalBuild) {
                    // For a final build, use the version number directly
                    sdkVersionNumber
                } else {
                    // If a buildNumber is provided and not blank, append it to the version.
                    if (!sdkBuildNumber.isNullOrBlank()) {
                        "$sdkVersionNumber-$sdkBuildNumber"
                    } else {
                        sdkVersionNumber
                    }
                }
                api("com.esri:arcgis-maps-kotlin:$dependencyVersion")
            } else {
                // Use libs.versions.toml if no gradle property is provided - for `main` branch.
                api(target.libs.findLibrary("arcgis-maps-kotlin").get())
            }
        }
    }
}
