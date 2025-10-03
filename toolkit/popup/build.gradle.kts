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

kotlin {
    // This flag is the same as applying '@ConsistentCopyVisibility' annotation to all data classes in the module.
    compilerOptions { freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility") }
}

android {
    namespace = "com.arcgismaps.toolkit.popup"
    lint {
        // remove these disables when strings.xml lint is fixed via localization
        disable += "MissingTranslation"
    }
}

apiValidation {
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.popup.internal.ui.ComposableSingletons\$ExpandableCardKt",
        "com.arcgismaps.toolkit.popup.internal.ui.fileviewer.ComposableSingletons\$FileViewerKt",
        "com.arcgismaps.toolkit.popup.internal.ui.expandablecard.ComposableSingletons\$ExpandableCardKt",
        "com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.ComposableSingletons\$UtilityAssociationDetailsKt",
        "com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.ComposableSingletons\$UtilityAssociationsFilterResultKt",
        "com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.ComposableSingletons\$UtilityAssociationGroupResultKt",
        "com.arcgismaps.toolkit.popup.internal.screens.ComposableSingletons\$ContentAwareTopBarKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    // Module-specific dependencies go here
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
}
