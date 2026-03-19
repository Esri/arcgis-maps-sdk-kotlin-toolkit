/*
 *
 *  Copyright 2026 Esri
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
    `kotlin-dsl`
}

// All plugins created in this module should have a corresponding placeholder plugin
// with the same ID in build-logic. build-logic-internal is not included for
// non-internal developers, so they can’t access these plugins and applying a missing
// plugin would fail their builds. The placeholder lets them compile, while internal
// developers get the real implementation from build-logic-internal.
gradlePlugin {
    plugins {
        register("androidIntegrationTesting") {
            id = "android-integration-testing"
            implementationClass = "AndroidIntegrationTestingConventionPlugin"
        }
    }
}

dependencies{
    compileOnly(libs.android.gradle.plugin)
    implementation(libs.arcgismaps.testdata.sync.gradle.plugin)
}
