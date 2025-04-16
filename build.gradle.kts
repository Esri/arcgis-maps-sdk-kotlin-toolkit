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
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {
    dependencies {
        // there doesn't appear to be a better way to provide this to subprojects.
        // this is what lets us put the version number dropdown list in the generated dokka.
        // it is a "dokka plugin" which is not a gradle plugin, it needs to be on the classpath
        // before any dependent subproject uses its symbols to configure a dokka task.
        classpath(libs.dokka.versioning)
    }
    val localProperties = java.util.Properties().apply {
        val localPropertiesFile = file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }
    // Find these in properties passed through command line or read from local.properties and
    // set them as project properties
    val artifactoryUrl: String? = project.findProperty("artifactoryUrl") as String?
        ?: localProperties.getProperty("artifactoryUrl")
        ?: "https://esri.jfrog.io/artifactory/arcgis"
    val artifactoryUsername: String? = project.findProperty("artifactoryUsername") as String?
        ?: localProperties.getProperty("artifactoryUsername")
        ?: ""
    val artifactoryPassword: String? = project.findProperty("artifactoryPassword") as String?
        ?: localProperties.getProperty("artifactoryPassword")
        ?: ""

    project.extra.set("artifactoryUrl", artifactoryUrl)
    project.extra.set("artifactoryUsername", artifactoryUsername)
    project.extra.set("artifactoryPassword", artifactoryPassword)

    val finalBuild: Boolean = (project.properties["finalBuild"] ?: "false")
        .run { this == "true" }

    if (finalBuild) {
        check(project.hasProperty("versionNumber"))
        project.logger.info("release candidate build requested version ${project.properties["versionNumber"]}")
    } else if (!project.hasProperty("versionNumber") && !project.hasProperty("buildNum")) {
        // both version number and build number must be set
        java.util.Properties().run {
            try {
                File(project.projectDir, "../buildnum/buildnum.txt")
                    .reader()
                    .use {
                        load(it)
                    }
                this["BUILDVER"]?.let {
                    project.extra.set("versionNumber", it)
                }
                this["BUILDNUM"]?.let {
                    project.extra.set("buildNumber", it)
                }
                check(project.hasProperty("versionNumber"))
                check(project.hasProperty("buildNumber"))
                project.logger.info("version and build number set from buildnum.txt to ${project.properties["versionNumber"]}-${project.properties["buildNumber"]}")
            } catch (t: Throwable) {
                // The buildnum file is not there. ignore it and log a warning.
                project.logger.warn("the buildnum.txt file is missing or not readable")
                project.extra.set("versionNumber", "0.0.0")
                project.extra.set("buildNumber", "SNAPSHOT")
            }
        }
    }
}

// Path to the centralized folder in root directory where test reports for connected tests end up
val connectedTestReportsPath by extra("${rootDir}/connectedTestReports")

/**
 * Configures the [gmazzo test aggregation plugin](https://github.com/gmazzo/gradle-android-test-aggregation-plugin)
 * with all local tests to be aggregated into a single test report.
 * Note: This works only for local tests, not for connected tests.
 * To run aggregated local tests, run the following at the root folder of the project:
 * ```
 * ./gradlew testAggregatedReport --continue
 * ```
 * Test report to be found under `arcgis-maps-sdk-kotlin-toolkit/build/reports`.
 */
testAggregation {
    getModulesExcept(
        "bom",
        "kdoc",
        "template",
        "microapps-lib",
        "composable-map"
    ).forEach {
        this.modules.include(project(":$it"))
    }
}

/**
 * Returns all modules in this project, except the ones specified by [modulesToExclude].
 */
fun getModulesExcept(vararg modulesToExclude: String): List<String> =
    with(File("$rootDir/settings.gradle.kts")) {
        readLines()
            .filter { it.startsWith("include") }
            .map {
                it.removePrefix("include(\":").removeSuffix("\")")
            }
            .filter { !modulesToExclude.contains(it) } // exclude specified modules
    }

