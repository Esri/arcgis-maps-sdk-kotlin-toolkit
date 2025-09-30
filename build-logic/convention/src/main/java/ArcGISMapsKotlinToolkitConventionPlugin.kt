/*
 *
 *  Copyright 2025 Esri
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

import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.ArcGISMapsKotlinSDKDependency
import com.esri.arcgismaps.kotlin.build_logic.convention.ArtifactPublisher
import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitModuleExtension
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import com.esri.arcgismaps.kotlin.build_logic.registry.ModuleConfig
import com.esri.arcgismaps.kotlin.build_logic.registry.toolkitRegistryServiceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ArcGISMapsKotlinToolkitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Create extension with default releasable = true
            val toolkitExt = extensions.create<ToolkitModuleExtension>(ToolkitModuleExtension.NAME)
                .apply { applyDefaults() }

            with(pluginManager) {
                apply(libs.findPlugin("arcgismaps-android-library").get().get().pluginId)
                apply(libs.findPlugin("arcgismaps-android-library-compose").get().get().pluginId)
                apply(libs.findPlugin("binary-compatibility-validator").get().get().pluginId)
            }
            // Push this module's configuration to the central registry service.
            val registryServiceProvider = toolkitRegistryServiceProvider(this)
            afterEvaluate {
                registryServiceProvider.get().toolkitModules.add(
                    provider {
                        ModuleConfig(
                            path = path,
                            name = name,
                            releasable = toolkitExt.releasable.get()
                        )
                    }
                )
            }
            // Configure artifact publishing for toolkit modules
            ArtifactPublisher.configureArtifactPublisher(this)

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
                    val connectedTestReportsPath: String by project
                    reportDir = "$connectedTestReportsPath/${name}"
                }

                publishing {
                    singleVariant("release") {
                        // This is the default variant.
                    }
                }
            }

            // Explicit API configuration for toolkit modules
            tasks.withType<KotlinCompile> {
                // Only toolkit modules, exclude tests
                if ("Test" !in name) {
                    compilerOptions {
                        freeCompilerArgs.addAll(
                            listOf(
                                "-Xexplicit-api=strict",
                                "-Xcontext-parameters"
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
