import com.android.build.api.dsl.ApplicationExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.ArcGISMapsKotlinSDKDependency
import com.esri.arcgismaps.kotlin.build_logic.extensions.implementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

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

            dependencies {
                // Configure the ArcGIS Maps SDK dependency
                ArcGISMapsKotlinSDKDependency.configureArcGISMapsDependencies(target)

                // Local project common microapps library
                implementation(project(":microapps-lib"))
            }
        }
    }
}
