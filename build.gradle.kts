/*
 *
 *  Copyright 2023 Esri
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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.gradle.secrets) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.gmazzo.test.aggregation)
}

buildscript {
    dependencies {
        // there doesn't appear to be a better way to provide this to subprojects.
        // this is what lets us put the version number dropdown list in the generated dokka.
        // it is a "dokka plugin" which is not a gradle plugin, it needs to be on the classpath
        // before any dependent subproject uses its symbols to configure a dokka task.
        classpath(libs.dokka.versioning)
    }
}

/**
 * Configures the [gmazzo test aggregation plugin](https://github.com/gmazzo/gradle-android-test-aggregation-plugin)
 * with all local tests to be aggregated into a single test report.
 * Note: This works only for local tests, not for connected tests.
 * To run aggregated local tests:
 * ```
 * ./gradlew testAggregatedReport
 * ```
 * Test report to be found under `arcgis-maps-sdk-kotlin-toolkit/build/reports`.
 */
testAggregation {
    getModules(
        "bom",
        "kdoc",
        "microapps-lib",
        "template",
        "template-app",
        "composable-map").forEach {
        this.modules.include(project(":$it"))
    }
}

/**
 * Returns all modules in this project, except the ones specified by [modulesToExclude].
 */
fun getModules(vararg modulesToExclude: String): List<String> =
    with(File("$rootDir/settings.gradle.kts")) {
        readLines()
            .filter { it.startsWith("include") }
            .map {
                it.removePrefix("include(\":").removeSuffix("\")")
            }
            .filter { !modulesToExclude.contains(it) } // exclude specified modules
    }
