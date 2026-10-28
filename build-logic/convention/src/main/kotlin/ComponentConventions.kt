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

import com.android.build.api.dsl.LibraryExtension
import com.arcgismaps.kotlin.toolkit.build_logic.convention.VaultTokenProvider
import com.arcgismaps.kotlin.toolkit.build_logic.convention.libs
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.of
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * A plugin to apply to all toolkit component modules
 *
 * @since 300.2.0
 */
@Suppress("UNUSED")
class ComponentConventions : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("kotlin-convention-plugin")
                apply("org.jetbrains.kotlinx.binary-compatibility-validator")
                apply("artifact-deploy")

                withPlugin("org.jetbrains.kotlinx.binary-compatibility-validator") {
                    withPlugin("com.android.library") {
                        // Work around https://github.com/Kotlin/binary-compatibility-validator/issues/312
                        // by feeding the validator the Android release compilation outputs directly.
                        val apiValidation =
                            project.extensions.getByType(ApiValidationExtension::class.java)
                        val validationEnabled = {
                            !apiValidation.validationDisabled && project.name !in apiValidation.ignoredProjects
                        }
                        val apiBuild = project.tasks.register(
                            "androidApiBuild",
                            KotlinApiBuildTask::class.java,
                            Action {
                                val releaseCompile = project.tasks.named(
                                    "compileReleaseKotlin",
                                    KotlinCompile::class.java
                                )
                                val releaseJavaCompile =
                                    project.tasks.named("compileReleaseJavaWithJavac")
                                dependsOn(releaseCompile)
                                dependsOn(releaseJavaCompile)
                                inputClassesDirs.from(releaseCompile.map { it.outputs.files })
                                inputClassesDirs.from(releaseJavaCompile.map { it.outputs.files })
                                outputApiFile.set(
                                    project.layout.buildDirectory.file("androidApiBuild/${project.name}.api")
                                )
                                onlyIf { validationEnabled() }
                            }
                        )

                        val apiCheck = project.tasks.register(
                            "androidApiCheck",
                            KotlinApiCompareTask::class.java,
                            Action {
                                group = "verification"
                                onlyIf { validationEnabled() }
                                projectApiFile.set(
                                    project.layout.projectDirectory.file("api/${project.name}.api")
                                )
                                generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
                            }
                        )
                        val apiDump = project.tasks.register(
                            "androidApiDump",
                            Copy::class.java,
                            Action {
                                onlyIf { validationEnabled() }
                                from(apiBuild.flatMap { it.outputApiFile })
                                into(project.layout.projectDirectory.dir("api"))
                            }
                        )

                        project.tasks.maybeCreate("apiCheck").apply {
                            group = "verification"
                            dependsOn(apiCheck)
                        }
                        project.tasks.maybeCreate("apiDump").dependsOn(apiDump)
                        project.tasks.named("check").configure(Action { dependsOn(apiCheck) })
                    }
                }

            }

            val finalBuild: Boolean = run {
                val propVal = providers.gradleProperty("finalBuild").orNull ?: "false"
                propVal.toBoolean()
            }

            extensions.configure<LibraryExtension> {
                compileSdk = project.libs.findVersion("compileSdk").get().toString().toInt()

                defaultConfig {
                    minSdk = project.libs.findVersion("minSdk").get().toString().toInt()

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    consumerProguardFiles("consumer-rules.pro")
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }
                buildFeatures {
                    compose = true
                    buildConfig = true
                }
                packaging {
                    resources {
                        excludes += setOf(
                            "META-INF/LICENSE-notice.md",
                            "META-INF/LICENSE.md"
                        )
                    }
                }
                // If this were not an android project, we would just write `explicitApi()` in the Kotlin scope.
                // but as an android project could write `freeCompilerArgs = listOf("-Xexplicit-api=strict")`
                // in the kotlinOptions above, but that would enforce api rules on the test code, which we don't want.
                tasks.withType<KotlinCompile> {
                    if ("Test" !in name) {
                        compilerOptions {
                            freeCompilerArgs.add("-Xexplicit-api=strict")
                        }
                    }
                }

                /**
                 * Configures the test report for connected (instrumented) tests to be copied to a central
                 * folder in the project's root directory.
                 */
                testOptions {
                    targetSdk = libs.findVersion("compileSdk").get().toString().toInt()
                    val connectedTestReportsPath: String =
                        rootProject.extra["connectedTestReportsPath"] as String
                    reportDir = "$connectedTestReportsPath/${project.name}"
                }

                publishing {
                    singleVariant("release") {
                        // This is the default variant.
                    }
                }

                val vaultTokenProvider = providers.of(VaultTokenProvider::class) {}
                val vaultToken = vaultTokenProvider.get().trim()
                if (vaultToken.isEmpty()) {
                    // dont change the build config object's structure even if the token isn't
                    // available
                    logger.warn("vault token not provided")
                }
                if (!finalBuild) {
                    defaultConfig.buildConfigField("String", "vaultToken", "\"${vaultToken}\"")
                } else {
                    defaultConfig.buildConfigField("String", "vaultToken", "\"redacted\"")
                    logger.info("vault token redacted for final build")
                }
                defaultConfig.testInstrumentationRunnerArguments["openbaoToken"] =
                    vaultToken
            }
        }
    }
}

