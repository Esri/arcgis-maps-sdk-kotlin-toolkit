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
    alias(libs.plugins.arcgismaps.kotlin.toolkit)
}

android {
    namespace = "com.arcgismaps.toolkit.ar"
}

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.

    // ComposableSingletons$WorldScaleSceneViewScopeKt is generated due to internal compose function CalibrationViewInternal(),
    // we don't want to check binary compatibility for this internal function
    val composableSingletons = listOf(
        "com/arcgismaps/toolkit/ar/ComposableSingletons\$WorldScaleSceneViewScopeKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    // Module-specific dependencies go here
    implementation(project(":geoview-compose"))
    implementation(libs.arcore)
    implementation(libs.play.services.location)
}
