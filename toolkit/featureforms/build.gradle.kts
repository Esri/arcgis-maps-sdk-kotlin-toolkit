plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("artifact-deploy")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "com.arcgismaps.toolkit.featureforms"
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
    @Suppress("UnstableApiUsage")
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }
    // If this were not an android project, we would just write `explicitApi()` in the Kotlin scope.
    // but as an android project could write `freeCompilerArgs = listOf("-Xexplicit-api=strict")`
    // in the kotlinOptions above, but that would enforce api rules on the test code, which we don't want.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        if ("Test" !in name) {
            kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
        }
    }
}

dependencies {
    api(arcgis.mapsSdk)
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
