/*
 *
 *  Copyright 2026 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import com.android.build.api.dsl.ApplicationExtension
import com.arcgismaps.kotlin.toolkit.build_logic.convention.arcgis
import com.arcgismaps.kotlin.toolkit.build_logic.convention.implementation
import com.arcgismaps.kotlin.toolkit.build_logic.convention.libs
import com.google.android.libraries.mapsplatform.secrets_gradle_plugin.SecretsPluginExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

@Suppress("UNUSED")
class MicroappConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("kotlin-convention-plugin")
                apply("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
            }
            configure<KotlinAndroidProjectExtension> {
                jvmToolchain(17)
            }

            configure<SecretsPluginExtension> {
                // this file doesn't contain secrets, it just provides defaults which can be committed into git.
                defaultPropertiesFileName = "secrets.defaults.properties"
            }

            extensions.configure<ApplicationExtension> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                compileSdk = libs.findVersion("targetSdk").get().toString().toInt()

                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                    versionCode = 1
                    versionName = "1.0"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                buildFeatures {
                    compose = true
                    buildConfig = true
                }

                // Avoids an empty test report showing up in the CI integration test report.
                // Remove this if tests will be added.
                tasks.withType<Test> {
                    enabled = false
                }

                flavorDimensions += "toolkitModule"

                productFlavors {
                    create("buildWithSourceCode") {
                        dimension = "toolkitModule"
                        isDefault = true
                    }
                    create("buildWithMavenArtifacts") {
                        dimension = "toolkitModule"
                    }
                }
            }

            dependencies {
                implementation(project(":microapps-lib"))

                "buildWithSourceCodeImplementation"(project(":geoview-compose"))
                "buildWithMavenArtifactsImplementation"(arcgis.findLibrary("geoview-compose").get())
                implementation(arcgis.findLibrary("mapsSdk").get())

                val composeBom = libs.findLibrary("androidx-compose-bom").get()
                implementation(platform(composeBom))
                implementation(libs.findLibrary("androidx-material-icons-core").get())
                implementation(libs.findBundle("composeCore").get())
                implementation(libs.findBundle("core").get())
                implementation(libs.findLibrary("androidx-activity-compose").get())
                implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            }
        }
    }
}
    