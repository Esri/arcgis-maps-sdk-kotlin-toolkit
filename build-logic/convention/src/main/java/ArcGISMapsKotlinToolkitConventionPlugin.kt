import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.ArcGISMapsKotlinSDKDependency
import com.esri.arcgismaps.kotlin.build_logic.convention.ArtifactPublisher
import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitModuleExtension
import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitRegistryExtension
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class ArcGISMapsKotlinToolkitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val toolkitExt = extensions.create<ToolkitModuleExtension>("toolkit").apply {
                applyDefaults() // releasable = true by default
            }

            with(pluginManager) {
                apply(libs.findPlugin("arcgismaps-android-library").get().get().pluginId)
                apply(libs.findPlugin("arcgismaps-android-library-compose").get().get().pluginId)
                if (toolkitExt.releasable.get()) {
                    apply(libs.findPlugin("binary-compatibility-validator").get().get().pluginId)
                }
            }
            // Provide maven publication for releasable toolkit projects
            ArtifactPublisher.configureArtifactPublisher(this)

            afterEvaluate {
                val registry = rootProject.extensions.getByType(ToolkitRegistryExtension::class.java)
                if (toolkitExt.releasable.orNull == true) {
                    logger.warn("SETTING RELEASABLE: ${this.name}")
                    registry.toolkitProjects.add(this)
                } else {
                    registry.toolkitProjects.remove(this)
                }
            }

            extensions.configure<LibraryExtension> {
                packaging {
                    resources {
                        excludes += setOf(
                            "META-INF/LICENSE-notice.md",
                            "META-INF/LICENSE.md"
                        )
                    }
                }

                testOptions {
                    targetSdk = libs.findVersion("compileSdk").get().toString().toInt()
                    val connectedTestReportsPath = target.findProperty("connectedTestReportsPath") as? String
                        ?: "${target.rootProject.rootDir}/connectedTestReports"
                    reportDir = "$connectedTestReportsPath/${target.name}"
                }

                publishing {
                    singleVariant("release") {
                        // This is the default variant.
                    }
                }
            }

            // Automatic detection of androidTest sources
            val androidTestDir = File(projectDir, "src/androidTest")
            val hasInstrumentedTests = androidTestDir.exists() && androidTestDir.walkTopDown().any {
                it.isFile && (it.extension == "kt")
            }
            // Filter out generating test reports for modules without tests
            if (!hasInstrumentedTests) {
                // Disable unit tests
                tasks.withType<Test>()
                    .configureEach { enabled = false }
                // Disable connected Android tests
                tasks.matching { it.name.startsWith("connected") && it.name.endsWith("AndroidTest") }
                    .configureEach { enabled = false }
            }

            // Explicit API configuration for toolkit modules
            tasks.withType<KotlinCompile> {
                // Only toolkit modules, exclude tests
                if ("Test" !in name) {
                    compilerOptions {
                        freeCompilerArgs.addAll(
                            listOf(
                                "-Xexplicit-api=strict",
                                "-Xcontext-receivers"
                            )
                        )
                    }
                }
            }

            dependencies {
                // Configure the ArcGIS Maps SDK dependency
                ArcGISMapsKotlinSDKDependency.configureArcGISMapsDependencies(target)
            }
        }
    }
}
