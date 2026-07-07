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
    alias(libs.plugins.microapp.convention.plugin)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

android {
    namespace = "com.arcgismaps.toolkit.featureformsapp"

    defaultConfig {
        applicationId ="com.arcgismaps.toolkit.featureformsapp"
    }

}

dependencies {

    "buildWithSourceCodeImplementation"(project(":authentication"))
    "buildWithMavenArtifactsImplementation"(arcgis.authentication)
    "buildWithSourceCodeImplementation"(project(":featureforms"))
    "buildWithMavenArtifactsImplementation"(arcgis.featureforms)
    "buildWithSourceCodeImplementation"(project(":offline"))
    "buildWithMavenArtifactsImplementation"(arcgis.offline)

    // hilt
    implementation(libs.hilt.android.core)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    // room
    implementation(libs.room.runtime)
    implementation(libs.room.ext)
    ksp(libs.room.compiler)
    // jetpack window manager
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    // kotlinx serialization
    implementation(libs.kotlinx.serialization.json)
    // datastore
    implementation(libs.androidx.datastore.preferences)
    // coil
    implementation(libs.coil3.compose)
    // compose
    implementation(libs.androidx.compose.navigation)
}
