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
    alias(libs.plugins.arcgismaps.kotlin.microapp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.arcgismaps.toolkit.featureformsapp"
    
    defaultConfig {
        applicationId = "com.arcgismaps.toolkit.featureformsapp"
    }
}

dependencies {
    // Module-specific dependencies go here
    implementation(project(":authentication"))
    implementation(project(":featureforms"))
    implementation(project(":geoview-compose"))
    implementation(libs.androidx.compose.navigation)
    implementation(libs.hilt.android.core)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.ext)
    ksp(libs.room.compiler)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
}
