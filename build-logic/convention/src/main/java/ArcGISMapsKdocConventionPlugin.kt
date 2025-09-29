import com.esri.arcgismaps.kotlin.build_logic.convention.VersionProvider
import com.esri.arcgismaps.kotlin.build_logic.extensions.implementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import com.esri.arcgismaps.kotlin.build_logic.registry.getToolkitRegistryServiceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaVersioningPluginParameters

class ArcGISMapsKdocConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("arcgismaps-android-library").get().get().pluginId)
            apply(libs.findPlugin("dokka").get().get().pluginId)
            // Put exposed dependencies in dokka's classpath
            apply(libs.findPlugin("arcgismaps.kotlin.sdk").get().get().pluginId)
        }

        dependencies {
            // Puts the version in the KDoc
            add("dokkaPlugin", libs.findLibrary("dokka.versioning").get())
            // Put exposed dependencies in dokka's classpath
            implementation(platform(libs.findLibrary("androidx-compose-bom").get()))
            implementation(libs.findBundle("composeCore").get())
        }

        //./gradlew :kdoc:dokkaGenerate
        // doc output will be under `kdoc/build/dokka/html`
        extensions.configure<DokkaExtension> {
            pluginsConfiguration.withType<DokkaVersioningPluginParameters>().configureEach {
                // Use centralized version provider
                version.set(VersionProvider.artifactVersionProvider(project))
            }

            moduleName.set("arcgis-maps-kotlin-toolkit")

            dokkaSourceSets.named("main") {
                // Get the toolkit registry provider
                val registryServiceProvider = getToolkitRegistryServiceProvider(target)
                // Lazily get releasable modules from the service provider
                val releasableModulesProvider = registryServiceProvider.map { service ->
                    service.toolkitModules.get().filter { it.releasable }
                }
                // Build the provider for source roots of all releasable modules
                val sourceRootFilesProvider = releasableModulesProvider.map { configs ->
                    configs.map { config -> rootProject.file(config.getSourceRoot(rootProject)) }
                }
                // Add the source roots using a provider string list
                sourceRoots.from(sourceRootFilesProvider)
                perPackageOption {
                    matchingRegex.set(".*internal.*")
                    suppress.set(true)
                }
                reportUndocumented.set(true)
            }
        }
    }
}
