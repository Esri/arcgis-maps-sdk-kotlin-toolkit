plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

secrets {
    // this file doesn't contain secrets, it just provides defaults which can be committed into git.
    defaultPropertiesFileName = "secrets.defaults.properties"
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.arcgismaps.toolkit.floorfilterapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId ="com.arcgismaps.toolkit.floorfilterapp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner ="androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "toolkitDependencies"

    productFlavors {
        create("buildWithSourceCode") {
            dimension = "toolkitDependencies"
            isDefault = true
        }
        create("buildWithMavenArtifacts") {
            dimension = "toolkitDependencies"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            //proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"),("proguard-rules.pro"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Avoids an empty test report showing up in the CI integration test report.
    // Remove this if tests will be added.
    tasks.withType<Test> {
        enabled = false
    }
}

dependencies {
    "buildWithSourceCodeImplementation"(project(mapOf("path" to ":geoview-compose")))
    "buildWithMavenArtifactsImplementation"(arcgis.geoview.compose)
    implementation(project(":microapps-lib"))
    "buildWithSourceCodeImplementation"(project(mapOf("path" to ":indoors")))
    "buildWithMavenArtifactsImplementation"(arcgis.indoors)
    implementation(arcgis.mapsSdk)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
