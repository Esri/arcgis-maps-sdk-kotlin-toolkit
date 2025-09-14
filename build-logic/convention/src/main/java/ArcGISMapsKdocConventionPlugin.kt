import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitRegistryExtension
import com.esri.arcgismaps.kotlin.build_logic.extensions.implementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaVersioningPluginParameters
import java.io.File

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

        val versionNumber: String by project
        // Create the toolkit sources provider
        val registry = rootProject.extensions.getByType(ToolkitRegistryExtension::class.java)
        val sourceRootsProvider = providers.provider {
            registry.toolkitProjects.map { p ->
                File(rootDir, "toolkit/${p.name}/src/main/java").canonicalPath
            }
        }

        //./gradlew :kdoc:dokkaGenerate
        // doc output will be under `kdoc/build/dokka/html`
        extensions.configure<DokkaExtension> {
            pluginsConfiguration.withType<DokkaVersioningPluginParameters>().configureEach {
                version.set(versionNumber)
            }

            moduleName.set("arcgis-maps-kotlin-toolkit")

            dokkaSourceSets {
                if (findByName("main") == null) {
                    register("main")
                }

                named("main") {
                    sourceRoots.from(files(sourceRootsProvider))
                }
                configureEach {
                    perPackageOption {
                        matchingRegex.set(".*internal.*")
                        suppress.set(true)
                    }
                    perPackageOption {
                        reportUndocumented.set(true)
                    }
                }
            }
        }
    }
}
