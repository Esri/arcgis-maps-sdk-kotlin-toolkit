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

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.arcgismaps.GrantDevicePermissions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import java.util.Locale

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
class AndroidIntegrationTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.library") {
                val android = extensions.getByType(LibraryExtension::class.java)

                // Registers a module-specific `grantDevicePermissions` task for the test app.
                tasks.register<GrantDevicePermissions>("grantDevicePermissions") {
                    adbExe.set(android.adbExecutable.absoluteFile)
                    testApplicationId.set(android.defaultConfig.testApplicationId)
                }

                afterEvaluate {

                    android.testVariants.forEach { testVariant ->
                        val capitalizedTestVariantName =
                            testVariant.name.replaceFirstChar {
                                if (it.isLowerCase()) {
                                    it.titlecase(Locale.US)
                                }
                                else {
                                    it.toString()
                                }
                            }
                        tasks.named("connected$capitalizedTestVariantName") {
                            val syncTestDataBeforeInstrumentedTests: String by project

                            if (syncTestDataBeforeInstrumentedTests.toBoolean()) {
                                // Uses adb to sync the test data to the test device.
                                dependsOn(gradle.includedBuild("build-logic-internal").task(":syncTestData"))
                            }
                            // Grants storage permissions requested by the test app.
                            dependsOn("grantDevicePermissions")
                            // Deletes ic-output folder before running connectedAndroidTests
                            dependsOn(gradle.includedBuild("build-logic-internal").task(":deleteICOutput"))
                        }

                        // Make sure the permissions task only runs after the install task, otherwise the app
                        // may not be installed yet.
                        tasks.named("grantDevicePermissions").dependsOn("install$capitalizedTestVariantName")
                    }
                }
            }
    } }
}
