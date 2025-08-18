package com.esri.arcgismaps.kotlin.build_logic.convention

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
            // First look for the version number provided via command line (for CI builds), if not found,
            // take the one defined in gradle.properties.
            // CI builds pass -PversionNumber=${BUILDVER}
            val sdkVersionNumber: String? =
                target.providers.gradleProperty("versionNumber").orNull
                    ?: target.providers.gradleProperty("sdkVersionNumber").orNull

            // The build number of the ArcGIS Maps SDK for Kotlin dependency.
            // First look for the version number provided via command line (for CI builds), if not found,
            // take the one defined in local.properties.
            // CI builds pass -PbuildNumber=${BUILDNUM}
            val sdkBuildNumber: String? =
                target.providers.gradleProperty("buildNumber").orNull
                    ?: target.providers.gradleProperty("sdkBuildNumber").orNull
                    ?: ""

            // ArcGIS Maps SDK dependency with build override support
            if (sdkVersionNumber != null) {
                // If a buildNumber is provided and not blank, append it to the version.
                val dependencyVersion = if (!sdkBuildNumber.isNullOrBlank()) {
                    "$sdkVersionNumber-$sdkBuildNumber"
                } else {
                    sdkVersionNumber
                }
                implementation("com.esri:arcgis-maps-kotlin:$dependencyVersion")
            } else {
                // Use libs.versions.toml if no gradle property is provided
                implementation(target.libs.findLibrary("arcgis-maps-kotlin").get())
            }
        }
    }
}
