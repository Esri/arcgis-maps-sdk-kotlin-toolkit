/*
 *
 *  Copyright CURRENT_YEAR Esri
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
    alias(libs.plugins.component.convention.plugin)
}

/**
 * Include this if any internal-only tests are required
 */
val toolkitTests = project.findProperty("toolkitTestDir") as String

android {
    namespace = "com.arcgismaps.toolkit.template"

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
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)

    /**
     * Include this if any internal-only test dependencies are required
     */
    if (file(project.findProperty("toolkitTestDir") as String).exists()) {
        androidTestImplementation(libs.mockingjay)
    }
}
