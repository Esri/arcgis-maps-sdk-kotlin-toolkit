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
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.internal.Actions.with
import org.gradle.internal.cc.base.logger
import org.gradle.kotlin.dsl.of
import org.gradle.kotlin.dsl.register
import java.io.File

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
                val androidDsl = extensions.getByType(LibraryExtension::class.java)
                val androidComponents =
                    extensions.getByType(LibraryAndroidComponentsExtension::class.java)

                val vaultTokenProvider = providers.of(VaultTokenProvider::class) {}
                val vaultToken = vaultTokenProvider.get().trim()
                if (vaultToken.isNotEmpty()) {
                    androidDsl.defaultConfig.testInstrumentationRunnerArguments["openbaoToken"] =
                        vaultToken
                } else {
                    logger.warn("vault token not provided")
                }

                val grantDevicePermissionsTask =
                    tasks.register<GrantDevicePermissions>("grantDevicePermissions") {
                        adbExe.set(androidComponents.sdkComponents.adb.map { it.asFile.absoluteFile })
                        testApplicationId.set(project.provider {
                            androidDsl.defaultConfig.testApplicationId.orEmpty()
                        })
                    }

                val syncTestDataBeforeInstrumentedTests =
                    project.findProperty("syncTestDataBeforeInstrumentedTests")
                        ?.toString()?.toBoolean() ?: false

                tasks.matching {
                    it.name.startsWith("connected") && it.name.endsWith("AndroidTest")
                }.configureEach {
                    dependsOn(grantDevicePermissionsTask)
                    if (syncTestDataBeforeInstrumentedTests) {
                        dependsOn(
                            gradle.includedBuild("build-logic-internal")
                                .task(":syncTestData")
                        )
                    }

                    dependsOn(
                        gradle.includedBuild("build-logic-internal")
                            .task(":deleteICOutput")
                    )
                }

                grantDevicePermissionsTask.configure {
                    dependsOn(
                        tasks.matching {
                            it.name.startsWith("install") && it.name.endsWith("AndroidTest")
                        }
                    )
                }

            }
        }
    }
}

/**
 * A [ValueSource] that reads the contents of the `.vault-token` file in the user's home directory.
 * If the file does not exist or is empty, an empty string is returned.
 *
 * @since 300.2.0
 */
@Suppress("UNUSED")
abstract class VaultTokenProvider : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        var token: String? = null

        val tokenFile = File(System.getProperty("user.home"), ".vault-token")
        if (tokenFile.exists()) {
            tokenFile.inputStream().bufferedReader().use {
                token = it.readText()
            }
        }

        return if (token != null) {
            token
        } else {
            logger.warn("vault token does not exist or is empty, returning an empty string.")
            ""
        }
    }
}

