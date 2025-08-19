import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.ArcGISMapsKotlinSDKDependency
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class ArcGISMapsKotlinToolkitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("arcgismaps-android-library").get().get().pluginId)
                apply(libs.findPlugin("arcgismaps-android-library-compose").get().get().pluginId)
                // Only apply binary compatibility validator if shouldValidateApi is true
                val shouldValidateApi = target.findProperty("shouldValidateApi") as? Boolean ?: true
                if (shouldValidateApi) {
                    apply(libs.findPlugin("binary-compatibility-validator").get().get().pluginId)
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
                if ("Test" !in name) {
                    compilerOptions {
                        freeCompilerArgs.addAll(
                            listOf(
                                // This flag is the same as applying '@ConsistentCopyVisibility' annotation to all data classes in the module.
                                "-Xconsistent-data-class-copy-visibility",
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
