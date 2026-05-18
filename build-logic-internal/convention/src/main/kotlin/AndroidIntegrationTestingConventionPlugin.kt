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
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.arcgismaps.GrantDevicePermissions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Convention plugin for Android library modules that wire prerequisite tasks for
 * instrumented integration tests.
 *
 * This plugin:
 * - Registers `grantDevicePermissions` to grant runtime permissions to the test app.
 * - Wires each `connected<Variant>` task to:
 *   - optionally sync test data from `:build-logic-internal:syncTestData`,
 *   - grant device permissions,
 *   - delete IC output via `:build-logic-internal:deleteICOutput`.
 *
 * Note:
 * By depending on tasks such as `syncTestData` and `deleteICOutput` from the `build-logic-internal` 
 * module, Gradle ensures that these tasks are only executed once after project evaluation. This means 
 * that test data sync and deleting IC output is only happening once for multiple modules.
 *
 * Expected project setup:
 * - `com.android.library` is applied on the target module.
 * - Gradle property `syncTestDataBeforeInstrumentedTests` is defined (`true`/`false`).
 */
@Suppress("UNUSED")
class AndroidIntegrationTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.library") {
                afterEvaluate {
                    val androidDsl = extensions.getByType(LibraryExtension::class.java)
                    val androidComponents = extensions.getByType(LibraryAndroidComponentsExtension::class.java)
                    val adbPath = androidComponents.sdkComponents.adb.get().asFile

                    tasks.register<GrantDevicePermissions>("grantDevicePermissions") {
                        if (adbPath.exists()) {
                            adbExe.set(adbPath.absoluteFile)
                        }
                        val testAppId = androidDsl.defaultConfig.testApplicationId
                        if (!testAppId.isNullOrBlank()) {
                            testApplicationId.set(testAppId)
                        }
                    }

                    val syncTestDataBeforeInstrumentedTests =
                        project.findProperty("syncTestDataBeforeInstrumentedTests")
                            ?.toString()?.toBoolean() ?: false

                    tasks.forEach { task ->
                        if (task.name.startsWith("connected") && task.name.endsWith("AndroidTest")) {
                            task.dependsOn("grantDevicePermissions")
                            if (syncTestDataBeforeInstrumentedTests) {
                                try {
                                    task.dependsOn(gradle.includedBuild("build-logic-internal")
                                        .task(":syncTestData"))
                                } catch (e: Exception) {
                                    // syncTestData may not exist
                                }
                            }
                            try {
                                task.dependsOn(gradle.includedBuild("build-logic-internal")
                                    .task(":deleteICOutput"))
                            } catch (e: Exception) {
                                // deleteICOutput may not exist
                            }
                        }
                    }

                    // Wire the grantDevicePermissions task to depend on install*AndroidTest tasks
                    tasks.forEach { task ->
                        if (task.name.startsWith("install") && task.name.endsWith("AndroidTest")) {
                            tasks.named("grantDevicePermissions").configure {
                                dependsOn(task.name)
                            }
                        }
                    }
                }
            }
        }
    }
}
