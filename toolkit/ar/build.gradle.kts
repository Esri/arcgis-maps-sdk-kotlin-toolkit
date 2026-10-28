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
    alias(libs.plugins.component.convention.plugin)
}

android {
    namespace = "com.arcgismaps.toolkit.ar"
}

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.

    // ComposableSingletons$WorldScaleSceneViewScopeKt is generated due to internal compose function CalibrationViewInternal(),
    // we don't want to check binary compatiblity for this internal function
    val composableSingletons = listOf(
        "com/arcgismaps/toolkit/ar/ComposableSingletons\$WorldScaleSceneViewScopeKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    implementation(project(":geoview-compose"))
    implementation(libs.arcore)
    api(arcgis.mapsSdk)
    implementation(libs.androidx.material.icons.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.play.services.location)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.bundles.composeTest)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.bundles.debug)
}
