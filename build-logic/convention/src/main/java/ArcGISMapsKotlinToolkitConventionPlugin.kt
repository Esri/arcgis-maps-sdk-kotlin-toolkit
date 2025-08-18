import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.api
import com.esri.arcgismaps.kotlin.build_logic.convention.implementation
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ArcGISMapsKotlinToolkitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("arcgismaps-android-library").get().get().pluginId)
                apply(libs.findPlugin("arcgismaps-android-library-compose").get().get().pluginId)
                apply(libs.findPlugin("binary-compatibility-validator").get().get().pluginId)
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

                @Suppress("UnstableApiUsage")
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
            }
        }
    }
}
