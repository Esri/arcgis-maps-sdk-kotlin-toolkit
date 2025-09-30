/*
 *
 *  Copyright 2025 Esri
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

package com.esri.arcgismaps.kotlin.build_logic.convention

import com.android.build.api.dsl.CommonExtension
import com.esri.arcgismaps.kotlin.build_logic.extensions.androidTestImplementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.debugImplementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.implementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Extension to use compose configurations and dependencies
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion = libs.findVersion("kotlin").get().toString()
        }

        dependencies {
            val composeBom = libs.findLibrary("androidx-compose-bom").get()
            implementation(platform(composeBom))
            androidTestImplementation(platform(composeBom))
            androidTestImplementation(libs.findLibrary("androidx-compose-ui-test").get())
            implementation(libs.findLibrary("androidx-activity-compose").get())
            implementation(libs.findLibrary("androidx-compose-material3").get())
            implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            implementation(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            debugImplementation(libs.findBundle("debug").get())
        }
    }
}
