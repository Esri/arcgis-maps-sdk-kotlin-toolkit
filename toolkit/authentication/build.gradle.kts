/*
 *
 *  Copyright 2023 Esri
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
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

android {
    namespace = "com.arcgismaps.toolkit.authentication"
}

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.authentication.ComposableSingletons\$ServerTrustAuthenticatorKt",
        "com.arcgismaps.toolkit.authentication.ComposableSingletons\$UsernamePasswordAuthenticatorKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.bundles.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    androidTestImplementation(project(":microapps-lib"))
    debugImplementation(libs.bundles.debug)

    // uiautomator
    androidTestImplementation(libs.androidx.uiautomator)
    // espresso intents
    androidTestImplementation(libs.androidx.espresso.intents)
    // mockk for android tests
    androidTestImplementation(libs.mockk.android)
}
