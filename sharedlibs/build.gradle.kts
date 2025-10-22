plugins {
    alias(libs.plugins.android.fusedlibrary)
    `maven-publish`
//    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
//    kotlin("android")
}

androidFusedLibrary {
    namespace = "com.arcgismaps.toolkit.sharedlibs"
    minSdk = 21
}

dependencies {
    include(platform(libs.androidx.compose.bom))
    include(libs.bundles.composeCore)
    include(libs.androidx.core.ktx)
    include(libs.androidx.appcompat)
    include(libs.material)
}

