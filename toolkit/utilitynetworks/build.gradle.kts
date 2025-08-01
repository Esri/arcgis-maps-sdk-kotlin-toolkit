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
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("artifact-deploy")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    alias(libs.plugins.binary.compatibility.validator) apply true
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
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
        // This flag is the same as applying '@ConsistentCopyVisibility' annotation to all data classes in the module.
        freeCompilerArgs = freeCompilerArgs + listOf("-Xconsistent-data-class-copy-visibility")
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
    @Suppress("UnstableApiUsage")
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

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.utilitynetworks.BuildConfig",
        "com.arcgismaps.toolkit.utilitynetworks.ComposableSingletons\$TraceKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceOptionsKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceOptionsScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceResultScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$AddStartingPointScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$StartingPointDetailsScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$ClearAllResultsDialogKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceErrorDialogKt",
        "com.arcgismaps.toolkit.utilitynetworks.internal.util.ComposableSingletons\$TitleRowKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.ComposableSingletons\$ExpandableCardKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(project(":geoview-compose"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.material.icons)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
