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
            val sdkVersionNumber = target.rootProject.properties["versionNumberInternal"].toString()
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
