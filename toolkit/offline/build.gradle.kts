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


plugins {
    alias(libs.plugins.component.convention.plugin)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.arcgismaps.toolkit.offline"
}

apiValidation {
    // TODO: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions.
    // this may be resolved in the compose compiler.
     val composableSingletons = listOf(
             "com.arcgismaps.toolkit.offline.preplanned.ComposableSingletons\$PreplannedMapAreasKt",
             "com.arcgismaps.toolkit.offline.ui.ComposableSingletons\$MapAreaDetailsScreenKt",
             "com.arcgismaps.toolkit.offline.ui.material3.ComposableSingletons\$ModalBottomSheetKt",
             "com.arcgismaps.toolkit.offline.ui.ComposableSingletons\$OfflineMapAreasStatusScreenKt",
             "com.arcgismaps.toolkit.offline.ondemand.ComposableSingletons\$OnDemandMapAreaSelectorKt",
             "com.arcgismaps.toolkit.offline.internal.utils.ComposableSingletons\$ButtonsKt",
             "com.arcgismaps.toolkit.offline.ondemand.ComposableSingletons\$OnDemandMapAreasKt"
     )
     ignoredClasses.addAll(composableSingletons)
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(project(":geoview-compose"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.bundles.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
