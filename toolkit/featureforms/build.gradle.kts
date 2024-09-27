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
    namespace = "com.arcgismaps.toolkit.featureforms"
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
    @Suppress("UnstableApiUsage")
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // If this were not an android project, we would just write `explicitApi()` in the Kotlin scope.
    // but as an android project could write `freeCompilerArgs = listOf("-Xexplicit-api=strict")`
    // in the kotlinOptions above, but that would enforce api rules on the test code, which we don't want.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        if ("Test" !in name) {
            kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
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
    lint {
        targetSdk = libs.versions.compileSdk.get().toInt()
    }
}

apiValidation {
    ignoredClasses.add("com.arcgismaps.toolkit.featureforms.BuildConfig")
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.featureforms.internal.components.base.ComposableSingletons\$BaseTextFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComposableSingletons\$ComboBoxFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComposableSingletons\$RadioButtonFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.ComposableSingletons\$DateTimeFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.ComposableSingletons\$DateTimePickerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.date.ComposableSingletons\$DatePickerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.ComposableSingletons\$TimePickerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$AttachmentFormElementKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$AttachmentTileKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.formelement.ComposableSingletons\$GroupElementKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.text.ComposableSingletons\$TextFormElementKt",
    )
    
    ignoredClasses.addAll(composableSingletons)
}


dependencies {
    api(arcgis.mapsSdk)
    implementation(libs.bundles.commonmark)
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    implementation(libs.bundles.composeCore)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons)
    implementation(libs.bundles.camerax)
    implementation(libs.barcode.scanning)
    implementation(libs.play.services.code.scanner)
    implementation(libs.play.services.base)
    implementation(libs.androidx.camera.mlkit.vision)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
