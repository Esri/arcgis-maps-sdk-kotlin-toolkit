import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.ArcGISMapsKotlinSDKDependency
import com.esri.arcgismaps.kotlin.build_logic.convention.ArtifactPublisher
import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitModuleExtension
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
            // Create extension with default releasable = true
            val toolkitExt = extensions.create<ToolkitModuleExtension>("toolkit").apply {
                applyDefaults() // releasable = true by default
            }

            with(pluginManager) {
                apply(libs.findPlugin("arcgismaps-android-library").get().get().pluginId)
                apply(libs.findPlugin("arcgismaps-android-library-compose").get().get().pluginId)
                apply(libs.findPlugin("binary-compatibility-validator").get().get().pluginId)
            }

            // Configure artifact publishing for toolkit modules
            ArtifactPublisher.configureArtifactPublisher(this)

            // Log configuration after evaluation
            afterEvaluate {
                val isReleasable = toolkitExt.releasable.getOrElse(true)
                logger.info("Toolkit module '${target.name}' configured as ${if (isReleasable) "releasable" else "non-releasable"}")
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
                    val connectedTestReportsPath =
                        target.findProperty("connectedTestReportsPath") as? String
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
