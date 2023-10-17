plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "com.arcgismaps.toolkit.featureformsapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId ="com.arcgismaps.toolkit.featureformsapp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner ="androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        create("release") {
            // You need to specify either an absolute path or include the
            // keystore file in the same directory as the build.gradle file.
            storeFile = file("/Users/sor10874/Desktop/keystore")
            storePassword = "featureform"
            keyAlias = "key0"
            keyPassword = "featureform"
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            //proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"),("proguard-rules.pro"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

//https://youtrack.jetbrains.com/issue/KTIJ-21063
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

dependencies {
    implementation(project(":featureforms"))
    implementation(project(":composable-map"))
    // sdk
    implementation(arcgis.mapsSdk)
    // hilt
    implementation(libs.hilt.android.core)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    // room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.ext)
    kapt(libs.room.compiler)
    // coil
    implementation(libs.coil.compose)
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
}
