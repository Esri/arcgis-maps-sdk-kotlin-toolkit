import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.esri.arcgismaps.kotlin.build_logic.convention"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.dokka.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplicationComposing") {
            id = "arcgismaps.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "arcgismaps.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "arcgismaps.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "arcgismaps.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("arcgismapsKotlinToolkit") {
            id = "arcgismaps.kotlin.toolkit"
            implementationClass = "ArcGISMapsKotlinToolkitConventionPlugin"
        }
        register("arcgismapsKotlinMicroapp") {
            id = "arcgismaps.kotlin.microapp"
            implementationClass = "ArcGISMapsKotlinMicroappConventionPlugin"
        }
        register("arcGISMapsKotlinSDK") {
            id = "arcgismaps.kotlin.sdk"
            implementationClass = "ArcGISMapsKotlinSDKConventionPlugin"
        }
        register("arcGISMapsKdocConventionPlugin") {
            id = "arcgismaps.kotlin.kdoc.convention"
            implementationClass = "ArcGISMapsKdocConventionPlugin"
        }
        register("arcGISMapsBomConventionPlugin") {
            id = "arcgismaps.kotlin.bom.convention"
            implementationClass = "ArcGISMapsBomConventionPlugin"
        }
    }
}
