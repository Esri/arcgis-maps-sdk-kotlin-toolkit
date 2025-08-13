/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import java.io.File

/**
 * An extension to configure the [GrantTestPermissions] plugin.
 */
abstract class PermissionPluginExtension {

    /**
     * The package name of the application for which permissions will be granted.
     */
    abstract val packageName: Property<String>

    /**
     * The path to the ADB executable.
     */
    abstract val adbPath: Property<File>

    /**
     * A list of permissions to be granted to the application.
     * Each permission should be specified as a string, e.g., "android.permission.CAMERA".
     */
    abstract val permissions: ListProperty<String>
}

/**
 * A Gradle plugin to grant runtime permissions to a test application. This plugin is useful for
 * Android instrumented tests that require specific permissions to be granted before running tests.
 * It automates the process of granting permissions using ADB.
 *
 * @since 300.0.0
 */
class GrantTestPermissions : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<PermissionPluginExtension>("grantTestPermissionsConfig")
        val grantPermissionTask = project.tasks.register("grantTestPermissions") {
            doLast {
                val packageName = extension.packageName.get()
                val adb = extension.adbPath.get()
                val permissions = extension.permissions.get()
                println("Granting permissions for package: $packageName")
                permissions.forEach { permission ->
                    if (permission.contains("MANAGE_EXTERNAL_STORAGE")) {
                        // Special handling for MANAGE_EXTERNAL_STORAGE permission
                        project.providers.exec {
                            commandLine(
                                adb,
                                "shell",
                                "appops",
                                "set",
                                "--uid",
                                packageName,
                                "MANAGE_EXTERNAL_STORAGE",
                                "allow"
                            )
                        }.result.get().assertNormalExitValue()
                    } else {
                        // General permission granting
                        project.providers.exec {
                            commandLine(adb, "shell", "pm", "grant", packageName, permission)
                        }.result.get().assertNormalExitValue()
                    }
                    println("Granted permission: $permission")
                }
            }
        }

        project.afterEvaluate {
            // Configure the grantPermissionTask runs right before the install task
            project.tasks.matching { it.name.startsWith("install") && it.name.endsWith("AndroidTest") }
                .forEach { installTask ->
                    grantPermissionTask.get().dependsOn(installTask)
                }
            // Configure the grantPermissionTask runs before connected tasks
            project.tasks.matching { it.name.startsWith("connected") && it.name.endsWith("AndroidTest") }
                .forEach { connectedTask ->
                    connectedTask.dependsOn(grantPermissionTask.get())
                }
        }
    }
}
