import com.android.build.api.dsl.ApplicationExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.implementation
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

class ArcGISMapsKotlinMicroappConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("arcgismaps-android-application").get().get().pluginId)
                apply(libs.findPlugin("arcgismaps-android-application-compose").get().get().pluginId)
                apply(libs.findPlugin("gradle-secrets").get().get().pluginId)
            }


            // Configure secrets plugin defaults and custom BuildConfig fields
            extensions.configure<ApplicationExtension> {
                val defaultPropertiesFile = rootProject.file("secrets.defaults.properties")
                if (defaultPropertiesFile.exists()) {
                    val properties = java.util.Properties()
                    defaultPropertiesFile.inputStream().use { properties.load(it) }

                    defaultConfig {
                        // Add each property as a BuildConfig field
                        properties.forEach { (key, value) ->
                            val escapedValue = value.toString().replace("\"", "\\\"")
                            buildConfigField("String", key.toString(), "\"${escapedValue}\"")
                        }
                    }
                }
            }

            // Avoids an empty test report showing up in the CI integration test report.
            // Remove this if tests will be added.
            tasks.withType<Test> {
                enabled = false
            }

            dependencies {
                // The version of the ArcGIS Maps SDK for Kotlin dependency.
                // First look for the version number provided via command line (for CI builds), if not found,
                // take the one defined in gradle.properties.
                // CI builds pass -PversionNumber=${BUILDVER}
                val sdkVersionNumber: String? =
                    providers.gradleProperty("versionNumber").orNull
                        ?: providers.gradleProperty("sdkVersionNumber").orNull

                // The build number of the ArcGIS Maps SDK for Kotlin dependency.
                // First look for the version number provided via command line (for CI builds), if not found,
                // take the one defined in local.properties.
                // CI builds pass -PbuildNumber=${BUILDNUM}
                val sdkBuildNumber: String =
                    providers.gradleProperty("buildNumber").orNull
                        ?: providers.gradleProperty("sdkBuildNumber").orNull
                        ?: ""
                // ArcGIS Maps SDK dependency with build override support
                if (sdkVersionNumber != null) {
                    if (!sdkVersionNumber.isBlank()) {
                        implementation("com.esri:arcgis-maps-kotlin:$sdkVersionNumber-$sdkBuildNumber")
                    } else {
                        implementation("com.esri:arcgis-maps-kotlin:$sdkVersionNumber")
                    }
                } else {
                    // Use libs.versions.toml if no gradle property provided
                    implementation(libs.findLibrary("arcgis-maps-kotlin").get())
                }

                // Local project common microapps library
                implementation(project(":microapps-lib"))
            }
        }
    }
}
