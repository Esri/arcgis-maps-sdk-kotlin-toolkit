plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("artifact-deploy")
    alias(libs.plugins.binary.compatibility.validator) apply true
}

android {
    namespace = "com.arcgismaps.toolkit.indoors"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    // If this were not an android project, we would just write `explicitApi()` in the Kotlin scope.
    // but as an android project could write `freeCompilerArgs = listOf("-Xexplicit-api=strict")`
    // in the kotlinOptions above, but that would enforce api rules on the test code, which we don't want.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        if ("Test" !in name) {
            compilerOptions {
                freeCompilerArgs.add("-Xexplicit-api=strict")
            }
        }
    }

    /**
     * Configures the test report for connected (instrumented) tests to be copied to a central
     * folder in the project's root directory.
     */
    @Suppress("UnstableApiUsage")
    testOptions {
        targetSdk = libs.versions.compileSdk.get().toInt()
        val connectedTestReportsPath: String by project
        reportDir = "$connectedTestReportsPath/${project.name}"
    }

    publishing {
        singleVariant("release") {
            // This is the default variant.
        }
    }
}

apiValidation {
    ignoredClasses.add("com.arcgismaps.toolkit.featureforms.BuildConfig")
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.indoors.ComposableSingletons\$SiteAndFacilitySelectorKt"
    )

    ignoredClasses.addAll(composableSingletons)
}


dependencies {
    api(arcgis.mapsSdk)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    androidTestImplementation(project(mapOf("path" to ":composable-map")))
    debugImplementation(libs.bundles.debug)
}
