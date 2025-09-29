import com.google.android.libraries.mapsplatform.secrets_gradle_plugin.SecretsPluginExtension
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

            // Configure the secrets-gradle-plugin using its dedicated extension.
            extensions.configure<SecretsPluginExtension> {
                defaultPropertiesFileName = "secrets.defaults.properties"
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
