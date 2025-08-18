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
    alias(libs.plugins.artifact.deploy)
}

android {
    namespace = "com.arcgismaps.toolkit.utilitynetworks"
}

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.utilitynetworks.BuildConfig",
        "com.arcgismaps.toolkit.utilitynetworks.ComposableSingletons\$TraceKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceOptionsKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceOptionsScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceResultScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$AddStartingPointScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$StartingPointDetailsScreenKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$ClearAllResultsDialogKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.ComposableSingletons\$TraceErrorDialogKt",
        "com.arcgismaps.toolkit.utilitynetworks.internal.util.ComposableSingletons\$TitleRowKt",
        "com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.ComposableSingletons\$ExpandableCardKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    // Module-specific dependencies go here
    implementation(project(":geoview-compose"))
    implementation(libs.androidx.compose.navigation)
}
