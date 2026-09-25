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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.internal.Actions.with
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask

@Suppress("UNUSED")
class KotlinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.withType(KotlinCompile::class.java).configureEach {
                compilerOptions {
                    allWarningsAsErrors.set(true)
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

            pluginManager.withPlugin("org.jetbrains.kotlinx.binary-compatibility-validator") {
                pluginManager.withPlugin("com.android.library") {
                    // Work around https://github.com/Kotlin/binary-compatibility-validator/issues/312
                    // by feeding the validator the Android release compilation outputs directly.
                    val apiValidation = project.extensions.getByType(ApiValidationExtension::class.java)
                    val validationEnabled = {
                        !apiValidation.validationDisabled && project.name !in apiValidation.ignoredProjects
                    }
                    val apiBuild = project.tasks.register(
                        "androidApiBuild",
                        KotlinApiBuildTask::class.java,
                        Action {
                            val releaseCompile = project.tasks.named("compileReleaseKotlin", KotlinCompile::class.java)
                            val releaseJavaCompile = project.tasks.named("compileReleaseJavaWithJavac")
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
    }
}
