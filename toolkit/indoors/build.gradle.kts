plugins {
    alias(libs.plugins.component.convention.plugin)
}

android {
    namespace = "com.arcgismaps.toolkit.indoors"
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
    implementation(libs.bundles.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    androidTestImplementation(project(":geoview-compose"))
    androidTestImplementation(project(":microapps-lib"))
    debugImplementation(libs.bundles.debug)
}
