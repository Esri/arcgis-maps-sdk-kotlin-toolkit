/*
 *
 *  Copyright 2024 Esri
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
    alias(libs.plugins.binary.compatibility.validator) apply true
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("artifact-deploy")
}
android {
    namespace = "com.arcgismaps.toolkit.utilitynetworks"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"

    }
    buildFeatures {
        compose = true
    }

    kotlin {
        compilerOptions {
            if ("Test" !in this.moduleName.get()) {
                freeCompilerArgs.add("-Xexplicit-api=strict")
            }

        }
    }

    /**
     * Configures the test report for connected (instrumented) tests to be copied to a central
     * folder in the project's root directory.
     */
    @Suppress("UnstableApiUsage")
    testOptions {
        targetSdk = libs.versions.compileSdk.get().toInt()
        val connectedTestReportsPath: String by project
        reportDir = "$connectedTestReportsPath/${project.name}"
    }
    lint {
        targetSdk = libs.versions.compileSdk.get().toInt()
    }
}

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.utilitynetworks.ComposableSingletons\$TraceKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceOptionsKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(project(":sharedlib"))
    implementation(project(":geoview-compose"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.navigation)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
