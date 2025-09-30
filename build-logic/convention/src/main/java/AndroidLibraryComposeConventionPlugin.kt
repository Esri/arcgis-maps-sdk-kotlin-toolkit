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

import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.configureAndroidCompose
import com.esri.arcgismaps.kotlin.build_logic.convention.configureKotlinAndroid
import com.esri.arcgismaps.kotlin.build_logic.extensions.androidTestImplementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.debugImplementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.implementation
import com.esri.arcgismaps.kotlin.build_logic.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("android-library").get().get().pluginId)
                apply(libs.findPlugin("kotlin-android").get().get().pluginId)
                apply(libs.findPlugin("compose-compiler").get().get().pluginId)
                apply(libs.findPlugin("kotlin-parcelize").get().get().pluginId)
                apply(libs.findPlugin("kotlin-serialization").get().get().pluginId)
            }
            val extension = extensions.getByType<LibraryExtension>()
            configureKotlinAndroid(extension)
            configureAndroidCompose(extension)

            // Add common Compose dependencies for library modules
            dependencies {
                implementation(platform(libs.findLibrary("androidx-compose-bom").get()))
                implementation(libs.findBundle("composeCore").get())
                implementation(libs.findBundle("core").get())
                implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                implementation(libs.findLibrary("androidx-activity-compose").get())
                implementation(libs.findLibrary("androidx-material-icons").get())
                implementation(libs.findLibrary("kotlinx-serialization-json").get())
                androidTestImplementation(libs.findBundle("composeTest").get())
                androidTestImplementation(libs.findBundle("androidXTest").get())
                debugImplementation(libs.findBundle("debug").get())
            }
        }
    }
}
