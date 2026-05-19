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

plugins {
    id("com.android.library")
    alias(libs.plugins.android.integration.testing)
    id("org.jetbrains.kotlin.plugin.compose")
    id("artifact-deploy")
    alias(libs.plugins.kotlin.convention.plugin)
    alias(libs.plugins.binary.compatibility.validator)
}

val toolkitTests = project.findProperty("toolkitTestDir") as String

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.arcgismaps.toolkit.geoviewcompose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testApplicationId = "com.arcgismaps.toolkit.geoviewcompose.test"
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
    }

    sourceSets {
        var file = file("$toolkitTests/${project.name}/androidTest")
        if (file.exists()) {
            named("androidTest") {
                kotlin {
                    directories += "$toolkitTests/${project.name}/androidTest"
                }
                java {
                    directories += "$toolkitTests/${project.name}/androidTest"
                }
            }
        }

        file = file("$toolkitTests/${project.name}/test")
        if (file.exists()) {
            named("test") {
                kotlin {
                    directories += "$toolkitTests/${project.name}/test"
                }
                java {
                    directories += "$toolkitTests/${project.name}/test"
                }
            }
        }
    }


    // If this were not an android project, we would just write `explicitApi()` in the Kotlin scope.
    // but as an android project could write `freeCompilerArgs = listOf("-Xexplicit-api=strict")`
    // in the kotlinOptions above, but that would enforce api rules on the test code, which we don't want.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
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
        targetSdk = libs.versions.compileSdk.get().toInt()
        val connectedTestReportsPath: String by project
        reportDir = "$connectedTestReportsPath/${project.name}"
    }

    publishing {
        singleVariant("release") {
            // This is the default variant.
        }
    }
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    androidTestImplementation(libs.androidx.uiautomator)
    debugImplementation(libs.bundles.debug)

    // Include only if internal tests are required
    if (file(project.findProperty("toolkitTestDir") as String).exists()) {
        androidTestImplementation(arcgis.mapsSdkTestFixtures)
    }
}
