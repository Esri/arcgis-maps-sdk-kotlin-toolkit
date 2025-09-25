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
    alias(libs.plugins.gradle.secrets)
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

kotlin {
    // This flag is the same as applying '@ConsistentCopyVisibility' annotation to all data classes in the module.
    compilerOptions { freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility") }
}

android {
    namespace = "com.arcgismaps.toolkit.featureforms"
    buildFeatures {
        buildConfig = true
    }
    // Lint crashes on the latest Android studio
    // (Bug with Android Studio Meerkat | 2024.3.1)
    // TODO: Remove this when Android Studio lint checker is fixed
    lint {
        disable.add("SuspiciousModifierThen")
        // remove these disables when strings.xml lint is fixed via localization
        disable += "MissingTranslation"
        disable += "MissingQuantity"
    }
}

apiValidation {
    ignoredClasses.add("com.arcgismaps.toolkit.featureforms.BuildConfig")
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.featureforms.internal.components.base.ComposableSingletons\$BaseTextFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComposableSingletons\$ComboBoxFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComposableSingletons\$ComboBoxDialogKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComposableSingletons\$RadioButtonFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.ComposableSingletons\$DateTimeFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.ComposableSingletons\$DateTimePickerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.date.ComposableSingletons\$DatePickerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.ComposableSingletons\$TimePickerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$AttachmentFormElementKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$AttachmentTileKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$AttachmentErrorDialogKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$DeleteAttachmentDialogKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.attachment.ComposableSingletons\$RenameAttachmentDialogKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.formelement.ComposableSingletons\$GroupElementKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.text.ComposableSingletons\$TextFormElementKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.barcode.ComposableSingletons\$BarcodeScannerKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.barcode.ComposableSingletons\$BarcodeTextFieldKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.ComposableSingletons\$UtilityAssociationsElementKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.ComposableSingletons\$UtilityAssociationsKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.ComposableSingletons\$UtilityAssociationDetailsKt",
        "com.arcgismaps.toolkit.featureforms.internal.components.dialogs.ComposableSingletons\$ConfirmationDialogsKt",
        "com.arcgismaps.toolkit.featureforms.internal.screens.ComposableSingletons\$ContentAwareTopBarKt",
        "com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction\$NavigateToFeature\$Creator",
        "com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction\$NavigateToAssociation\$Creator",
        "com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction\$Dismiss\$Creator",
        "com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction\$NavigateBack\$Creator",
        "com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction\$None\$Creator",
        "com.arcgismaps.toolkit.featureforms.internal.components.dialogs.ComposableSingletons\$ConfirmationDialogsKt"
    )
    
    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    // Module-specific dependencies go here
    implementation(libs.bundles.commonmark)
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.bundles.camerax)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.compose.navigation)
}
